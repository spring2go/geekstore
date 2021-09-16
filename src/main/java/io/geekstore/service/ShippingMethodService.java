/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.ShippingMethodEntity;
import io.geekstore.exception.EntityNotFoundException;
import io.geekstore.mapper.ShippingMethodEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.service.helpers.ShippingConfiguration;
import io.geekstore.types.common.ConfigurableOperationDefinition;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.shipping.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class ShippingMethodService {
    private final ShippingConfiguration shippingConfiguration;
    private final ShippingMethodEntityMapper shippingMethodEntityMapper;

    private List<ShippingMethodEntity> activeShippingMethods;

    public ShippingMethodList findAll(ShippingMethodListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<ShippingMethodEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<ShippingMethodEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        queryWrapper.lambda().isNull(ShippingMethodEntity::getDeletedAt); // 未删除
        IPage<ShippingMethodEntity> shippingMethodEntityPage =
                this.shippingMethodEntityMapper.selectPage(page, queryWrapper);

        ShippingMethodList shippingMethodList = new ShippingMethodList();
        shippingMethodList.setTotalItems((int) shippingMethodEntityPage.getTotal());

        if (CollectionUtils.isEmpty(shippingMethodEntityPage.getRecords())) return shippingMethodList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        shippingMethodEntityPage.getRecords().forEach(shippingMethodEntity -> {
            ShippingMethod shippingMethod = BeanMapper.map(shippingMethodEntity, ShippingMethod.class);
            shippingMethodList.getItems().add(shippingMethod);
        });

        return shippingMethodList;
    }

    private void buildSortOrder(QueryWrapper queryWrapper, ShippingMethodSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCode(), "code");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getDescription(), "description");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, ShippingMethodFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getCode(), "code");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getDescription(), "description");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    public ShippingMethodEntity findOne(Long shippingMethodId) {
        QueryWrapper<ShippingMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ShippingMethodEntity::getId, shippingMethodId)
                .isNull(ShippingMethodEntity::getDeletedAt);
        return this.shippingMethodEntityMapper.selectOne(queryWrapper);
    }

    public ShippingMethodEntity create(CreateShippingMethodInput input) {
        ShippingMethodEntity shippingMethodEntity = BeanMapper.map(input, ShippingMethodEntity.class);
        shippingMethodEntity.setChecker(this.shippingConfiguration.parseCheckerInput(input.getChecker()));
        shippingMethodEntity.setCalculator(this.shippingConfiguration.parseCalculatorInput(input.getCalculator()));
        this.shippingMethodEntityMapper.insert(shippingMethodEntity);
        this.updateActiveShippingMethods();
        return shippingMethodEntity;
    }

    public ShippingMethodEntity update(UpdateShippingMethodInput input) {
        ShippingMethodEntity shippingMethodEntity = this.findOne(input.getId());
        if (shippingMethodEntity == null) {
            throw new EntityNotFoundException("ShippingMethod", input.getId());
        }
        BeanMapper.patch(input, shippingMethodEntity);
        if (input.getChecker() != null) {
            shippingMethodEntity.setChecker(this.shippingConfiguration.parseCheckerInput(input.getChecker()));
        }
        if (input.getCalculator() != null) {
            shippingMethodEntity.setCalculator(this.shippingConfiguration.parseCalculatorInput(input.getCalculator()));
        }
        this.shippingMethodEntityMapper.updateById(shippingMethodEntity);
        this.updateActiveShippingMethods();
        return shippingMethodEntity;
    }

    public DeletionResponse softDelete(Long id) {
        ShippingMethodEntity shippingMethodEntity =
                ServiceHelper.getEntityOrThrow(shippingMethodEntityMapper, ShippingMethodEntity.class, id);
        shippingMethodEntity.setDeletedAt(new Date());
        this.shippingMethodEntityMapper.updateById(shippingMethodEntity);
        this.updateActiveShippingMethods();
        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setResult(DeletionResult.DELETED);
        return deletionResponse;
    }

    public List<ConfigurableOperationDefinition> getShippingEligibilityCheckers() {
        return this.shippingConfiguration.getShippingEligibilityCheckers().stream()
                .map(x -> x.toGraphQLType()).collect(Collectors.toList());
    }

    public List<ConfigurableOperationDefinition> getShippingCalculators() {
        return this.shippingConfiguration.getShippingCalculators().stream()
                .map(x -> x.toGraphQLType()).collect(Collectors.toList());
    }

    public List<ShippingMethodEntity> getActiveShippingMethods() {
        return this.activeShippingMethods;
    }

    @PostConstruct
    void updateActiveShippingMethods() {
        QueryWrapper<ShippingMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(ShippingMethodEntity::getDeletedAt);
        this.activeShippingMethods  = this.shippingMethodEntityMapper.selectList(queryWrapper);
    }

}
