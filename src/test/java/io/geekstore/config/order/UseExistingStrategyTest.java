/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.order;

import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderLineEntity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static io.geekstore.config.order.OrderTestUtils.createOrderFromLines;
import static io.geekstore.config.order.OrderTestUtils.parseLines;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 */
public class UseExistingStrategyTest {
    private OrderMergeStrategy strategy = new UseExistingStrategy();

    @Test
    public void test_both_orders_empty() {
        OrderEntity guestOrder = new OrderEntity();
        OrderEntity existingOrder = new OrderEntity();

        List<OrderLineEntity> result = strategy.merge(guestOrder, existingOrder);
        assertThat(result).isEmpty();
    }

    @Test
    public void test_existingOrder_empty() {
        SimpleLine simpleLine = SimpleLine.builder()
                .lineId(1L).quantity(2).productVariantId(100L).build();

        List<SimpleLine> guestLines = Arrays.asList(simpleLine);

        OrderEntity guestOrder = createOrderFromLines(guestLines);
        OrderEntity existingOrder = new OrderEntity();

        List<OrderLineEntity> result = strategy.merge(guestOrder, existingOrder);
        assertThat(parseLines(result)).isEmpty();
    }

    @Test
    public void test_guestOrder_empty() {
        SimpleLine simpleLine = SimpleLine.builder()
                .lineId(1L).quantity(2).productVariantId(100L).build();
        List<SimpleLine> existingLines = Arrays.asList(simpleLine);

        OrderEntity guestOrder = new OrderEntity();
        OrderEntity existingOrder = createOrderFromLines(existingLines);

        List<OrderLineEntity> result = strategy.merge(guestOrder, existingOrder);

        assertThat(parseLines(result)).isEqualTo(existingLines);
    }

    @Test
    public void test_both_orders_have_non_conflicting_lines() {
        List<SimpleLine> guestLines = Arrays.asList(
                SimpleLine.builder().lineId(21L).quantity(2).productVariantId(201L).build(),
                SimpleLine.builder().lineId(22L).quantity(1).productVariantId(202L).build()
        );

        List<SimpleLine> existingLines = Arrays.asList(
                SimpleLine.builder().lineId(1L).quantity(1).productVariantId(101L).build(),
                SimpleLine.builder().lineId(2L).quantity(1).productVariantId(102L).build(),
                SimpleLine.builder().lineId(3L).quantity(1).productVariantId(103L).build()
        );

        OrderEntity guestOrder = createOrderFromLines(guestLines);
        OrderEntity existingOrder = createOrderFromLines(existingLines);

        List<OrderLineEntity> result = strategy.merge(guestOrder, existingOrder);

        assertThat(parseLines(result)).containsExactly(
                existingLines.toArray(new SimpleLine[0])
        );
    }

    @Test
    public void test_orders_have_conflicting_lines_some_of_which_conflict() {
        List<SimpleLine> guestLines = Arrays.asList(
                SimpleLine.builder().lineId(21L).quantity(2).productVariantId(102L).build(),
                SimpleLine.builder().lineId(22L).quantity(1).productVariantId(202L).build()
        );

        List<SimpleLine> existingLines = Arrays.asList(
                SimpleLine.builder().lineId(1L).quantity(1).productVariantId(101L).build(),
                SimpleLine.builder().lineId(2L).quantity(1).productVariantId(102L).build(),
                SimpleLine.builder().lineId(3L).quantity(1).productVariantId(103L).build()
        );

        OrderEntity guestOrder = createOrderFromLines(guestLines);
        OrderEntity existingOrder = createOrderFromLines(existingLines);

        List<OrderLineEntity> result = strategy.merge(guestOrder, existingOrder);

        assertThat(parseLines(result)).containsExactly(
                existingLines.toArray(new SimpleLine[0])
        );
    }

    @Test
    public void test_returns_a_new_array() {
        List<SimpleLine> guestLines = Arrays.asList(
                SimpleLine.builder().lineId(21L).quantity(2).productVariantId(102L).build()
        );

        List<SimpleLine> existingLines = Arrays.asList(
                SimpleLine.builder().lineId(1L).quantity(1).productVariantId(101L).build()
        );

        OrderEntity guestOrder = createOrderFromLines(guestLines);
        OrderEntity existingOrder = createOrderFromLines(existingLines);

        List<OrderLineEntity> result = strategy.merge(guestOrder, existingOrder);

        assertThat(result == guestOrder).isFalse();
        assertThat(result == existingOrder).isFalse();
    }
}
