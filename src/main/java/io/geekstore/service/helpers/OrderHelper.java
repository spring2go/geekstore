/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.entity.OrderLineEntity;
import io.geekstore.entity.PaymentEntity;
import io.geekstore.mapper.OrderEntityMapper;
import io.geekstore.mapper.OrderItemEntityMapper;
import io.geekstore.mapper.OrderLineEntityMapper;
import io.geekstore.mapper.PaymentEntityMapper;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class OrderHelper {
    private final PaymentEntityMapper paymentEntityMapper;
    private final OrderEntityMapper orderEntityMapper;
    private final OrderItemEntityMapper orderItemEntityMapper;
    private final OrderLineEntityMapper orderLineEntityMapper;

    /**
     * Returns true if the Order total is covered by Payments in the specified state.
     */
    public boolean orderTotalIsCovered(OrderEntity orderEntity, PaymentState state) {
        QueryWrapper<PaymentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PaymentEntity::getOrderId, orderEntity.getId());
        List<PaymentEntity> paymentEntities = this.paymentEntityMapper.selectList(queryWrapper);
        List<PaymentEntity> filteredPaymentEntities = paymentEntities.stream()
                .filter(p -> Objects.equals(p.getState(), state)).collect(Collectors.toList());
        int sum = 0;
        for(PaymentEntity paymentEntity: filteredPaymentEntities) {
            sum += paymentEntity.getAmount();
        }
        return sum >= orderEntity.getTotal();
    }

    public boolean orderItemsAreFulfilled(OrderEntity orderEntity) {
        return getOrderItems(orderEntity).stream()
                .filter(orderItemEntity -> !orderItemEntity.isCancelled())
                .allMatch(this::isFulfilled);
    }

    /**
     * Returns true if at least one, but not all (non-cancelled) OrderItems are fulfilled.
     */
    public boolean orderItemArePartiallyFulfilled(OrderEntity orderEntity) {
        List<OrderItemEntity> nonCancelledItems = getOrderItems(orderEntity).stream()
                .filter(orderItemEntity -> !orderItemEntity.isCancelled()).collect(Collectors.toList());
        return nonCancelledItems.stream().anyMatch(this::isFulfilled) &&
                !nonCancelledItems.stream().allMatch(this::isFulfilled);
    }

    public List<Long> getOrderItemIds(OrderEntity order) {
        return getOrderItems(order).stream().map(OrderItemEntity::getId).collect(Collectors.toList());
    }

    public List<Long> getOrderItemIds(OrderLineEntity line) {
        return line.getItems().stream().map(OrderItemEntity::getId).collect(Collectors.toList());
    }

    public List<Long> getOrderLineIds(OrderEntity order) {
        return order.getLines().stream().map(OrderLineEntity::getId).collect(Collectors.toList());
    }

    /**
     * Returns true if all OrderItems in the order are cancelled.
     */
    public boolean orderItemsAreAllCancelled(OrderEntity orderEntity) {
        return getOrderItems(orderEntity).stream().allMatch(item -> item.isCancelled());
    }

    private List<OrderItemEntity> getOrderItems(OrderEntity orderEntity) {
        List<OrderItemEntity> result = new ArrayList<>();
        orderEntity.getLines().forEach(line -> {
            result.addAll(line.getItems());
        });
        return result;
    }

    private boolean isFulfilled(OrderItemEntity orderItemEntity) {
        return orderItemEntity.getFulfillmentId() != null;
    }

    public OrderEntity findOrderWithItems(Long orderId) {
        OrderEntity order = this.orderEntityMapper.selectById(orderId);
        if (order == null) return null;

        populateOrderWithItems(order);

        return order;
    }

    public OrderEntity getOrderWithItems(Long orderId) {
        OrderEntity order = ServiceHelper.getEntityOrThrow(
                this.orderEntityMapper, OrderEntity.class, orderId);

        populateOrderWithItems(order);

        return order;
    }

    private void populateOrderWithItems(OrderEntity order) {
        QueryWrapper<OrderLineEntity> orderLineEntityQueryWrapper = new QueryWrapper<>();
        orderLineEntityQueryWrapper.lambda().eq(OrderLineEntity::getOrderId, order.getId())
                .orderByAsc(OrderLineEntity::getCreatedAt);
        List<OrderLineEntity> orderLines = this.orderLineEntityMapper.selectList(orderLineEntityQueryWrapper);
        order.setLines(orderLines);

        // 预填充OrderItems
        for(OrderLineEntity orderLine : orderLines) {
            QueryWrapper<OrderItemEntity> orderItemEntityQueryWrapper = new QueryWrapper<>();
            orderItemEntityQueryWrapper.lambda().eq(OrderItemEntity::getOrderLineId, orderLine.getId())
                    .orderByAsc(OrderItemEntity::getCreatedAt);
            List<OrderItemEntity> orderItems = this.orderItemEntityMapper.selectList(orderItemEntityQueryWrapper);
            orderLine.setItems(orderItems);
        }
    }
}
