/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.*;
import io.geekstore.custom.mybatis_plus.ConfigurableOperationListTypeHandler;
import io.geekstore.service.helpers.SpringContext;
import io.geekstore.types.common.Adjustment;
import io.geekstore.types.common.AdjustmentType;
import io.geekstore.types.common.ConfigurableOperation;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * A Promotion is used to define a set of conditions under which promotions actions (typically discounts)
 * will be applied to an Order.
 *
 * Each assinged {@link PromotionCondition} is checked against the Order, and if they all return `true`,
 * then each assgin @{link PromotionItemAction} / {@link PromodtionOrderAction} is applied to the Order.
 *
 * Created on Dec, 2020 by @author bobo
 */
@TableName(value = "tb_promotion", autoResultMap = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PromotionEntity extends AdjustmentSource {

    public PromotionEntity() {
        this.setType(AdjustmentType.PROMOTION);

        PromotionOptions promotionOptions = SpringContext.getBean(PromotionOptions.class);
        List<PromotionCondition> promotionConditions = promotionOptions.getPromotionConditions();
        List<PromotionAction> promotionActions = promotionOptions.getPromotionActions();
        this.allConditions = promotionConditions == null ? new HashMap<>() :
                promotionConditions.stream().collect(toMap(PromotionCondition::getCode, condition -> condition));
        this.allActions = promotionActions == null ? new HashMap<>() :
                promotionActions.stream().collect(toMap(PromotionAction::getCode, action -> action));
    }

    public PromotionEntity(List<PromotionCondition> promotionConditions, List<PromotionAction> promotionActions) {
        this.setType(AdjustmentType.PROMOTION);

        PromotionOptions promotionOptions = SpringContext.getBean(PromotionOptions.class);
        if (promotionConditions == null) {
            promotionConditions = promotionOptions.getPromotionConditions();
        }
        if (promotionActions == null) {
            promotionActions = promotionOptions.getPromotionActions();
        }

        this.allConditions = promotionConditions == null ? new HashMap<>() :
                promotionConditions.stream().collect(toMap(PromotionCondition::getCode, condition -> condition));
        this.allActions = promotionActions == null ? new HashMap<>() :
                promotionActions.stream().collect(toMap(PromotionAction::getCode, action -> action));
    }

    @TableField(exist = false)
    private final Map<String, PromotionAction> allActions;
    @TableField(exist = false)
    private final Map<String, PromotionCondition> allConditions;

    private Date deletedAt; // SoftDelete
    private Date startsAt;
    private Date endsAt;
    private String couponCode;
    private Integer perCustomerUsageLimit;
    private String name;
    private boolean enabled;
    @TableField(typeHandler = ConfigurableOperationListTypeHandler.class)
    private List<ConfigurableOperation> conditions = new ArrayList<>();
    @TableField(typeHandler = ConfigurableOperationListTypeHandler.class)
    private List<ConfigurableOperation> actions = new ArrayList<>();

    /**
     * The PriorityScore is used to determine the sequence in which multiple promotions are tested
     * on a given order. A higher number moves the Promotion towards the end of the sequence.
     *
     * The score is derived from the sum of the priorityValues of the PromotionConditions and PromotionActions
     * comprising this Promotion.
     *
     * An example illustrating the need for a priority is this:
     *
     * Consider 2 Promotions, 1) buy 1 get one free and 2) 10% off when order total is over $50.
     * If Promotion 2 is evaluated prior to Promotion 1, then it can trigger the 10% discount even
     * if the subsequent application of Promotion 1 brings the order total down to way below $50.
     */
    private Integer priorityScore;

    @Override
    public boolean test(Object... args) {
        if (this.endsAt != null && this.endsAt.getTime() < new Date().getTime()) {
            return false;
        }
        if (this.startsAt != null && this.startsAt.getTime() > new Date().getTime()) {
            return false;
        }
        OrderEntity orderEntity = (OrderEntity) args[0];
        if (!StringUtils.isEmpty(this.couponCode) && !orderEntity.getCouponCodes().contains(this.couponCode)) {
            return false;
        }
        for(ConfigurableOperation condition : this.conditions) {
            PromotionCondition promotionCondition = this.allConditions.get(condition.getCode());
            if (promotionCondition != null &&
                    !promotionCondition.check(orderEntity, new ConfigArgValues(condition.getArgs()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Adjustment apply(Object... args) {
        int amount = 0;

        for(ConfigurableOperation action : this.actions) {
            PromotionAction promotionAction = this.allActions.get(action.getCode());
            if (promotionAction instanceof PromotionItemAction) {
                if (args[0] instanceof OrderItemEntity) {
                    PromotionItemAction promotionItemAction = (PromotionItemAction) promotionAction;
                    OrderItemEntity orderItemEntity = (OrderItemEntity) args[0];
                    OrderLineEntity orderLineEntity = (OrderLineEntity) args[1];
                    amount += Math.round(
                            promotionItemAction.execute(
                                    orderItemEntity,
                                    orderLineEntity,
                                    new ConfigArgValues(action.getArgs())
                            )
                    );
                }
            } else {
                if (args[0] instanceof OrderEntity) {
                    PromotionOrderAction promotionOrderAction = (PromotionOrderAction) promotionAction;
                    OrderEntity orderEntity = (OrderEntity) args[0];
                    amount += Math.round(
                            promotionOrderAction.execute(
                                    orderEntity,
                                    new ConfigArgValues(action.getArgs())
                            )
                    );
                }
            }
        }

        if (amount != 0) {
            Adjustment adjustment = new Adjustment();
            adjustment.setAmount(amount);
            adjustment.setType(this.getType());
            adjustment.setDescription(this.getName());
            adjustment.setAdjustmentSource(this.getSourceId());
            return adjustment;
        }
        return null;
    }
}
