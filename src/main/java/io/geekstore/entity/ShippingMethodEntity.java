/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.shipping_method.ShippingCalculationResult;
import io.geekstore.config.shipping_method.ShippingCalculator;
import io.geekstore.config.shipping_method.ShippingEligibilityChecker;
import io.geekstore.config.shipping_method.ShippingOptions;
import io.geekstore.service.helpers.SpringContext;
import io.geekstore.types.common.ConfigurableOperation;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * A ShippingMethod is used to apply to a shipping_method price to an {@link OrderEntity}. It is composed of a
 * {@link ShippingEligibilityChecker} and a {@link ShippingCalculator}. For a given Order,
 * the `checker` is used to determine whether this ShippingMethod can be used. If yes, then
 * the ShippingMethod can be applied and the `calculator` is used to determine the price of shipping_method.
 *
 * Created on Dec, 2020 by @author bobo
 */
@TableName(value = "tb_shipping_method", autoResultMap = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ShippingMethodEntity extends BaseEntity {

    public ShippingMethodEntity() {
        ShippingOptions shippingOptions = SpringContext.getBean(ShippingOptions.class);
        List<ShippingEligibilityChecker> checkers = shippingOptions.getShippingEligibilityCheckers();
        List<ShippingCalculator> calculators = shippingOptions.getShippingCalculators();
        this.allCheckers = checkers ==  null ? new HashMap<>() :
                checkers.stream().collect(toMap(ShippingEligibilityChecker::getCode, checker -> checker));
        this.allCalculators = calculators == null ? new HashMap<>() :
                calculators.stream().collect(toMap(ShippingCalculator::getCode, calculator -> calculator));
    }

    @TableField(exist = false)
    private final Map<String, ShippingEligibilityChecker> allCheckers;
    @TableField(exist = false)
    private final Map<String, ShippingCalculator> allCalculators;

    private Date deletedAt;
    private String code;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ConfigurableOperation checker;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ConfigurableOperation calculator;

    public boolean test(OrderEntity orderEntity) {
        ShippingEligibilityChecker checker = this.allCheckers.get(this.checker.getCode());
        if (checker != null) {
            return checker.check(orderEntity, new ConfigArgValues(this.checker.getArgs()));
        } else {
            return false;
        }
    }

    public ShippingCalculationResult apply(OrderEntity orderEntity) {
        ShippingCalculator calculator = this.allCalculators.get(this.calculator.getCode());
        if (calculator != null) {
            return calculator.calculate(orderEntity, new ConfigArgValues(this.calculator.getArgs()));
        }
        return null;
    }
}
