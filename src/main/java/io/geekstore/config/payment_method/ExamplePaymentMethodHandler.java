/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.payment_method;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.PaymentEntity;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.service.helpers.payment_state_machine.PaymentTransitionData;
import io.geekstore.types.common.ConfigArgDefinition;
import io.geekstore.types.order.RefundOrderInput;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An example of a payment method which sets up and authorizes the payment on the client side and then
 * requires a further step on the server side to charge the card.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class ExamplePaymentMethodHandler extends PaymentMethodHandler {

    private final Map<String, ConfigArgDefinition> argSpec;

    public ExamplePaymentMethodHandler() {
        super(
                "example-payment-provider",
                "Example Payment Provider"
        );

        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();
        argDef.setType("boolean");
        argDefMap.put("automaticCapture", argDef);

        argDef = new ConfigArgDefinition();
        argDef.setType("string");
        argDefMap.put("apiKey", argDef);

        argSpec = argDefMap;
    }


    @Override
    public CreatePaymentResult createPayment(
            OrderEntity orderEntity, ConfigArgValues argValues, Map<String, String> metadata) {
        CreatePaymentResult createPaymentResult = new CreatePaymentResult();
        try {
            CompletableFuture<String> result = GripeSDK.create(
                    ImmutableMap.of("apiKey", argValues.getString("apiKey"),
                            "amount", orderEntity.getTotal(),
                            "source", metadata.get("authToken"))
            );

            createPaymentResult.setAmount(orderEntity.getTotal());
            createPaymentResult.setState(
                    argValues.getBoolean("automaticCapture") ? PaymentState.Settled : PaymentState.Authorized
            );
            createPaymentResult.setTransactionId(result.get());
            createPaymentResult.setMetadata(metadata);
            return createPaymentResult;
        } catch (Exception ex) {
            createPaymentResult.setAmount(orderEntity.getTotal());
            createPaymentResult.setState(PaymentState.Declined);
            createPaymentResult.getMetadata().put("errorMessage", ex.getMessage());
        }
        return createPaymentResult;
    }

    @Override
    public SettlePaymentResult settlePayment(
            OrderEntity orderEntity, PaymentEntity paymentEntity, ConfigArgValues argValues) {
        boolean captureResult = GripeSDK.capture(paymentEntity.getTransactionId());

        SettlePaymentResult settlePaymentResult = new SettlePaymentResult();
        settlePaymentResult.setSuccess(captureResult);
        settlePaymentResult.getMetadata().put("captureId", "1234567");
        return settlePaymentResult;
    }

    @Override
    public CreateRefundResult createRefund(
            RefundOrderInput input,
            Integer total,
            OrderEntity orderEntity,
            PaymentEntity paymentEntity,
            ConfigArgValues argValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object onStateTransitionStart(PaymentState fromState, PaymentState toState, PaymentTransitionData data) {
        return true;
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return argSpec;
    }
}
