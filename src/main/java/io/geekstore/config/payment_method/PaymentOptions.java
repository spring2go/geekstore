/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.payment_method;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Payment-related options
 *
 * Created on Dec, 2020 by @author bobo
 */
@Getter
@RequiredArgsConstructor
public class PaymentOptions {
    /**
     * A list of {@link PaymentMethodHandler}s with which to process payments.
     */
    private final List<PaymentMethodHandler> paymentMethodHandlers;
}
