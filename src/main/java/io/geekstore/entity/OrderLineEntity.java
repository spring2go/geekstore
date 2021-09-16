/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.types.common.Adjustment;
import io.geekstore.types.common.AdjustmentType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A single line on an {@link OrderEntity} which contains one or more {@link OrderItemEntity}s
 *
 * Created on Dec, 2020 by @author bobo
 */
@TableName("tb_order_line")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderLineEntity extends BaseEntity {
    private Long productVariantId;
    private Long featuredAssetId;
    private Long orderId;

    @TableField(exist = false)
    private List<OrderItemEntity> items = new ArrayList<>();

    public Integer getUnitPrice() {
        return CollectionUtils.isEmpty(this.getActiveItems()) ? 0 : this.getActiveItems().get(0).getUnitPrice();
    }

    public Integer getQuantity() {
        return this.getActiveItems().size();
    }

    public Integer getTotalPrice() {
        return this.getActiveItems().stream()
                .reduce(0, (total, item) -> total + item.getUnitPriceWithPromotions(), Integer::sum);
    }

    public List<OrderItemEntity> getActiveItems() {
        return this.items.stream().filter(i -> !i.isCancelled()).collect(Collectors.toList());
    }

    public List<Adjustment> getAdjustments() {
        List<Adjustment> result = new ArrayList<>();
        this.getActiveItems().forEach(item -> result.addAll(item.getAdjustments()));
        return result;
    }

    /**
     * Clears Adjustments from all OrderItems of the given type. If no type
     * is specified, then all adjustments are removed.
     */
    public void clearAdjustments(AdjustmentType type) {
        this.getActiveItems().forEach(item -> item.clearAdjustments(type));
    }
}
