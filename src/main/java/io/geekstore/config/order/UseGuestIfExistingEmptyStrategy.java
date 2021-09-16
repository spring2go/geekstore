/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.order;

import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderLineEntity;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * If the existing order is empty, then the guest order is used. Otherwise the existing order is used.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class UseGuestIfExistingEmptyStrategy implements OrderMergeStrategy {
    @Override
    public List<OrderLineEntity> merge(OrderEntity guestOrderEntity, OrderEntity existingOrderEntity) {
        return CollectionUtils.isEmpty(existingOrderEntity.getLines()) ? new ArrayList<>(guestOrderEntity.getLines())
                : new ArrayList<>(existingOrderEntity.getLines());
    }
}
