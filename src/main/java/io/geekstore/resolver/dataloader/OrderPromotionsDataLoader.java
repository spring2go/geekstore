/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.OrderPromotionJoinEntity;
import io.geekstore.entity.PromotionEntity;
import io.geekstore.mapper.OrderPromotionJoinEntityMapper;
import io.geekstore.mapper.PromotionEntityMapper;
import io.geekstore.types.promotion.Promotion;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
public class OrderPromotionsDataLoader implements MappedBatchLoader<Long, List<Promotion>> {

    private final OrderPromotionJoinEntityMapper orderPromotionJoinEntityMapper;
    private final PromotionEntityMapper promotionEntityMapper;

    public OrderPromotionsDataLoader(
            OrderPromotionJoinEntityMapper orderPromotionJoinEntityMapper,
            PromotionEntityMapper promotionEntityMapper) {
        this.orderPromotionJoinEntityMapper = orderPromotionJoinEntityMapper;
        this.promotionEntityMapper = promotionEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, List<Promotion>>> load(Set<Long> orderIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, List<Promotion>> orderPromotionsMap = new HashMap<>();
            orderIds.forEach(id -> orderPromotionsMap.put(id, new ArrayList<>()));

            QueryWrapper<OrderPromotionJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(OrderPromotionJoinEntity::getOrderId, orderIds);
            List<OrderPromotionJoinEntity> orderPromotionJoinEntities =
                    orderPromotionJoinEntityMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(orderPromotionJoinEntities)) return orderPromotionsMap;

            Set<Long> promotionIds = orderPromotionJoinEntities.stream()
                    .map(OrderPromotionJoinEntity::getPromotionId).collect(Collectors.toSet());
            List<PromotionEntity> promotionEntities = promotionEntityMapper.selectBatchIds(promotionIds);
            if (CollectionUtils.isEmpty(promotionEntities)) return orderPromotionsMap;

            Map<Long, PromotionEntity> promotionEntityMap = promotionEntities.stream()
                    .collect(Collectors.toMap(PromotionEntity::getId, promotionEntity -> promotionEntity));

            orderPromotionJoinEntities.forEach(joinEntity -> {
                Long orderId = joinEntity.getOrderId();
                Long promotionId = joinEntity.getPromotionId();
                List<Promotion> promotions = orderPromotionsMap.get(orderId);
                PromotionEntity promotionEntity = promotionEntityMap.get(promotionId);
                Promotion promotion = BeanMapper.patch(promotionEntity, Promotion.class);
                promotions.add(promotion);
            });

            return orderPromotionsMap;
        });
    }
}
