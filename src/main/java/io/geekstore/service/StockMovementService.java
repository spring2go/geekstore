/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.*;
import io.geekstore.exception.InternalServerError;
import io.geekstore.mapper.OrderLineEntityMapper;
import io.geekstore.mapper.ProductVariantEntityMapper;
import io.geekstore.mapper.StockMovementEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.product.StockMovementListOptions;
import io.geekstore.types.stock.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class StockMovementService {
    private final StockMovementEntityMapper stockMovementEntityMapper;
    private final ProductVariantEntityMapper productVariantEntityMapper;
    private final OrderLineEntityMapper orderLineEntityMapper;

    public StockMovementList getStockMovementsByProductVariantId(
            Long productVariantId, StockMovementListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<StockMovementEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<StockMovementEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(StockMovementEntity::getProductVariantId, productVariantId);
        if (options != null && options.getType() != null) {
            queryWrapper.lambda().eq(StockMovementEntity::getType, options.getType());
        }
        IPage<StockMovementEntity> stockMovementEntityPage =
                this.stockMovementEntityMapper.selectPage(page, queryWrapper);

        StockMovementList stockMovementList = new StockMovementList();
        stockMovementList.setTotalItems((int) stockMovementEntityPage.getTotal()); // 设置满足条件总记录数

        if (CollectionUtils.isEmpty(stockMovementEntityPage.getRecords()))
            return stockMovementList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        stockMovementEntityPage.getRecords().forEach(stockMovementEntity -> {
            StockMovement stockMovement = BeanMapper.map(stockMovementEntity, StockMovement.class);
            stockMovementList.getItems().add(stockMovement);
        });

        return stockMovementList;
    }

    public StockMovement adjustProductVariantStock(
            Long productVariantId, Integer oldStockLevel, Integer newStockLevel) {
        if (Objects.equals(oldStockLevel, newStockLevel)) {
            return null;
        }

        Integer delta = newStockLevel - oldStockLevel;

        StockMovementEntity adjustment = new StockMovementEntity();
        adjustment.setType(StockMovementType.ADJUSTMENT);
        adjustment.setQuantity(delta);
        adjustment.setProductVariantId(productVariantId);

        this.stockMovementEntityMapper.insert(adjustment);

        return BeanMapper.map(adjustment, StockMovement.class);
    }

    @Transactional
    public List<StockMovementEntity> createSalesForOrder(OrderEntity orderEntity) {
        if (orderEntity.isActive()) {
            throw new InternalServerError("Cannot create a Sale for an Order which is still active");
        }

        List<StockMovementEntity> saleList = new ArrayList<>();
        for(OrderLineEntity lineEntity : orderEntity.getLines()) {
            StockMovementEntity saleEntity = new StockMovementEntity();
            saleEntity.setType(StockMovementType.SALE);
            saleEntity.setQuantity(lineEntity.getQuantity() * -1);
            saleEntity.setProductVariantId(lineEntity.getProductVariantId());
            saleEntity.setOrderLineId(lineEntity.getId());

            this.stockMovementEntityMapper.insert(saleEntity);

            ProductVariantEntity productVariantEntity =
                    this.productVariantEntityMapper.selectById(lineEntity.getProductVariantId());
            if (productVariantEntity.isTrackInventory()) {
                Integer stockOnHand = productVariantEntity.getStockOnHand() - lineEntity.getQuantity();
                productVariantEntity.setStockOnHand(stockOnHand);
                this.productVariantEntityMapper.updateById(productVariantEntity);
            }

            saleList.add(saleEntity);
        }

        return saleList;
    }

    public List<StockMovement> createCancellationsForOrderItems(List<OrderItemEntity> items) {
        List<StockMovement> stockMovements = new ArrayList<>();
        Map<Long, ProductVariantEntity> variantsMap = new HashMap<>();
        for(OrderItemEntity item : items) {
            ProductVariantEntity productVariant = null;
            OrderLineEntity line = orderLineEntityMapper.selectById(item.getOrderLineId());
            Long productVariantId = line.getProductVariantId();
            if (variantsMap.containsKey(productVariantId)) {
                productVariant = variantsMap.get(productVariantId);
            } else {
                productVariant = this.productVariantEntityMapper.selectById(productVariantId);
                variantsMap.put(productVariantId, productVariant);
            }
            StockMovementEntity cancellation = new StockMovementEntity();
            cancellation.setProductVariantId(productVariantId);
            cancellation.setQuantity(1);
            cancellation.setOrderItemId(item.getId());
            cancellation.setOrderLineId(item.getOrderLineId());
            cancellation.setType(StockMovementType.CANCELLATION);
            this.stockMovementEntityMapper.insert(cancellation);
            stockMovements.add(BeanMapper.map(cancellation, StockMovement.class));
            item.setCancellationId(cancellation.getId()); // 预填充cancellationId

            if (productVariant.isTrackInventory()) {
                productVariant.setStockOnHand(productVariant.getStockOnHand() + 1);
                this.productVariantEntityMapper.updateById(productVariant);
            }
        }
        return stockMovements;
    }
}
