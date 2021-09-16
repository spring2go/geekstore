/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.FacetEntity;
import io.geekstore.entity.FacetValueEntity;
import io.geekstore.mapper.FacetEntityMapper;
import io.geekstore.mapper.FacetValueEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.facet.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class FacetService {

    private final FacetValueService facetValueService;
    private final FacetEntityMapper facetEntityMapper;
    private final FacetValueEntityMapper facetValueEntityMapper;

    public FacetList findAll(FacetListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<FacetEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<FacetEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<FacetEntity> facetEntityPage =
                this.facetEntityMapper.selectPage(page, queryWrapper);

        FacetList facetList = new FacetList();
        facetList.setTotalItems((int) facetEntityPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(facetEntityPage.getRecords()))
            return facetList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        facetEntityPage.getRecords().forEach(facetEntity -> {
            Facet facet = BeanMapper.map(facetEntity, Facet.class);
            facetList.getItems().add(facet);
        });

        return facetList;
    }

    private void buildFilter(QueryWrapper queryWrapper, FacetFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getCode(), "code");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneBooleanOperatorFilter(
                queryWrapper, filterParameter.getPrivateOnly(), "private_only");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    private void buildSortOrder(QueryWrapper queryWrapper, FacetSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCode(), "code");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    public FacetEntity findOne(Long facetId) {
        return facetEntityMapper.selectById(facetId);
    }

    public FacetEntity findByCode(String facetCode) {
        QueryWrapper<FacetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(FacetEntity::getCode, facetCode);
        return this.facetEntityMapper.selectOne(queryWrapper);
    }

    public FacetEntity findByFacetValueId(Long facetValueId) {
        FacetValueEntity facetValueEntity = facetValueService.findOne(facetValueId);
        if (facetValueEntity == null) return null;
        Long facetId = facetValueEntity.getFacetId();
        return facetEntityMapper.selectById(facetId);
    }

    @Transactional
    public FacetEntity create(CreateFacetInput input) {
        FacetEntity facetEntity = new FacetEntity();
        facetEntity.setCode(input.getCode());
        facetEntity.setName(input.getName());
        facetEntity.setPrivateOnly(BooleanUtils.toBoolean(input.getPrivateOnly()));
        this.facetEntityMapper.insert(facetEntity);

        if (!CollectionUtils.isEmpty(input.getValues())) {
            input.getValues().forEach(createFacetValueWithFacetInput -> {
                FacetValueEntity facetValueEntity = new FacetValueEntity();
                facetValueEntity.setFacetId(facetEntity.getId());
                facetValueEntity.setCode(createFacetValueWithFacetInput.getCode());
                facetValueEntity.setName(createFacetValueWithFacetInput.getName());
                this.facetValueEntityMapper.insert(facetValueEntity);
            });
        }
        return facetEntity;
    }

    public FacetEntity update(UpdateFacetInput input) {
        FacetEntity facetEntity =
                ServiceHelper.getEntityOrThrow(this.facetEntityMapper, FacetEntity.class, input.getId());
        BeanMapper.patch(input, facetEntity);
        this.facetEntityMapper.updateById(facetEntity);
        return facetEntity;
    }

    @Transactional
    public DeletionResponse delete(Long id) {
        FacetEntity facetEntity = ServiceHelper.getEntityOrThrow(this.facetEntityMapper, FacetEntity.class, id);

        QueryWrapper<FacetValueEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(FacetValueEntity::getFacetId, facetEntity.getId()).select(FacetValueEntity::getId);
        List<Long> facetValueIds = this.facetValueEntityMapper.selectList(queryWrapper)
                .stream().map(FacetValueEntity::getId).distinct().collect(Collectors.toList());

        FacetValueService.FacetValueUsage facetValueUsage =
                this.facetValueService.checkFacetValueUsage(facetValueIds);

        int productCount = facetValueUsage.getProductCount();
        int variantCount = facetValueUsage.getVariantCount();

        boolean isInUse = productCount > 0 || variantCount > 0;

        String message = "";
        DeletionResult result = null;

        if (!isInUse) {
            QueryWrapper<FacetValueEntity> queryWrapperFVE = new QueryWrapper<>();
            queryWrapperFVE.lambda().eq(FacetValueEntity::getFacetId, id);
            this.facetValueEntityMapper.delete(queryWrapperFVE);
            this.facetEntityMapper.deleteById(id);
            result = DeletionResult.DELETED;
        } else {
            message = "The selected Facet includes FacetValues which are assigned to " +
                    productCount + " Product(s) and " + variantCount + " Variant(s)";
            result = DeletionResult.NOT_DELETED;
        }

        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setMessage(message);
        deletionResponse.setResult(result);

        return deletionResponse;
    }
}
