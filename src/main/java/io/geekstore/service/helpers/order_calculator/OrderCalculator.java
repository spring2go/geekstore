/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.order_calculator;

import io.geekstore.entity.*;
import io.geekstore.mapper.ShippingMethodEntityMapper;
import io.geekstore.service.helpers.shipping_calculator.EligibleShippingMethod;
import io.geekstore.service.helpers.shipping_calculator.ShippingCalculator;
import io.geekstore.types.common.Adjustment;
import io.geekstore.types.common.AdjustmentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class OrderCalculator {

    private final ShippingCalculator shippingCalculator;
    private final ShippingMethodEntityMapper shippingMethodEntityMapper;

    public List<OrderItemEntity> applyPriceAdjustments(
            OrderEntity order, List<PromotionEntity> promotions) {
        return this.applyPriceAdjustments(order, promotions, null);
    }

    /**
     * Applies promotions to an Order. Mutates the order object.
     * Returns a list of OrderItems which had new promotion adjustments applied.
     */
    public List<OrderItemEntity> applyPriceAdjustments(
            OrderEntity order, List<PromotionEntity> promotions, OrderLineEntity updatedOrderLine) {
        Set<OrderItemEntity> updatedOrderItems = new HashSet<>();
        if (updatedOrderLine != null) {
            updatedOrderLine.getActiveItems().forEach(item -> updatedOrderItems.add(item));
        }
        order.clearAdjustments(null);
        this.calculatoOrderTotal(order);
        if (!CollectionUtils.isEmpty(order.getLines())) {
            // Test and apply promotions
            List<OrderItemEntity> itemsModifiedByPromotions = this.applyPromotions(order, promotions);
            itemsModifiedByPromotions.forEach(item -> updatedOrderItems.add(item));

            this.applyShipping(order);
        }
        this.calculatoOrderTotal(order);
        return new ArrayList<>(updatedOrderItems);
    }

    /**
     * Applies any eligible promotions to each OrderItem in the order. Returns a list of
     * any OrderItems which had their Adjustments modified
     */
    private List<OrderItemEntity> applyPromotions(OrderEntity order, List<PromotionEntity> promotions) {
        List<OrderItemEntity> updatedItems = this.applyOrderItemPromotions(order, promotions);
        this.applyOrderPromotions(order, promotions);
        return updatedItems;
    }

    /**
     * Applies promotions to OrderItems. This is quite complex function, due to the inherent complexity
     * of applying the promotions, and also due to added complexity in the name of performance optimization.
     * Therefore it is heavily annotated so that the purpose of each step is clear.
     */
    private List<OrderItemEntity> applyOrderItemPromotions(OrderEntity order, List<PromotionEntity> promotions) {
        /**
         * The naive implementation updates *every* OrderItem after this function is run.
         * However, on a very large order with hundreds or thousands of OrderItems, this results in
         * very poor performance. E.g. updating a single quantity of an OrderLine results in saving
         * all 1000 (for example) OrderItems to the DB.
         * The solution is to try to be smart about tracking exactly which OrderItems have changed,
         * so that we only update those.
         */
        Set<OrderItemEntity> updatedOrderItems = new HashSet<>();

        for(OrderLineEntity line : order.getLines()) {
            /**
             * Must be re-calculated for each line, since the previous lines may have triggered promotions
             * which affected the order price.
             */
            List<PromotionEntity> applicablePromotions = promotions.stream()
                    .filter(p -> p.test(order)).collect(Collectors.toList());

            boolean lineHasExistingPromotions = line.getItems().get(0).getPendingAdjustments().stream()
                                    .anyMatch(a -> a.getType() == AdjustmentType.PROMOTION);
            boolean forceUpdateItems = this.orderLineHasInapplicablePromotions(applicablePromotions, line);

            if (forceUpdateItems || lineHasExistingPromotions) {
                line.clearAdjustments(AdjustmentType.PROMOTION);
            }
            if (forceUpdateItems) {
                /**
                 * This OrderLine contains Promotion adjustments for Promotion that are no longer
                 * applicable. So we know for sure we will need to update these OrderItems in the DB.
                 * Therefore add them to the `updatedOrderItems` set.
                 */
                line.getItems().forEach(i -> updatedOrderItems.add(i));
            }

            for(PromotionEntity promotion : applicablePromotions) {
                boolean priceAdjusted = false;
                /**
                 * We need to test the promotion *again*, even though we've test them for the line.
                 * This is because the previous Promotions may have adjusted the Order in such a way
                 * as to render later promotions no longer applicable.
                 */
                if (promotion.test(order)) {
                    for(OrderItemEntity item : line.getItems()) {
                        Adjustment adjustment = promotion.apply(item, line);
                        if (adjustment != null) {
                            item.getPendingAdjustments().add(adjustment);
                            priceAdjusted = true;
                            updatedOrderItems.add(item);
                        }
                    }
                    if (priceAdjusted) {
                        this.calculatoOrderTotal(order);
                    }
                }
            }
            boolean lineNoLongerHasPromotions = !line.getItems().get(0).getPendingAdjustments().stream()
                                    .anyMatch(a -> a.getType() == AdjustmentType.PROMOTION);
            if (lineHasExistingPromotions && lineNoLongerHasPromotions) {
                line.getItems().forEach(i -> updatedOrderItems.add(i));
            }

            if (forceUpdateItems) {
                /**
                 * If we are forcing an update, we need to ensure that totals get
                 * re-calculated *even if* there are no applicable promotions (i.e.
                 * the other call to `this.calculateOrderTotals()` inside the `for...:`
                 * loop was never invoked).
                 */
                this.calculatoOrderTotal(order);
            }
        }
        return new ArrayList<>(updatedOrderItems);
    }

    /**
     * An OrderLine may have promotion adjustments from Promotions which are no longer applicable.
     * For example, a coupon code might have caused a discount to be applied, and now that code has been removed from
     * the order. The adjustment will still be there on each OrderItem it was applied to, even though that Promotion
     * is no longer found in `applicablePromotions` list.
     *
     * We need to know about this because it means that all OrderItems in the OrderLine must be updated.
     */
    private boolean orderLineHasInapplicablePromotions(
            List<PromotionEntity> applicablePromotions, OrderLineEntity line) {
        List<String> applicablePromotionIds = applicablePromotions.stream()
                .map(PromotionEntity::getSourceId).collect(Collectors.toList());
        List<String> linePromotionIds = line.getAdjustments().stream()
                .filter(a -> a.getType() == AdjustmentType.PROMOTION)
                .map(Adjustment::getAdjustmentSource).collect(Collectors.toList());
        boolean hasPromotionsThatAreNoLongerApplicable = !linePromotionIds.stream()
                .allMatch(id -> applicablePromotionIds.contains(id));
        return hasPromotionsThatAreNoLongerApplicable;
    }

    private void applyOrderPromotions(OrderEntity order, List<PromotionEntity> promotions) {
        order.clearAdjustments(AdjustmentType.PROMOTION);
        List<PromotionEntity> applicableOrderPromotions = promotions.stream()
                .filter(p -> p.test(order)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(applicableOrderPromotions)) {
            for (PromotionEntity promotion : applicableOrderPromotions) {
                // re-test the promotion on each iteration, since the order total
                // may be modified by a previously-applied promotion
                if (promotion.test(order)) {
                    Adjustment adjustment = promotion.apply(order);
                    if (adjustment != null) {
                        order.getPendingAdjustments().add(adjustment);
                    }
                }
            }
            this.calculatoOrderTotal(order);
        }
    }

    private void applyShipping(OrderEntity order) {
        List<EligibleShippingMethod> results = this.shippingCalculator.getEligibleShippingMethods(order);
        ShippingMethodEntity currentShippingMethod =
                shippingMethodEntityMapper.selectById(order.getShippingMethodId());
        if (!CollectionUtils.isEmpty(results) && currentShippingMethod != null) {
            EligibleShippingMethod selected = results.stream()
                    .filter(r -> Objects.equals(r.getMethod().getId(), currentShippingMethod.getId()))
                    .findFirst().orElse(null);
            if (selected == null) {
                selected = results.get(0);
            }
            order.setShipping(selected.getResult().getPrice());
        }
    }

    private void calculatoOrderTotal(OrderEntity order) {
        int totalPrice = 0;

        for(OrderLineEntity line : order.getLines()) {
            totalPrice += line.getTotalPrice();
        }
        order.setSubTotal(totalPrice);
    }
}
