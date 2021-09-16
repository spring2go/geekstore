/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AdministratorEntity;
import io.geekstore.entity.CustomerHistoryEntryEntity;
import io.geekstore.entity.OrderHistoryEntryEntity;
import io.geekstore.exception.EntityNotFoundException;
import io.geekstore.mapper.CustomerHistoryEntryEntityMapper;
import io.geekstore.mapper.OrderHistoryEntryEntityMapper;
import io.geekstore.service.args.CreateCustomerHistoryEntryArgs;
import io.geekstore.service.args.CreateOrderHistoryEntryArgs;
import io.geekstore.service.args.UpdateCustomerHistoryEntryArgs;
import io.geekstore.service.args.UpdateOrderHistoryEntryArgs;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.history.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


/**
 * The HistoryService is responsible for creating and retrieving HistoryEntry entities.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class HistoryService {
    private final AdministratorService administratorService;
    private final CustomerHistoryEntryEntityMapper customerHistoryEntryEntityMapper;
    private final OrderHistoryEntryEntityMapper orderHistoryEntryEntityMapper;

    public static final String KEY_STRATEGY = "strategy";
    public static final String KEY_OLD_EMAIL_ADDRESS = "oldEmailAddress";
    public static final String KEY_NEW_EMAIL_ADDRESS = "newEmailAddress";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_NOTE = "note";

    @SuppressWarnings("Duplicates")
    public HistoryEntryList getHistoryForOrder(
            Long orderId,
            boolean publicOnly,
            HistoryEntryListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<OrderHistoryEntryEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<OrderHistoryEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrderHistoryEntryEntity::getOrderId, orderId);
        if (publicOnly) {
            queryWrapper.lambda().eq(OrderHistoryEntryEntity::isPrivateOnly, false);
        }
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<OrderHistoryEntryEntity> orderHistoryEntryEntityPage =
                this.orderHistoryEntryEntityMapper.selectPage(page, queryWrapper);

        HistoryEntryList historyEntryList = new HistoryEntryList();
        historyEntryList.setTotalItems((int) orderHistoryEntryEntityPage.getTotal());

        if (CollectionUtils.isEmpty(orderHistoryEntryEntityPage.getRecords()))
            return historyEntryList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        orderHistoryEntryEntityPage.getRecords().forEach(orderHistoryEntryEntity -> {
            HistoryEntry historyEntry = BeanMapper.map(orderHistoryEntryEntity, HistoryEntry.class);
            historyEntryList.getItems().add(historyEntry);
        });

        return historyEntryList;
    }

    @SuppressWarnings("Duplicates")
    public HistoryEntryList getHistoryForCustomer(
            Long customerId, HistoryEntryListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<CustomerHistoryEntryEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<CustomerHistoryEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CustomerHistoryEntryEntity::getCustomerId, customerId);
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<CustomerHistoryEntryEntity> customerHistoryEntryEntityPage =
                this.customerHistoryEntryEntityMapper.selectPage(page, queryWrapper);

        HistoryEntryList historyEntryList = new HistoryEntryList();
        historyEntryList.setTotalItems((int) customerHistoryEntryEntityPage.getTotal());

        if (CollectionUtils.isEmpty(customerHistoryEntryEntityPage.getRecords()))
            return historyEntryList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        customerHistoryEntryEntityPage.getRecords().forEach(customerHistoryEntryEntity -> {
            HistoryEntry historyEntry = BeanMapper.map(customerHistoryEntryEntity, HistoryEntry.class);
            historyEntryList.getItems().add(historyEntry);
        });

        return historyEntryList;
    }

    private void buildSortOrder(QueryWrapper queryWrapper, HistoryEntrySortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, HistoryEntryFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getType(), "type");
        QueryHelper.buildOneBooleanOperatorFilter(queryWrapper, filterParameter.getIsPublic(), "is_public");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    public OrderHistoryEntryEntity createHistoryEntryForOrder(CreateOrderHistoryEntryArgs args) {
        return this.createHistoryEntryForOrder(args, false);
    }

    public OrderHistoryEntryEntity createHistoryEntryForOrder(CreateOrderHistoryEntryArgs args, boolean privateOnly) {
        AdministratorEntity administratorEntity = this.getAdministratorFromContext(args.getCtx());
        OrderHistoryEntryEntity orderHistoryEntryEntity = new OrderHistoryEntryEntity();
        orderHistoryEntryEntity.setType(args.getType());
        orderHistoryEntryEntity.setPrivateOnly(privateOnly);
        orderHistoryEntryEntity.setData(args.getData());
        orderHistoryEntryEntity.setAdministratorId(administratorEntity != null ? administratorEntity.getId() : null);
        orderHistoryEntryEntity.setOrderId(args.getOrderId());
        this.orderHistoryEntryEntityMapper.insert(orderHistoryEntryEntity);
        return orderHistoryEntryEntity;
    }

    public OrderHistoryEntryEntity updateOrderHistoryEntry(
            RequestContext ctx, UpdateOrderHistoryEntryArgs args) {
        QueryWrapper<OrderHistoryEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrderHistoryEntryEntity::getType, args.getType())
                .eq(OrderHistoryEntryEntity::getId, args.getEntryId());
        OrderHistoryEntryEntity entry = this.orderHistoryEntryEntityMapper.selectOne(queryWrapper);
        if (entry == null) {
            throw new EntityNotFoundException("OrderHistoryEntryEntity", args.getEntryId());
        }
        if (args.getData().size() > 0) {
            entry.setData(args.getData());
        }
        entry.setPrivateOnly(args.isPrivateOnly());
        AdministratorEntity administratorEntity = this.getAdministratorFromContext(ctx);
        entry.setAdministratorId(administratorEntity.getId());
        this.orderHistoryEntryEntityMapper.updateById(entry);
        return entry;
    }

    public void deleteOrderHistoryEntry(Long id) {
        // 确保存在
        ServiceHelper.getEntityOrThrow(this.orderHistoryEntryEntityMapper, OrderHistoryEntryEntity.class, id);
        this.orderHistoryEntryEntityMapper.deleteById(id);
    }

    public CustomerHistoryEntryEntity createHistoryEntryForCustomer(CreateCustomerHistoryEntryArgs args) {
        return this.createHistoryEntryForCustomer(args, false);
    }

    public CustomerHistoryEntryEntity createHistoryEntryForCustomer(
            CreateCustomerHistoryEntryArgs args, Boolean privateOnly) {
        AdministratorEntity administratorEntity =
                this.getAdministratorFromContext(args.getCtx());
        CustomerHistoryEntryEntity customerHistoryEntryEntity =
                BeanMapper.map(args, CustomerHistoryEntryEntity.class);
        customerHistoryEntryEntity.setAdministratorId(administratorEntity == null ? null : administratorEntity.getId());
        customerHistoryEntryEntity.setPrivateOnly(BooleanUtils.toBoolean(privateOnly));
        this.customerHistoryEntryEntityMapper.insert(customerHistoryEntryEntity);
        return customerHistoryEntryEntity;
    }

    public CustomerHistoryEntryEntity updateCustomerHistoryEntry(UpdateCustomerHistoryEntryArgs args) {
        QueryWrapper<CustomerHistoryEntryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CustomerHistoryEntryEntity::getId, args.getEntryId())
                .eq(CustomerHistoryEntryEntity::getType, args.getType());
        CustomerHistoryEntryEntity customerHistoryEntryEntity =
                this.customerHistoryEntryEntityMapper.selectOne(queryWrapper);
        if (customerHistoryEntryEntity == null) {
            throw new EntityNotFoundException("CustomerHistoryEntry", args.getEntryId());
        }

        if (args.getData() != null && !args.getData().isEmpty()) {
            customerHistoryEntryEntity.setData(args.getData());
        }
        AdministratorEntity administratorEntity = this.getAdministratorFromContext(args.getCtx());
        if (administratorEntity != null) {
            customerHistoryEntryEntity.setCustomerId(administratorEntity.getId());
        }
        this.customerHistoryEntryEntityMapper.updateById(customerHistoryEntryEntity);
        return customerHistoryEntryEntity;
    }

    public void deleteCustomerHistoryEntry(Long id) {
        // 确保存在
        ServiceHelper.getEntityOrThrow(this.customerHistoryEntryEntityMapper, CustomerHistoryEntryEntity.class, id);
        this.customerHistoryEntryEntityMapper.deleteById(id);
    }

    private AdministratorEntity getAdministratorFromContext(RequestContext ctx) {
        if (ctx.getActiveUserId() == null) return null;

        return this.administratorService.findOneEntityByUserId(ctx.getActiveUserId());
    }
}
