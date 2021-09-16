/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.config.promotion.PromotionItemAction;
import io.geekstore.config.promotion.PromotionOrderAction;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.OrderItemEntity;
import io.geekstore.entity.OrderLineEntity;
import io.geekstore.entity.PromotionEntity;
import io.geekstore.mapper.ShippingMethodEntityMapper;
import io.geekstore.service.helpers.order_calculator.OrderCalculator;
import io.geekstore.service.helpers.shipping_calculator.ShippingCalculator;
import io.geekstore.types.common.ConfigArg;
import io.geekstore.types.common.ConfigArgDefinition;
import io.geekstore.types.common.ConfigurableOperation;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Created on Dec, 2020 by @author bobo
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderCalculatorTest {
    OrderCalculator orderCalculator;

    @BeforeEach
    void beforeEach() {
        ShippingCalculator shippingCalculator = Mockito.mock(ShippingCalculator.class);
        Mockito.when(shippingCalculator.getEligibleShippingMethods(any())).thenReturn(new ArrayList<>());
        ShippingMethodEntityMapper shippingMethodEntityMapper = Mockito.mock(ShippingMethodEntityMapper.class);
        orderCalculator = new OrderCalculator(shippingCalculator, shippingMethodEntityMapper);
    }

    private OrderEntity createOrder(OrderConfig orderConfig) {
        List<OrderLineEntity> lines = orderConfig.getLines().stream()
                .map(item -> {
                    OrderLineEntity orderLine = new OrderLineEntity();
                    for(int i = 0; i < item.getQuantity(); i++) {
                        OrderItemEntity orderItem = new OrderItemEntity();
                        orderItem.setUnitPrice(item.getUnitPrice());
                        orderLine.getItems().add(orderItem);
                    }
                    return orderLine;
                }).collect(Collectors.toList());
        OrderEntity order = new OrderEntity();
        order.setLines(lines);
        return order;
    }

    /**
     * promotions tests
     */
    PromotionCondition alwaysTrueCondition = new PromotionCondition("always_true_condition", "") {
        @Override
        public boolean check(OrderEntity order, ConfigArgValues argValues) {
            return true;
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            return ImmutableMap.of();
        }
    };

    PromotionCondition orderTotalCondition = new PromotionCondition("order_total_condition", "") {
        @Override
        public boolean check(OrderEntity order, ConfigArgValues argValues) {
            return argValues.getInteger("minimum") <= order.getTotal();
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            ConfigArgDefinition argDef = new ConfigArgDefinition();
            argDef.setType("int");
            return ImmutableMap.of("minimum", argDef);
        }
    };

    PromotionItemAction fixedPriceItemAction = new PromotionItemAction("fixed_price_item_action", "") {
        @Override
        public float execute(OrderItemEntity item, OrderLineEntity line, ConfigArgValues argValues) {
            return - item.getUnitPrice() + 42;
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            return ImmutableMap.of();
        }
    };

    PromotionOrderAction fixedPriceOrderAction =
            new PromotionOrderAction("fixed_price_order_action", "") {
        @Override
        public float execute(OrderEntity order, ConfigArgValues argValues) {
            return -order.getTotal() + 42;
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            return ImmutableMap.of();
        }
    };

    PromotionItemAction percentageItemAction = new PromotionItemAction("percentage_item_action", "") {
        @Override
        public float execute(OrderItemEntity orderItem, OrderLineEntity orderLine, ConfigArgValues argValues) {
            return -orderLine.getUnitPrice() * (argValues.getInteger("discount") / 100.0F);
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            ConfigArgDefinition argDef = new ConfigArgDefinition();
            argDef.setType("int");
            return ImmutableMap.of("discount", argDef);
        }
    };

    PromotionOrderAction percentageOrderAction = new PromotionOrderAction("percentage_order_action", "") {
        @Override
        public float execute(OrderEntity order, ConfigArgValues argValues) {
            return -order.getSubTotal() * (argValues.getInteger("discount") / 100.0F);
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            ConfigArgDefinition argDef = new ConfigArgDefinition();
            argDef.setType("int");
            return ImmutableMap.of("discount", argDef);
        }
    };

    @Test
    @Order(1)
    public void single_line_with_single_applicable_promotion() {
        PromotionEntity promotion =
                new PromotionEntity(Arrays.asList(alwaysTrueCondition), Arrays.asList(fixedPriceOrderAction));
        promotion.setId(1L);
        ConfigurableOperation conditionOp = new ConfigurableOperation();
        conditionOp.setCode(alwaysTrueCondition.getCode());
        promotion.getConditions().add(conditionOp);
        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(fixedPriceOrderAction.getCode());
        promotion.getActions().add(actionOp);

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(123);
        testLineItem.setQuantity(1);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(promotion));

        assertThat(order.getSubTotal()).isEqualTo(123);
        assertThat(order.getTotal()).isEqualTo(42);
    }

    @Test
    @Order(2)
    public void condition_based_on_order_total() {
        PromotionEntity promotion =
                new PromotionEntity(Arrays.asList(orderTotalCondition), Arrays.asList(fixedPriceOrderAction));
        promotion.setId(1L);
        promotion.setName("Test Promotion 1");
        ConfigurableOperation conditionOp = new ConfigurableOperation();
        conditionOp.setCode(orderTotalCondition.getCode());
        ConfigArg arg = new ConfigArg();
        arg.setName("minimum");
        arg.setValue("100");
        conditionOp.getArgs().add(arg);
        promotion.getConditions().add(conditionOp);
        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(fixedPriceOrderAction.getCode());
        promotion.getActions().add(actionOp);

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(50);
        testLineItem.setQuantity(1);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(promotion));

        assertThat(order.getSubTotal()).isEqualTo(50);
        assertThat(order.getAdjustments()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(50);

        /**
         * increase the quantity to 2, which will take the total over the minimum set by the condition
         */
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setUnitPrice(50);
        order.getLines().get(0).getItems().add(orderItem);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(promotion));

        assertThat(order.getSubTotal()).isEqualTo(100);
        /**
         * Now the fixedPriceOrderAction should be in effect
         */
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(42);
    }

    @Test
    @Order(3)
    public void percentage_order_discount() {
        PromotionEntity promotion =
                new PromotionEntity(Arrays.asList(alwaysTrueCondition), Arrays.asList(percentageOrderAction));
        promotion.setId(1L);
        promotion.setName("50% off order");
        ConfigurableOperation conditionOp = new ConfigurableOperation();
        conditionOp.setCode(alwaysTrueCondition.getCode());
        promotion.getConditions().add(conditionOp);
        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageOrderAction.getCode());
        ConfigArg arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("50");
        actionOp.getArgs().add(arg);
        promotion.getActions().add(actionOp);

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(100);
        testLineItem.setQuantity(1);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(promotion));

        assertThat(order.getSubTotal()).isEqualTo(100);
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo("50% off order");
        assertThat(order.getTotal()).isEqualTo(50);
    }

    @Test
    @Order(4)
    public void percentage_items_discount() {
        PromotionEntity promotion =
                new PromotionEntity(Arrays.asList(alwaysTrueCondition), Arrays.asList(percentageItemAction));
        promotion.setId(1L);
        promotion.setName("50% off each item");
        ConfigurableOperation conditionOp = new ConfigurableOperation();
        conditionOp.setCode(alwaysTrueCondition.getCode());
        promotion.getConditions().add(conditionOp);

        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageItemAction.getCode());
        ConfigArg arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("50");
        actionOp.getArgs().add(arg);
        promotion.getActions().add(actionOp);

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(100);
        testLineItem.setQuantity(1);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(promotion));

        assertThat(order.getSubTotal()).isEqualTo(50);
        assertThat(order.getLines().get(0).getAdjustments()).hasSize(1);
        assertThat(order.getLines().get(0).getAdjustments().get(0).getDescription()).isEqualTo("50% off each item");
        assertThat(order.getTotal()).isEqualTo(50);
    }

    /**
     * interaction amongst promotion actions
     */
    PromotionCondition orderQuanityCondition = new PromotionCondition(
            "order_quantity_condition",
            "Passes if any order line has at least the minimum quantity") {
        @Override
        public boolean check(OrderEntity order, ConfigArgValues argValues) {
            for(OrderLineEntity line : order.getLines()) {
                if (argValues.getInteger("minimum") <= line.getQuantity()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Map<String, ConfigArgDefinition> getArgSpec() {
            ConfigArgDefinition argDef = new ConfigArgDefinition();
            argDef.setType("int");
            return ImmutableMap.of("minimum", argDef);
        }
    };

    PromotionEntity buy3Get10pcOffOrder;
    PromotionEntity spend100Get10pcOffOrder;

    private void before_test_5() {
        buy3Get10pcOffOrder =
                new PromotionEntity(Arrays.asList(orderQuanityCondition), Arrays.asList(percentageOrderAction));
        buy3Get10pcOffOrder.setId(1L);
        buy3Get10pcOffOrder.setName("Buy 3 Get 50% off order");
        ConfigurableOperation conditionOp = new ConfigurableOperation();
        conditionOp.setCode(orderQuanityCondition.getCode());
        ConfigArg arg = new ConfigArg();
        arg.setName("minimum");
        arg.setValue("3");
        conditionOp.getArgs().add(arg);
        buy3Get10pcOffOrder.getConditions().add(conditionOp);

        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageOrderAction.getCode());
        arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("50");
        actionOp.getArgs().add(arg);
        buy3Get10pcOffOrder.getActions().add(actionOp);

        spend100Get10pcOffOrder =
                new PromotionEntity(Arrays.asList(orderTotalCondition), Arrays.asList(percentageOrderAction));
        spend100Get10pcOffOrder.setId(1L);
        spend100Get10pcOffOrder.setName("Spend $100 Get 10% off order");
        conditionOp = new ConfigurableOperation();
        conditionOp.setCode(orderTotalCondition.getCode());
        arg = new ConfigArg();
        arg.setName("minimum");
        arg.setValue("100");
        conditionOp.getArgs().add(arg);
        spend100Get10pcOffOrder.getConditions().add(conditionOp);

        actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageOrderAction.getCode());
        arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("10");
        actionOp.getArgs().add(arg);
        spend100Get10pcOffOrder.getActions().add(actionOp);
    }

    @Test
    @Order(5)
    public void two_order_level_percentage_discounts() {
        before_test_5();

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(50);
        testLineItem.setQuantity(2);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        /**
         * initially the order is $100, so the second promotion applies
         */
        orderCalculator.applyPriceAdjustments(order, Arrays.asList(buy3Get10pcOffOrder, spend100Get10pcOffOrder));

        assertThat(order.getSubTotal()).isEqualTo(100);
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo(spend100Get10pcOffOrder.getName());
        assertThat(order.getTotal()).isEqualTo(90);

        /**
         * increase the quantity to 3, which will trigger the first promotion and thus
         * bring the order total below the threshold for the second promotion.
         */
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setUnitPrice(50);
        order.getLines().get(0).getItems().add(orderItem);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(buy3Get10pcOffOrder, spend100Get10pcOffOrder));

        assertThat(order.getSubTotal()).isEqualTo(150);
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(75);
    }

    PromotionEntity orderPromo;
    PromotionEntity itemPromo;

    private void before_test_6() {
        orderPromo =
                new PromotionEntity(Arrays.asList(), Arrays.asList(percentageOrderAction));
        orderPromo.setId(3L);
        orderPromo.setName("10% off order");
        orderPromo.setCouponCode("ORDER10");

        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageOrderAction.getCode());
        ConfigArg arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("10");
        actionOp.getArgs().add(arg);
        orderPromo.getActions().add(actionOp);

        itemPromo =
                new PromotionEntity(Arrays.asList(), Arrays.asList(percentageItemAction));
        itemPromo.setId(4L);
        itemPromo.setName("10% off item");
        itemPromo.setCouponCode("ITEM10");

        actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageItemAction.getCode());
        arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("10");
        actionOp.getArgs().add(arg);
        itemPromo.getActions().add(actionOp);
    }

    @Test
    @Order(6)
    private void item_level_and_order_level_percentage_discounts() {
        before_test_6();

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(155880);
        testLineItem.setQuantity(1);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        assertThat(order.getTotal()).isEqualTo(155880);

        // Apply the item-level discount
        order.getCouponCodes().add("ITEM10");
        orderCalculator.applyPriceAdjustments(order, Arrays.asList(orderPromo, itemPromo));
        assertThat(order.getTotal()).isEqualTo(126263);

        // Apply the order-level discount
        order.getCouponCodes().add("ORDER10");
        orderCalculator.applyPriceAdjustments(order, Arrays.asList(orderPromo, itemPromo));
        assertThat(order.getTotal()).isEqualTo(126263);
    }

    PromotionEntity hasEmptyStringCouponCode;

    private void before_test_7() {
        hasEmptyStringCouponCode =
                new PromotionEntity(Arrays.asList(orderTotalCondition), Arrays.asList(percentageOrderAction));
        hasEmptyStringCouponCode.setId(5L);
        hasEmptyStringCouponCode.setName("Has empty string couponCode");
        hasEmptyStringCouponCode.setCouponCode("");
        ConfigurableOperation conditionOp = new ConfigurableOperation();
        conditionOp.setCode(orderTotalCondition.getCode());
        ConfigArg arg = new ConfigArg();
        arg.setName("minimum");
        arg.setValue("10");
        conditionOp.getArgs().add(arg);
        hasEmptyStringCouponCode.getConditions().add(conditionOp);

        ConfigurableOperation actionOp = new ConfigurableOperation();
        actionOp.setCode(percentageOrderAction.getCode());
        arg = new ConfigArg();
        arg.setName("discount");
        arg.setValue("10");
        actionOp.getArgs().add(arg);
        hasEmptyStringCouponCode.getActions().add(actionOp);
    }

    @Test
    @Order(7)
    public void empty_string_couponCode_does_not_prevent_promotion_being_applied() {
        before_test_7();

        TestLineItem testLineItem = new TestLineItem();
        testLineItem.setUnitPrice(100);
        testLineItem.setQuantity(1);
        OrderConfig orderConfig = new OrderConfig();
        orderConfig.getLines().add(testLineItem);
        OrderEntity order = createOrder(orderConfig);

        orderCalculator.applyPriceAdjustments(order, Arrays.asList(hasEmptyStringCouponCode));
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo(hasEmptyStringCouponCode.getName());
    }


    @Data
    static class OrderConfig {
        private List<TestLineItem> lines = new ArrayList<>();
    }

    @Data
    static class TestLineItem {
        private Integer unitPrice;
        private Integer quantity;
    }
}
