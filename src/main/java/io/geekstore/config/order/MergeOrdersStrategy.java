/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.order;

import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderLineEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Merges both Orders. If the guest order contains items which are already in the
 * existing Order, the guest Order items will be replaced by the existing Order.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class MergeOrdersStrategy implements OrderMergeStrategy {
    @Override
    public List<OrderLineEntity> merge(OrderEntity guestOrderEntity, OrderEntity existingOrderEntity) {
        List<OrderLineEntity> mergedLines = new ArrayList<>(existingOrderEntity.getLines());
        List<OrderLineEntity> guestLines = new ArrayList<>(guestOrderEntity.getLines());
        Collections.reverse(guestLines);
        for(OrderLineEntity guestLine : guestLines) {
            OrderLineEntity existingLine = this.findCorrespondingLine(existingOrderEntity, guestLine);
            if (existingLine == null) {
                mergedLines.add(0, guestLine);
            }
        }
        return mergedLines;
    }

    private OrderLineEntity findCorrespondingLine(OrderEntity existingOrderEntity, OrderLineEntity guestLineEntity) {
        return existingOrderEntity.getLines().stream()
                .filter(line -> Objects.equals(line.getProductVariantId(), guestLineEntity.getProductVariantId()))
                .findFirst().orElse(null);
    }

}
