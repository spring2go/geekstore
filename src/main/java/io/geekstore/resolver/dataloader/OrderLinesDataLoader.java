/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.entity.OrderLineEntity;
import io.geekstore.mapper.OrderItemEntityMapper;
import io.geekstore.mapper.OrderLineEntityMapper;
import io.geekstore.types.order.OrderLine;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dataloader.MappedBatchLoader;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@SuppressWarnings("Duplicates")
@RequiredArgsConstructor
public class OrderLinesDataLoader implements MappedBatchLoader<Long, List<OrderLine>> {

    private final OrderLineEntityMapper orderLineEntityMapper;
    private final OrderItemEntityMapper orderItemEntityMapper;

    @Override
    public CompletionStage<Map<Long, List<OrderLine>>> load(Set<Long> orderIds) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<OrderLineEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(OrderLineEntity::getOrderId, orderIds);
            List<OrderLineEntity> orderLineEntities = this.orderLineEntityMapper.selectList(queryWrapper);

            if (orderLineEntities.size() == 0) {
                Map<Long, List<OrderLine>> emptyMap = new HashMap<>();
                orderIds.forEach(id -> emptyMap.put(id, new ArrayList<>()));
                return emptyMap;
            }

            List<Long> orderLineIds = orderLineEntities.stream()
                    .map(OrderLineEntity::getId)
                    .collect(Collectors.toList());

            // 加载OrderItemEntity，用于计算OrderLineEntity的Adjustments
            QueryWrapper<OrderItemEntity> orderItemEntityQueryWrapper = new QueryWrapper<>();
            orderItemEntityQueryWrapper.lambda().in(OrderItemEntity::getOrderLineId, orderLineIds);
            List<OrderItemEntity> orderItemEntities = orderItemEntityMapper.selectList(orderItemEntityQueryWrapper);
            Map<Long, List<OrderItemEntity>> groupByOrderLineId = orderItemEntities.stream()
                    .collect(Collectors.groupingBy(OrderItemEntity::getOrderLineId));

            Map<Long, List<OrderLine>> groupByOrderId = orderLineEntities.stream()
                    .collect(Collectors.groupingBy(OrderLineEntity::getOrderId,
                            Collectors.mapping(orderLineEntity -> {
                                // 加载OrderItemEntity，用于计算OrderLineEntity的Adjustments
                                List<OrderItemEntity> items =
                                        groupByOrderLineId.get(orderLineEntity.getId());
                                if (!CollectionUtils.isEmpty(items)) {
                                    orderLineEntity.setItems(items);
                                }
                                OrderLine orderLine = BeanMapper.map(orderLineEntity, OrderLine.class);
                                orderLine.setAdjustments(orderLineEntity.getAdjustments());
                                return orderLine; },
                                    Collectors.toList()
                            )));

            return groupByOrderId;
        });
    }
}
