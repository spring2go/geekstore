/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import.populator;

import io.geekstore.common.ApiType;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.NormalizeUtil;
import io.geekstore.config.shipping_method.ShippingCalculator;
import io.geekstore.config.shipping_method.ShippingEligibilityChecker;
import io.geekstore.data_import.CollectionDefinition;
import io.geekstore.data_import.FacetValueCollectionFilterDefinition;
import io.geekstore.data_import.InitialData;
import io.geekstore.data_import.ShippingMethodData;
import io.geekstore.data_import.asset_importer.AssetImporter;
import io.geekstore.entity.AssetEntity;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.entity.FacetValueEntity;
import io.geekstore.service.*;
import io.geekstore.types.collection.CreateCollectionInput;
import io.geekstore.types.common.ConfigArgInput;
import io.geekstore.types.common.ConfigurableOperationInput;
import io.geekstore.types.shipping.CreateShippingMethodInput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Responsible for populating the database with initial data.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class Populator {
    private final CollectionService collectionService;
    private final FacetValueService facetValueService;
    private final AssetImporter assetImporter;
    private final ObjectMapper objectMapper;
    private final ShippingMethodService shippingMethodService;
    private final ShippingEligibilityChecker defaultShippingEligibilityChecker;
    private final ShippingCalculator defaultShippingCalculator;
    private final SearchIndexService searchIndexService;

    /**
     * Should be run *before* populating the products.
     */
    public void populateInitialData(InitialData data) {
        this.populateShippingMethods(data.getShippingMethods());
    }

    private void populateShippingMethods(List<ShippingMethodData> shippingMethods) {
        for(ShippingMethodData method: shippingMethods) {
            CreateShippingMethodInput input = new CreateShippingMethodInput();
            ConfigurableOperationInput checker = new ConfigurableOperationInput();
            checker.setCode(defaultShippingEligibilityChecker.getCode());
            ConfigArgInput configArgInput = new ConfigArgInput();
            configArgInput.setName("orderMinimum");
            configArgInput.setValue("0");
            checker.getArguments().add(configArgInput);
            input.setChecker(checker);

            ConfigurableOperationInput calculator = new ConfigurableOperationInput();
            calculator.setCode(defaultShippingCalculator.getCode());
            configArgInput = new ConfigArgInput();
            configArgInput.setName("rate");
            configArgInput.setValue(method.getPrice().toString());
            calculator.getArguments().add(configArgInput);
            input.setCalculator(calculator);

            input.setDescription(method.getName());
            input.setCode(NormalizeUtil.normalizeString(method.getName(), "-"));

            this.shippingMethodService.create(input);
        }
    }

    /**
     * Should be run *after* the products have been populated, otherwise the expected FacetValues will not yet exist.
     */
    public void populateCollections(InitialData data) {
        RequestContext ctx = this.createRequestContext(data);

        List<FacetValueEntity> allFacetValues = this.facetValueService.findAll();
        Map<String, CollectionEntity> collectionMap = new HashMap<>();
        for(CollectionDefinition collectionDef : data.getColletions()) {
            CollectionEntity parent = null;
            if (collectionDef.getParentName() != null) {
                parent = collectionMap.get(collectionDef.getParentName());
            }
            Long parentId = null;
            if (parent != null) {
                parentId = parent.getId();
            }
            List<AssetEntity> assets = this.assetImporter.getAssets(collectionDef.getAssetPaths()).getAssets();

            CreateCollectionInput createCollectionInput = new CreateCollectionInput();
            createCollectionInput.setName(collectionDef.getName());
            createCollectionInput.setDescription(collectionDef.getDescription());
            if (createCollectionInput.getDescription() == null) {
                createCollectionInput.setDescription("");
            }
            createCollectionInput.setSlug(collectionDef.getSlug());
            if (createCollectionInput.getSlug() == null) {
                createCollectionInput.setSlug(collectionDef.getName());
            }
            createCollectionInput.setPrivateOnly(collectionDef.isPrivateOnly());
            createCollectionInput.setParentId(parentId);
            createCollectionInput.setAssetIds(assets.stream().map(a -> a.getId()).collect(Collectors.toList()));
            if (assets.size() > 0) {
                createCollectionInput.setFeaturedAssetId(assets.get(0).getId());
            }
            createCollectionInput.setFilters(
                    collectionDef.getFilters().stream().map(
                            filter -> this.processFilterDefinition(filter, allFacetValues)
                    ).collect(Collectors.toList())
            );
            CollectionEntity collection = this.collectionService.create(ctx, createCollectionInput);
            collectionMap.put(collectionDef.getName(), collection);
        }

        // Wait for the created collection operations to complete before running the reindex of the search index.
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.searchIndexService.reindex();
    }

    private ConfigurableOperationInput processFilterDefinition(
            FacetValueCollectionFilterDefinition filter, List<FacetValueEntity> allFacetValueEntities) {
        switch (filter.getCode()) {
            case "facet-value-filter":
                List<Long> facetValueIds = filter.getFacetValueNames().stream()
                        .map(name -> allFacetValueEntities.stream().filter(fv -> Objects.equals(fv.getName(), name))
                                .findFirst().orElse(null))
                        .filter(facetValueEntity -> facetValueEntity != null)
                        .map(fv -> fv.getId()).collect(Collectors.toList());
                ConfigurableOperationInput input = new ConfigurableOperationInput();
                input.setCode(filter.getCode());

                ConfigArgInput argInput = new ConfigArgInput();
                argInput.setName("facetValueIds");
                try {
                    argInput.setValue(objectMapper.writeValueAsString(facetValueIds));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                input.getArguments().add(argInput);

                argInput = new ConfigArgInput();
                argInput.setName("containsAny");
                argInput.setValue(String.valueOf(filter.isContainsAny()));
                input.getArguments().add(argInput);

                return input;
            default:
                throw new RuntimeException("Filter with code " + filter.getCode() + " is not recognized");
        }
    }

    private RequestContext createRequestContext(InitialData data) {
        RequestContext ctx = new RequestContext();
        ctx.setApiType(ApiType.ADMIN);
        ctx.setAuthorized(true);
        ctx.setAuthorizedAsOwnerOnly(false);
        return ctx;
    }
}
