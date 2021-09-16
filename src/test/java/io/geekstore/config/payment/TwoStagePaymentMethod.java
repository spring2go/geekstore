/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.payment;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.payment_method.CreatePaymentResult;
import io.geekstore.config.payment_method.PaymentMethodHandler;
import io.geekstore.config.payment_method.SettlePaymentResult;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.PaymentEntity;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.types.common.ConfigArgDefinition;
import com.google.common.collect.ImmutableMap;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * A two-stage (authorize, capture) payment method, with no createRefund method.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class TwoStagePaymentMethod extends PaymentMethodHandler {
    public TwoStagePaymentMethod() {
        super("authorize-only-payment-method", "Test Payment Method");
    }

    @Override
    public CreatePaymentResult createPayment(
            OrderEntity orderEntity, ConfigArgValues argValues, Map<String, String> metadata) {
        CreatePaymentResult result = new CreatePaymentResult();
        result.setAmount(orderEntity.getTotal());
        result.setState(PaymentState.Authorized);
        result.setTransactionId("12345");
        if (!CollectionUtils.isEmpty(metadata)) {
            result.setMetadata(metadata);
        }
        return result;
    }

    @Override
    public SettlePaymentResult settlePayment(
            OrderEntity orderEntity, PaymentEntity paymentEntity, ConfigArgValues argValues) {
        SettlePaymentResult result = new SettlePaymentResult();
        result.setSuccess(true);
        result.getMetadata().put("moreData", "42");
        return result;
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return ImmutableMap.of();
    }
}
