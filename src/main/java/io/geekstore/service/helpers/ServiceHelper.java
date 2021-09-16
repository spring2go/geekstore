/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.common.Constant;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AddressEntity;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.exception.EntityNotFoundException;
import io.geekstore.service.args.CreateCustomerHistoryEntryArgs;
import io.geekstore.service.args.CreateOrderHistoryEntryArgs;
import io.geekstore.service.args.UpdateCustomerHistoryEntryArgs;
import io.geekstore.service.args.UpdateOrderHistoryEntryArgs;
import io.geekstore.types.common.ListOptions;
import io.geekstore.types.history.HistoryEntryType;
import io.geekstore.types.order.Order;
import io.geekstore.types.order.OrderItem;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
public abstract class ServiceHelper {
    private static String COLUMN_DELETED_AT = "deleted_at";
    private static String FIELD_DELETED_AT = "deletedAt";
    private static String COLUMN_ID = "id";

    public static <T> T getEntityOrThrow(BaseMapper<T> mapper, Class<T> clazz, Long id) {
        QueryWrapper<T> queryWrapper = new QueryWrapper();
        queryWrapper.eq(COLUMN_ID, id);
        boolean supportSoftDelete =
                getAllFields(clazz).contains(FIELD_DELETED_AT);
        if (supportSoftDelete) {
            queryWrapper.isNull(COLUMN_DELETED_AT);
        }
        T entity = mapper.selectOne(queryWrapper);
        if (entity == null) {
            throw new EntityNotFoundException(clazz.getSimpleName(), id);
        }
        return entity;
    }

    private static Set<String> getAllFields(final Class<?> type) {
        return Arrays.stream(type.getDeclaredFields()).map(field -> field.getName()).collect(Collectors.toSet());
    }

    public static PageInfo getListOptions(ListOptions options) {
        int currentPage = Constant.DEFAULT_CURRENT_PAGE;
        if (options != null && options.getCurrentPage() != null) {
            currentPage = options.getCurrentPage();
        }
        int pageSize = Constant.DEFAULT_PAGE_SIZE;
        if (options != null && options.getPageSize() != null) {
            pageSize = options.getPageSize();
        }
        return PageInfo.builder().current(currentPage).size(pageSize).build();
    }

    public static CreateCustomerHistoryEntryArgs buildCreateCustomerHistoryEntryArgs(
            RequestContext ctx, Long customerId, HistoryEntryType type) {
        return buildCreateCustomerHistoryEntryArgs(
                ctx, customerId, type, null
        );
    }

    public static CreateCustomerHistoryEntryArgs buildCreateCustomerHistoryEntryArgs(
            RequestContext ctx, Long customerId, HistoryEntryType type, Map<String, String> data) {
        CreateCustomerHistoryEntryArgs args = new CreateCustomerHistoryEntryArgs();
        args.setCustomerId(customerId);
        args.setCtx(ctx);
        args.setType(type);
        if (data != null && data.size() > 0) {
            args.getData().putAll(data);
        }
        return args;
    }

    public static CreateOrderHistoryEntryArgs buildCreateOrderHistoryEntryArgs(
            RequestContext ctx, Long orderId, HistoryEntryType type, Map<String, String> data) {
        CreateOrderHistoryEntryArgs args = new CreateOrderHistoryEntryArgs();
        args.setOrderId(orderId);
        args.setCtx(ctx);
        args.setType(type);
        args.setData(data);
        return args;
    }

    public static UpdateOrderHistoryEntryArgs buildUpdateOrderHistoryEntryArgs(
            RequestContext ctx, HistoryEntryType type, String note, boolean isPrivateOnly, Long entryId) {
        UpdateOrderHistoryEntryArgs args = new UpdateOrderHistoryEntryArgs();
        args.setType(type);
        args.getData().put("note", note);
        args.setPrivateOnly(isPrivateOnly);
        args.setCtx(ctx);
        args.setEntryId(entryId);
        return args;
    }

    public static UpdateCustomerHistoryEntryArgs buildUpdateCustomerHistoryEntryArgs(
            RequestContext ctx, Long entryId, HistoryEntryType type) {
        return buildUpdateCustomerHistoryEntryArgs(
                ctx, entryId, type, null
        );
    }

    public static UpdateCustomerHistoryEntryArgs buildUpdateCustomerHistoryEntryArgs(
            RequestContext ctx, Long entryId, HistoryEntryType type, Map<String, String> data) {
        UpdateCustomerHistoryEntryArgs args = new UpdateCustomerHistoryEntryArgs();
        args.setEntryId(entryId);
        args.setCtx(ctx);
        args.setType(type);
        if (data != null && data.size() > 0) {
            args.getData().putAll(data);
        }
        return args;
    }

    public static String addressToLine(AddressEntity addressEntity) {
        String result = "";
        if (!StringUtils.isEmpty(addressEntity.getStreetLine1())) {
            result += addressEntity.getStreetLine1();
        }
        if (!StringUtils.isEmpty(addressEntity.getPostalCode())) {
            result += ", " + addressEntity.getPostalCode();
        }
        return result;
    }

    public static Order mapOrderEntityToOrder(OrderEntity orderEntity) {
        Order order = BeanMapper.map(orderEntity, Order.class);
        if (orderEntity.getState() != null) {
            order.setState(orderEntity.getState().name());
        }
        order.setAdjustments(orderEntity.getAdjustments());
        order.setTotal(orderEntity.getTotal());
        order.setTotalQuantity(orderEntity.getTotalQuantity());
        return order;
    }

    public static OrderItem mapOrderItemEntityToOrderItem(OrderItemEntity orderItemEntity) {
        OrderItem orderItem = BeanMapper.map(orderItemEntity, OrderItem.class);
        orderItem.setAdjustments(orderItemEntity.getAdjustments());
        return orderItem;
    }
}
