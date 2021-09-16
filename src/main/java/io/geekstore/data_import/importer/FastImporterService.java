/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import.importer;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.*;
import io.geekstore.mapper.*;
import io.geekstore.service.StockMovementService;
import io.geekstore.types.product.CreateProductInput;
import io.geekstore.types.product.CreateProductOptionGroupInput;
import io.geekstore.types.product.CreateProductOptionInput;
import io.geekstore.types.product.CreateProductVariantInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * A importer to import entities into the database. This replaces the regular
 * `create` methods of the importer layer with faster versions which skip much of the defensive checks
 * and other DB calls which are not needed when running an import.
 *
 * In testing, the use of the FastImporterService approximately doubled the speed of bulk imports.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class FastImporterService {
    private final StockMovementService stockMovementService;
    private final ProductEntityMapper productEntityMapper;
    private final ProductFacetValueJoinEntityMapper productFacetValueJoinEntityMapper;
    private final ProductAssetJoinEntityMapper productAssetJoinEntityMapper;
    private final ProductOptionEntityMapper productOptionEntityMapper;
    private final ProductOptionGroupEntityMapper productOptionGroupEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final ProductVariantProductOptionJoinEntityMapper productVariantProductOptionJoinEntityMapper;
    private final ProductVariantFacetValueJoinEntityMapper productVariantFacetValueJoinEntityMapper;
    private final ProductVariantAssetJoinEntityMapper productVariantAssetJoinEntityMapper;
    private final ProductOptionGroupJoinEntityMapper productOptionGroupJoinEntityMapper;

    public Long createProduct(CreateProductInput input) {
        ProductEntity productEntity = BeanMapper.map(input, ProductEntity.class);
        productEntityMapper.insert(productEntity);

        if (!CollectionUtils.isEmpty(input.getFacetValueIds())) {
            for(Long facetValueId : input.getFacetValueIds()) {
                ProductFacetValueJoinEntity productFacetValueJoinEntity = new ProductFacetValueJoinEntity();
                productFacetValueJoinEntity.setProductId(productEntity.getId());
                productFacetValueJoinEntity.setFacetValueId(facetValueId);
                productFacetValueJoinEntityMapper.insert(productFacetValueJoinEntity);
            }
        }

        if (!CollectionUtils.isEmpty(input.getAssetIds())) {
            for(Long assetId : input.getAssetIds()) {
                ProductAssetJoinEntity productAssetJoinEntity = new ProductAssetJoinEntity();
                productAssetJoinEntity.setProductId(productEntity.getId());
                productAssetJoinEntity.setAssetId(assetId);
                this.productAssetJoinEntityMapper.insert(productAssetJoinEntity);
            }
        }
        return productEntity.getId();
    }

    public Long createProductOptionGroup(CreateProductOptionGroupInput input) {
        ProductOptionGroupEntity productOptionGroupEntity = new ProductOptionGroupEntity();
        productOptionGroupEntity.setCode(input.getCode());
        productOptionGroupEntity.setName(input.getName());
        this.productOptionGroupEntityMapper.insert(productOptionGroupEntity);
        return productOptionGroupEntity.getId();
    }

    public Long createProductOption(CreateProductOptionInput input) {
        ProductOptionEntity productOptionEntity = new ProductOptionEntity();
        productOptionEntity.setCode(input.getCode());
        productOptionEntity.setName(input.getName());
        productOptionEntity.setGroupId(input.getProductOptionGroupId());

        productOptionEntityMapper.insert(productOptionEntity);

        return productOptionEntity.getId();
    }

    public void addOptionGroupToProduct(Long productId, Long optionGroupId) {
        ProductOptionGroupJoinEntity joinEntity = new ProductOptionGroupJoinEntity();
        joinEntity.setProductId(productId);
        joinEntity.setOptionGroupId(optionGroupId);
        this.productOptionGroupJoinEntityMapper.insert(joinEntity);
    }

    public Long createProductVariant(CreateProductVariantInput input) {
        if (input.getPrice() == null) {
            input.setPrice(0);
        }

        ProductVariantEntity createdVariantEntity = BeanMapper.patch(input, ProductVariantEntity.class);
        this.productVariantEntityMapper.insert(createdVariantEntity);

        if (!CollectionUtils.isEmpty(input.getOptionIds())) {
            for(Long optionId : input.getOptionIds()) {
                ProductVariantProductOptionJoinEntity productVariantProductOptionJoinEntity =
                        new ProductVariantProductOptionJoinEntity();
                productVariantProductOptionJoinEntity.setProductVariantId(createdVariantEntity.getId());
                productVariantProductOptionJoinEntity.setProductOptionId(optionId);
                this.productVariantProductOptionJoinEntityMapper.insert(productVariantProductOptionJoinEntity);
            }
        }

        if (!CollectionUtils.isEmpty(input.getFacetValueIds())) {
            for(Long facetValueId : input.getFacetValueIds()) {
                ProductVariantFacetValueJoinEntity productVariantFacetValueJoinEntity =
                        new ProductVariantFacetValueJoinEntity();
                productVariantFacetValueJoinEntity.setProductVariantId(createdVariantEntity.getId());
                productVariantFacetValueJoinEntity.setFacetValueId(facetValueId);
                this.productVariantFacetValueJoinEntityMapper.insert(productVariantFacetValueJoinEntity);
            }
        }

        if (!CollectionUtils.isEmpty(input.getAssetIds())) {
            int pos = 0;
            for(Long assetId : input.getAssetIds()) {
                ProductVariantAssetJoinEntity productVariantAssetJoinEntity = new ProductVariantAssetJoinEntity();
                productVariantAssetJoinEntity.setProductVariantId(createdVariantEntity.getId());
                productVariantAssetJoinEntity.setAssetId(assetId);
                productVariantAssetJoinEntity.setPosition(pos);
                this.productVariantAssetJoinEntityMapper.insert(productVariantAssetJoinEntity);
                pos++;
            }
        }

        if (input.getStockOnHand() != null && input.getStockOnHand() != 0) {
            this.stockMovementService.adjustProductVariantStock(
                    createdVariantEntity.getId(), 0, input.getStockOnHand());
        }

        return createdVariantEntity.getId();
    }
}
