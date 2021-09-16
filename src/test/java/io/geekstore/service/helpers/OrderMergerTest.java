/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.config.order.MergeOrdersStrategy;
import io.geekstore.config.order.OrderMergeOptions;
import io.geekstore.config.order.SimpleLine;
import io.geekstore.entity.OrderEntity;
import io.geekstore.service.helpers.order_merger.LineItem;
import io.geekstore.service.helpers.order_merger.MergeResult;
import io.geekstore.service.helpers.order_merger.OrderMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static io.geekstore.config.order.OrderTestUtils.createOrderFromLines;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 *
 * 参考：
 * https://reflectoring.io/unit-testing-spring-boot/
 */
public class OrderMergerTest {
    OrderMerger orderMerger;

    @BeforeEach
    void beforeEach() {
        OrderMergeOptions orderMergeOptions =
                new OrderMergeOptions(new MergeOrdersStrategy(), null);
        orderMerger = new OrderMerger(orderMergeOptions);
    }


    @Test
    public void test_both_orders_null() {
        OrderEntity guestOrder = new OrderEntity();
        OrderEntity existingOrder = new OrderEntity();

        MergeResult result = orderMerger.merge(guestOrder, existingOrder);
        assertThat(result.getOrder()).isNull();
        assertThat(result.getLinesToInsert()).isNull();
        assertThat(result.getOrderToDelete()).isNull();
    }

    @Test
    public void test_guestOrder_null() {
        OrderEntity existingOrder = createOrderFromLines(
                Arrays.asList(
                        SimpleLine.builder()
                                .lineId(1L)
                                .quantity(2)
                                .productVariantId(100L)
                                .build()
                )
        );
        MergeResult result = orderMerger.merge(null, existingOrder);
        assertThat(result.getOrder()).isEqualTo(existingOrder);
        assertThat(result.getLinesToInsert()).isNull();
        assertThat(result.getOrderToDelete()).isNull();
    }

    @Test
    public void test_existingOrder_null() {
        OrderEntity guestOrder = createOrderFromLines(
                Arrays.asList(
                        SimpleLine.builder()
                                .lineId(1L)
                                .quantity(2)
                                .productVariantId(100L)
                                .build()
                )
        );
        MergeResult result = orderMerger.merge(guestOrder, null);
        assertThat(result.getOrder()).isEqualTo(guestOrder);
        assertThat(result.getLinesToInsert()).isNull();
        assertThat(result.getOrderToDelete()).isNull();
    }

    @Test
    public void test_empty_guestOrder() {
        OrderEntity guestOrder = createOrderFromLines(
                new ArrayList<>()
        );
        guestOrder.setId(42L);
        OrderEntity existingOrder = createOrderFromLines(
                Arrays.asList(
                        SimpleLine.builder()
                                .lineId(1L)
                                .quantity(2)
                                .productVariantId(100L)
                                .build()
                )
        );

        MergeResult result = orderMerger.merge(guestOrder, existingOrder);

        assertThat(result.getOrder()).isEqualTo(existingOrder);
        assertThat(result.getLinesToInsert()).isNull();
        assertThat(result.getOrderToDelete()).isEqualTo(guestOrder);
    }

    @Test
    public void test_empty_existingOrder() {
        OrderEntity guestOrder = createOrderFromLines(
                Arrays.asList(
                        SimpleLine.builder()
                                .lineId(1L)
                                .quantity(2)
                                .productVariantId(100L)
                                .build()
                )
        );
        OrderEntity existingOrder = createOrderFromLines(
                new ArrayList<>()
        );
        existingOrder.setId(42L);

        MergeResult result = orderMerger.merge(guestOrder, existingOrder);

        assertThat(result.getOrder()).isEqualTo(guestOrder);
        assertThat(result.getLinesToInsert()).isNull();
        assertThat(result.getOrderToDelete()).isEqualTo(existingOrder);
    }

    @Test
    public void new_lines_added_by_merge() {
        OrderEntity guestOrder = createOrderFromLines(
                Arrays.asList(
                        SimpleLine.builder()
                                .lineId(20L)
                                .quantity(2)
                                .productVariantId(200L)
                                .build()
                )
        );
        guestOrder.setId(42L);
        OrderEntity existingOrder = createOrderFromLines(
                Arrays.asList(
                        SimpleLine.builder()
                                .lineId(1L)
                                .quantity(2)
                                .productVariantId(100L)
                                .build()
                )
        );


        MergeResult result = orderMerger.merge(guestOrder, existingOrder);

        assertThat(result.getOrder()).isEqualTo(existingOrder);
        assertThat(result.getLinesToInsert())
                .containsExactly(LineItem.builder().productVariantId(200L).quantity(2).build());
        assertThat(result.getOrderToDelete()).isEqualTo(guestOrder);
    }

}
