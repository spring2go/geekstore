/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.config.shipping_method.ShippingCalculator;
import io.geekstore.config.shipping_method.ShippingEligibilityChecker;
import io.geekstore.exception.UserInputException;
import io.geekstore.service.ConfigService;
import io.geekstore.types.common.ConfigArg;
import io.geekstore.types.common.ConfigurableOperation;
import io.geekstore.types.common.ConfigurableOperationInput;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This helper class provides methods relating to ShippingMethod configurable operations (eligibility checkers
 * and calculators).
 *
 * Created on Dec, 2020 by @author bobo
 */
@Component
public class ShippingConfiguration {
    private final ConfigService configService;
    @Getter
    private List<ShippingEligibilityChecker> shippingEligibilityCheckers;
    @Getter
    private List<ShippingCalculator> shippingCalculators;

    public ShippingConfiguration(ConfigService configService) {
        this.configService = configService;
        this.shippingCalculators = configService.getShippingOptions().getShippingCalculators();
        if (this.shippingCalculators == null) {
            this.shippingCalculators = new ArrayList<>();
        }
        this.shippingEligibilityCheckers = configService.getShippingOptions().getShippingEligibilityCheckers();
        if (this.shippingEligibilityCheckers == null) {
            this.shippingEligibilityCheckers = new ArrayList<>();
        }
    }

    public ConfigurableOperation parseCheckerInput(ConfigurableOperationInput input) {
        // 校验checker存在
        this.getChecker(input.getCode());
        return this.parseOperationArgs(input);
    }

    public ConfigurableOperation parseCalculatorInput(ConfigurableOperationInput input) {
        // 校验calculator存在
        this.getCalculator(input.getCode());
        return this.parseOperationArgs(input);
    }

    /**
     * Converts the input values of the "create" and "update" mutations into the format expected by the
     * ShippingMethod entity.
     */
    private ConfigurableOperation parseOperationArgs(ConfigurableOperationInput input) {
        ConfigurableOperation output = new ConfigurableOperation();
        output.setCode(input.getCode());
        output.setArgs(input.getArguments().stream()
                .map(arg -> {
                    ConfigArg configArg = new ConfigArg();
                    configArg.setName(arg.getName());
                    configArg.setValue(arg.getValue());
                    return configArg;
                }).collect(Collectors.toList()));
        return output;
    }

    private ShippingEligibilityChecker getChecker(String code) {
        ShippingEligibilityChecker match = this.shippingEligibilityCheckers.stream()
                .filter(a -> Objects.equals(code, a.getCode())).findFirst().orElse(null);
        if (match == null) {
            throw new UserInputException("shipping eligibility checker with code '" + code +"' was not found." );
        }
        return match;
    }

    private ShippingCalculator getCalculator(String code) {
        ShippingCalculator match = this.shippingCalculators.stream()
                .filter(a -> Objects.equals(code, a.getCode())).findFirst().orElse(null);
        if (match == null) {
            throw new UserInputException("shipping calculator with code '" + code + "' was not found.");
        }
        return match;
    }
}
