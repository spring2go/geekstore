/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.ProductOptionGroupEntity;
import io.geekstore.entity.ProductOptionGroupJoinEntity;
import io.geekstore.mapper.ProductOptionGroupEntityMapper;
import io.geekstore.mapper.ProductOptionGroupJoinEntityMapper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.product.CreateProductOptionGroupInput;
import io.geekstore.types.product.CreateProductOptionInput;
import io.geekstore.types.product.UpdateProductOptionGroupInput;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class ProductOptionGroupService {
    private final ProductOptionGroupEntityMapper productOptionGroupEntityMapper;
    private final ProductOptionGroupJoinEntityMapper productOptionGroupJoinEntityMapper;
    private final ProductOptionService productOptionService;

    public List<ProductOptionGroupEntity> findAll(String filterTerm) {
        QueryWrapper<ProductOptionGroupEntity> queryWrapper = new QueryWrapper<>();
        if (filterTerm != null) {
            queryWrapper.lambda().like(ProductOptionGroupEntity::getCode, filterTerm);
        }
        List<ProductOptionGroupEntity> productOptionGroupEntityList =
                productOptionGroupEntityMapper.selectList(queryWrapper);
        return productOptionGroupEntityList;
    }

    public ProductOptionGroupEntity findOne(Long id) {
        return this.productOptionGroupEntityMapper.selectById(id);
    }

    public List<ProductOptionGroupEntity> getOptionGroupsByProductId(Long productId) {
        QueryWrapper<ProductOptionGroupJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProductOptionGroupJoinEntity::getProductId, productId);
        List<ProductOptionGroupJoinEntity> productOptionGroupJoinEntities =
                this.productOptionGroupJoinEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(productOptionGroupJoinEntities)) return new ArrayList<>();

        List<Long> productGroupIds = productOptionGroupJoinEntities.stream()
                .map(ProductOptionGroupJoinEntity::getOptionGroupId).distinct().sorted().collect(Collectors.toList());

        return this.productOptionGroupEntityMapper.selectBatchIds(productGroupIds);
    }

    @Transactional
    public ProductOptionGroupEntity create(CreateProductOptionGroupInput input) {
        ProductOptionGroupEntity productOptionGroupEntity = new ProductOptionGroupEntity();
        productOptionGroupEntity.setCode(input.getCode());
        productOptionGroupEntity.setName(input.getName());
        this.productOptionGroupEntityMapper.insert(productOptionGroupEntity);
        if (!CollectionUtils.isEmpty(input.getOptions())) {
            input.getOptions().forEach(createGroupOptionInput -> {
                CreateProductOptionInput createProductOptionInput =
                        BeanMapper.map(createGroupOptionInput, CreateProductOptionInput.class);
                createProductOptionInput.setProductOptionGroupId(productOptionGroupEntity.getId());
                productOptionService.create(createProductOptionInput);
            });
        }
        return productOptionGroupEntity;
    }

    public ProductOptionGroupEntity update(UpdateProductOptionGroupInput input) {
        ProductOptionGroupEntity productOptionGroupEntity =
                ServiceHelper.getEntityOrThrow(
                        productOptionGroupEntityMapper, ProductOptionGroupEntity.class, input.getId());
        BeanMapper.patch(input, productOptionGroupEntity);
        productOptionGroupEntityMapper.updateById(productOptionGroupEntity);
        return productOptionGroupEntity;
    }
}