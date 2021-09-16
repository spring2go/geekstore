/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import.importer;

import io.geekstore.common.utils.NormalizeUtil;
import io.geekstore.data_import.asset_importer.AssetImportResult;
import io.geekstore.data_import.asset_importer.AssetImporter;
import io.geekstore.data_import.parser.*;
import io.geekstore.entity.AssetEntity;
import io.geekstore.entity.FacetEntity;
import io.geekstore.entity.FacetValueEntity;
import io.geekstore.service.FacetService;
import io.geekstore.service.FacetValueService;
import io.geekstore.types.ImportInfo;
import io.geekstore.types.facet.CreateFacetInput;
import io.geekstore.types.facet.CreateFacetValueInput;
import io.geekstore.types.product.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Importer {

    private final ImportParser importParser;
    private final FacetService facetService;
    private final FacetValueService facetValueService;
    private final FastImporterService fastImporter;
    private final AssetImporter assetImporter;

    /**
     * These Maps are used to cache newly-created entities and prevent duplicates
     * from beging created.
     */
    private Map<String, FacetEntity> facetMap = new HashMap<>();
    private Map<String, FacetValueEntity> facetValueMap = new HashMap<>();

    public ImportInfo parseAndImport(InputStream input) {
        ImportInfo importInfo = new ImportInfo();
        Reader inputReader = new InputStreamReader(input);
        ParseResult<ParsedProductWithVariants> parsed = this.importParser.parseProducts(inputReader);
        if (parsed != null && parsed.getResults().size() > 0) {
            List<String> importErrors = this.importProducts(parsed.getResults());
            importInfo.getErrors().addAll(parsed.getErrors());
            importInfo.getErrors().addAll(importErrors);
            importInfo.setImported(parsed.getResults().size());
            importInfo.setProcessed(parsed.getProcessed());
        } else {
            importInfo.setImported(0);
            importInfo.setProcessed(parsed.getProcessed());
        }
        return importInfo;
    }

    /**
     * Imports the products specified in the rows object. Return an array of error message.
     */
    private List<String> importProducts(List<ParsedProductWithVariants> rows) {
        List<String> errors = new ArrayList<>();
        Integer imported = 0;
        for(ParsedProductWithVariants row : rows) {
            ParsedProduct product = row.getProduct();
            List<ParsedProductVariant> variants = row.getVariants();
            AssetImportResult createProductAssets = this.assetImporter.getAssets(product.getAssetPaths());
            List<AssetEntity> productAssets = createProductAssets.getAssets();
            if (createProductAssets.getErrors().size() > 0) {
                errors.addAll(createProductAssets.getErrors());
            }
            CreateProductInput createProductInput = new CreateProductInput();
            if (productAssets.size() > 0) {
                createProductInput.setFeaturedAssetId(productAssets.get(0).getId());
            }
            createProductInput.setAssetIds(productAssets.stream().map(a -> a.getId()).collect(Collectors.toList()));
            createProductInput.setFacetValueIds(this.getFacetValueIds(product.getFacets()));
            createProductInput.setName(product.getName());
            createProductInput.setDescription(product.getDescription());
            createProductInput.setSlug(product.getSlug());
            Long createdProductId = this.fastImporter.createProduct(createProductInput);

            Map<String, Long> optionsMap = new HashMap<>();
            for(StringOptionGroup optionGroup : product.getOptionGroups()) {
                String code = NormalizeUtil.normalizeString(
                        product.getName() + "-" + optionGroup.getName(), "-");
                CreateProductOptionGroupInput createProductOptionGroupInput = new CreateProductOptionGroupInput();
                createProductOptionGroupInput.setCode(code);
                createProductOptionGroupInput.setName(optionGroup.getName());
                createProductOptionGroupInput.setOptions(
                        optionGroup.getValues().stream().map(name -> {
                            CreateGroupOptionInput input = new CreateGroupOptionInput();
                            // 暂时忽略填充具体字段，后面单独处理
                            return input;
                }).collect(Collectors.toList()));
                Long groupId = this.fastImporter.createProductOptionGroup(createProductOptionGroupInput);
                for(String option : optionGroup.getValues()) {
                    CreateProductOptionInput createProductOptionInput = new CreateProductOptionInput();
                    createProductOptionInput.setProductOptionGroupId(groupId);
                    createProductOptionInput.setCode(NormalizeUtil.normalizeString(option, "-"));
                    createProductOptionInput.setName(option);
                    Long createdOptionId = this.fastImporter.createProductOption(createProductOptionInput);
                    optionsMap.put(option, createdOptionId);
                }
                this.fastImporter.addOptionGroupToProduct(createdProductId, groupId);
            }

            for(ParsedProductVariant variant : variants) {
                AssetImportResult createVariantAssets = this.assetImporter.getAssets(variant.getAssetPaths());
                List<AssetEntity> variantAssets = createVariantAssets.getAssets();
                if (createVariantAssets.getErrors().size() > 0) {
                    errors.addAll(createVariantAssets.getErrors());
                }
                List<Long> facetValueIds = new ArrayList<>();
                if (variant.getFacets().size() > 0) {
                    facetValueIds = this.getFacetValueIds(variant.getFacets());
                }
                CreateProductVariantInput createProductVariantInput = new CreateProductVariantInput();
                createProductVariantInput.setProductId(createdProductId);
                createProductVariantInput.setFacetValueIds(facetValueIds);
                if (variantAssets.size() > 0) {
                    createProductVariantInput.setFeaturedAssetId(variantAssets.get(0).getId());
                }
                createProductVariantInput.setAssetIds(
                        variantAssets.stream().map(a -> a.getId()).collect(Collectors.toList()));
                createProductVariantInput.setSku(variant.getSku());
                createProductVariantInput.setStockOnHand(variant.getStockOnHand());
                createProductVariantInput.setOptionIds(
                        variant.getOptionValues().stream().map(v -> optionsMap.get(v))
                                .collect(Collectors.toList()));
                List<String> tempNames = new ArrayList<>();
                tempNames.add(product.getName());
                tempNames.addAll(variant.getOptionValues());
                createProductVariantInput.setName(tempNames.stream().collect(Collectors.joining(" ")));
                createProductVariantInput.setPrice(Math.round(variant.getPrice() * 100));
                this.fastImporter.createProductVariant(createProductVariantInput);
            }
            imported++;
        }
        return errors;
    }


    private List<Long> getFacetValueIds(List<StringFacet> facets) {
        List<Long> facetValueIds = new ArrayList<>();

        for(StringFacet item : facets) {
            String facetName = item.getFacet();
            String valueName = item.getValue();

            FacetEntity facetEntity = null;
            FacetEntity cachedFacet = this.facetMap.get(facetName);
            if (cachedFacet != null) {
                facetEntity = cachedFacet;
            } else {
                FacetEntity existing = this.facetService.findByCode(NormalizeUtil.normalizeString(facetName));
                if (existing != null) {
                    facetEntity = existing;
                } else {
                    CreateFacetInput createFacetInput = new CreateFacetInput();
                    createFacetInput.setPrivateOnly(false);
                    createFacetInput.setCode(NormalizeUtil.normalizeString(facetName, "-"));
                    createFacetInput.setName(facetName);
                    facetEntity = this.facetService.create(createFacetInput);
                }
                this.facetMap.put(facetName, facetEntity);
            }

            FacetValueEntity facetValueEntity = null;
            String facetValueMapKey = facetName + ":" + valueName;
            FacetValueEntity cachedFacetValue = this.facetValueMap.get(facetValueMapKey);
            if (cachedFacetValue != null) {
                facetValueEntity = cachedFacetValue;
            } else {
                List<FacetValueEntity> facetValueEntities = this.facetValueService.findByFacetId(facetEntity.getId());
                FacetValueEntity existing = facetValueEntities.stream()
                        .filter(v -> Objects.equals(v.getName(), valueName)).findFirst().orElse(null);
                if (existing != null) {
                    facetValueEntity = existing;
                } else {
                    CreateFacetValueInput createFacetValueInput = new CreateFacetValueInput();
                    createFacetValueInput.setFacetId(facetEntity.getId());
                    createFacetValueInput.setCode(NormalizeUtil.normalizeString(valueName, "-"));
                    createFacetValueInput.setName(valueName);
                    facetValueEntity = this.facetValueService.create(createFacetValueInput);
                }
                this.facetValueMap.put(facetValueMapKey, facetValueEntity);
            }
            facetValueIds.add(facetValueEntity.getId());
        }
        return facetValueIds;
    }
}
