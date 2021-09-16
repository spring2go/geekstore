/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CustomerEntity;
import io.geekstore.entity.CustomerGroupEntity;
import io.geekstore.entity.CustomerGroupJoinEntity;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.CustomerEntityMapper;
import io.geekstore.mapper.CustomerGroupEntityMapper;
import io.geekstore.mapper.CustomerGroupJoinEntityMapper;
import io.geekstore.service.args.CreateCustomerHistoryEntryArgs;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.customer.*;
import io.geekstore.types.history.HistoryEntryType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class CustomerGroupService {

    private final CustomerGroupEntityMapper customerGroupEntityMapper;
    private final CustomerEntityMapper customerEntityMapper;
    private final CustomerGroupJoinEntityMapper customerGroupJoinEntityMapper;
    private final HistoryService historyService;
    private final CustomerService customerService;

    public CustomerGroupList findAll(CustomerGroupListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<CustomerGroupEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<CustomerGroupEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<CustomerGroupEntity> customerGroupEntityPage =
                this.customerGroupEntityMapper.selectPage(page, queryWrapper);

        CustomerGroupList customerGroupList = new CustomerGroupList();
        customerGroupList.setTotalItems((int) customerGroupEntityPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(customerGroupEntityPage.getRecords()))
            return customerGroupList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        customerGroupEntityPage.getRecords().forEach(customerGroupEntity -> {
            CustomerGroup customerGroup = BeanMapper.map(customerGroupEntity, CustomerGroup.class);
            customerGroupList.getItems().add(customerGroup);
        });

        return customerGroupList;
    }

    public CustomerList getGroupCustomers(Long customerGroupId, CustomerListOptions options) {
        QueryWrapper<CustomerGroupJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CustomerGroupJoinEntity::getGroupId, customerGroupId);
        List<CustomerGroupJoinEntity> customerGroupJoinEntityList =
                this.customerGroupJoinEntityMapper.selectList(queryWrapper);

        List<Long> customerIds =
                customerGroupJoinEntityList.stream().map(CustomerGroupJoinEntity::getCustomerId)
                .collect(Collectors.toList());

        return customerService.findAll(options, customerIds);
    }

    private void buildSortOrder(QueryWrapper queryWrapper, CustomerGroupSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, CustomerGroupFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    public CustomerGroupEntity findOne(Long customerGroupId) {
        return this.customerGroupEntityMapper.selectById(customerGroupId);
    }

    public CustomerGroupEntity create(RequestContext ctx, CreateCustomerGroupInput input) {
        CustomerGroupEntity groupEntity = new CustomerGroupEntity();
        groupEntity.setName(input.getName());

        this.customerGroupEntityMapper.insert(groupEntity);

        if (!CollectionUtils.isEmpty(input.getCustomerIds())) {
            Set<Long> existingCustomerIds = this.getCustomerIdsFromIds(input.getCustomerIds());
            for(Long customerId : existingCustomerIds) {
                CustomerGroupJoinEntity customerGroupJoinEntity = new CustomerGroupJoinEntity();
                customerGroupJoinEntity.setCustomerId(customerId);
                customerGroupJoinEntity.setGroupId(groupEntity.getId());
                this.customerGroupJoinEntityMapper.insert(customerGroupJoinEntity);

                this.createHistoryEntry(
                        ctx, customerId, groupEntity.getName(), HistoryEntryType.CUSTOMER_ADDED_TO_GROUP);
            }
        }

        return groupEntity;
    }

    public CustomerGroupEntity update(UpdateCustomerGroupInput input) {
        CustomerGroupEntity customerGroupEntity =
                ServiceHelper.getEntityOrThrow(
                        this.customerGroupEntityMapper, CustomerGroupEntity.class, input.getId());
        BeanMapper.patch(input, customerGroupEntity);

        this.customerGroupEntityMapper.updateById(customerGroupEntity);

        return customerGroupEntity;
    }


    @Transactional
    public DeletionResponse delete(Long id) {
        // 确保存在
        CustomerGroupEntity customerGroupEntity =
                ServiceHelper.getEntityOrThrow(this.customerGroupEntityMapper, CustomerGroupEntity.class, id);

        DeletionResponse deletionResponse = new DeletionResponse();
        try {
            // 先删除join关联表数据
            QueryWrapper<CustomerGroupJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(CustomerGroupJoinEntity::getGroupId, id);
            this.customerGroupJoinEntityMapper.delete(queryWrapper);
            // 再删除group组数据
            this.customerGroupEntityMapper.deleteById(id);
            deletionResponse.setResult(DeletionResult.DELETED);
        } catch (Exception ex) {
            deletionResponse.setResult(DeletionResult.NOT_DELETED);
            deletionResponse.setMessage(ex.getMessage());
        }
        return deletionResponse;
    }

    public CustomerGroupEntity addCustomersToGroup(RequestContext ctx, Long customerGroupId, List<Long> customerIds) {
        // 确保存在
        Set<Long> existingCustomerIds = this.getCustomerIdsFromIds(customerIds);
        // 确保存在
        CustomerGroupEntity groupEntity =
                ServiceHelper.getEntityOrThrow(
                        this.customerGroupEntityMapper, CustomerGroupEntity.class, customerGroupId);
        for(Long customerId : existingCustomerIds) {
            QueryWrapper<CustomerGroupJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(CustomerGroupJoinEntity::getCustomerId, customerId)
                    .eq(CustomerGroupJoinEntity::getGroupId, customerGroupId);
            if (this.customerGroupJoinEntityMapper.selectCount(queryWrapper) == 0) {
                CustomerGroupJoinEntity customerGroupJoinEntity = new CustomerGroupJoinEntity();
                customerGroupJoinEntity.setCustomerId(customerId);
                customerGroupJoinEntity.setGroupId(customerGroupId);
                this.customerGroupJoinEntityMapper.insert(customerGroupJoinEntity);

                this.createHistoryEntry(
                        ctx, customerId, groupEntity.getName(), HistoryEntryType.CUSTOMER_ADDED_TO_GROUP);
            }
        }

        return groupEntity;
    }

    public CustomerGroupEntity removeCustomersFromGroup(
            RequestContext ctx, Long customerGroupId, List<Long> customerIds) {
        // 确保存在
        Set<Long> existingCustomerIds = this.getCustomerIdsFromIds(customerIds);
        // 确保存在
        CustomerGroupEntity groupEntity =
                ServiceHelper.getEntityOrThrow(
                        this.customerGroupEntityMapper, CustomerGroupEntity.class, customerGroupId);
        for(Long customerId : existingCustomerIds) {
            QueryWrapper<CustomerGroupJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(CustomerGroupJoinEntity::getCustomerId, customerId)
                    .eq(CustomerGroupJoinEntity::getGroupId, customerGroupId);
            if (this.customerGroupJoinEntityMapper.selectCount(queryWrapper) == 0) {
                String errorMessage =
                        String.format("Customer '%d' does not belong to this CustomerGroup '%d'",
                                customerId, customerGroupId);
                throw new UserInputException(errorMessage);
            }
            this.customerGroupJoinEntityMapper.delete(queryWrapper);
            this.createHistoryEntry(
                    ctx, customerId, groupEntity.getName(), HistoryEntryType.CUSTOMER_REMOVED_FROM_GROUP);
        }

        return groupEntity;
    }

    private void createHistoryEntry(RequestContext ctx, Long customerId, String groupName, HistoryEntryType type) {
        CreateCustomerHistoryEntryArgs args = new CreateCustomerHistoryEntryArgs();
        args.setType(type);
        args.setCtx(ctx);
        args.setCustomerId(customerId);
        args.getData().put("groupName", groupName);
        this.historyService.createHistoryEntryForCustomer(args);
    }


    private Set<Long> getCustomerIdsFromIds(List<Long> ids) {
        QueryWrapper<CustomerEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(CustomerEntity::getId, ids)
                .isNull(CustomerEntity::getDeletedAt)
                .select(CustomerEntity::getId);
        List<CustomerEntity> customerEntityList = customerEntityMapper.selectList(queryWrapper);
        return customerEntityList.stream().map(customerEntity -> customerEntity.getId())
                .collect(Collectors.toSet());
    }
}
