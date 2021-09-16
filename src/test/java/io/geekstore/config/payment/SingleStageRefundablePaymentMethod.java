/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.payment;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.payment_method.CreatePaymentResult;
import io.geekstore.config.payment_method.CreateRefundResult;
import io.geekstore.config.payment_method.PaymentMethodHandler;
import io.geekstore.config.payment_method.SettlePaymentResult;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.PaymentEntity;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.service.helpers.refund_state_machine.RefundState;
import io.geekstore.types.common.ConfigArgDefinition;
import io.geekstore.types.order.RefundOrderInput;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A payment method which includes a createRefund method.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class SingleStageRefundablePaymentMethod extends PaymentMethodHandler {
    public SingleStageRefundablePaymentMethod() {
        super("single-stage-refundable-payment-method", "Test Payment Method");
    }

    @Override
    public CreatePaymentResult createPayment(
            OrderEntity orderEntity, ConfigArgValues argValues, Map<String, String> metadata) {
        CreatePaymentResult result = new CreatePaymentResult();
        result.setAmount(orderEntity.getTotal());
        result.setState(PaymentState.Settled);
        result.setTransactionId("12345");
        result.setMetadata(metadata);
        return result;
    }

    @Override
    public SettlePaymentResult settlePayment(
            OrderEntity orderEntity, PaymentEntity paymentEntity, ConfigArgValues argValues) {
        SettlePaymentResult result = new SettlePaymentResult();
        result.setSuccess(true);
        return result;
    }

    @Override
    public CreateRefundResult createRefund(
            RefundOrderInput input,
            Integer total,
            OrderEntity orderEntity,
            PaymentEntity paymentEntity,
            ConfigArgValues argValues) {
        CreateRefundResult result = new CreateRefundResult();
        result.setAmount(total);
        result.setState(RefundState.Settled);
        result.setTransactionId("abc123");
        return result;
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return ImmutableMap.of();
    }
}
