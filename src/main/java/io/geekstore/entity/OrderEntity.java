/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.custom.mybatis_plus.AdjustmentListTypeHandler;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.types.common.Adjustment;
import io.geekstore.types.common.AdjustmentType;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.order.OrderAddress;
import io.geekstore.types.product.ProductVariant;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An Order is created whenever a {@link Customer} adds an item to the cart. It contains all the
 * information required to fulfill an order: which {@link ProductVariant}s
 * in what quantities; the shipping_method address and price; any applicable promotions; payments etc.
 *
 * An Order exists in a well-defined state according to the
 * {@link OrderState} type.
 *
 * A state machine is used to govern the transition from one state to another.
 *
 * Created on Dec, 2020 by @author bobo
 */
@TableName(value = "tb_order", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderEntity extends BaseEntity {
    private String code;

    private OrderState state;

    private boolean active = true;

    private Date orderPlacedAt;

    private Long customerId;

    @TableField(exist = false)
    private List<OrderLineEntity> lines = new ArrayList<>();

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> couponCodes = new ArrayList<>();

    @TableField(typeHandler = AdjustmentListTypeHandler.class)
    private List<Adjustment> pendingAdjustments = new ArrayList<>();

    @TableField(typeHandler = JacksonTypeHandler.class)
    private OrderAddress shippingAddress;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private OrderAddress billingAddress;
    /**
     * The subTotal is the total of the OrderLines, before order-level promotions
     * and shipping_method has been applied.
     */
    private Integer subTotal = 0;

    private Long shippingMethodId;

    private Integer shipping = 0;

    public Integer getTotal() {
        return this.subTotal + this.getPromotionAdjustmentsTotal() + (this.shipping == null ? 0 : this.shipping);
    }

    public List<Adjustment> getAdjustments() {
        return this.pendingAdjustments;
    }

    public Integer getTotalQuantity() {
        return this.lines.stream().reduce(0, (total, line) -> total + line.getQuantity(), Integer::sum);
    }

    public Integer getPromotionAdjustmentsTotal() {
        return this.getAdjustments().stream()
                .filter(adjustment -> adjustment.getType() == AdjustmentType.PROMOTION)
                .reduce(0, (total, a) -> total + a.getAmount(), Integer::sum);
    }

    /**
     * Clears Adjustments of the given type. If no typoe
     * is specified, then all adjustments are removed.
     */
    public void clearAdjustments(AdjustmentType type) {
        if (type == null) {
            this.pendingAdjustments = new ArrayList<>();
        } else {
            this.pendingAdjustments = this.pendingAdjustments.stream()
                    .filter(adjustment -> adjustment.getType() != type)
                    .collect(Collectors.toList());
        }
    }

    public List<OrderItemEntity> getOrderItems() {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        this.lines.forEach(line -> orderItemEntities.addAll(line.getItems()));
        return orderItemEntities;
    }
}
