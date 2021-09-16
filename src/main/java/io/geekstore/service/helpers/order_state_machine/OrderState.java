/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.order_state_machine;

/**
 * These are the default states of the Order process.
 *
 * Created on Dec, 2020 by @author bobo
 */
public enum OrderState {
    AddingItems,
    ArrangingPayment,
    PaymentAuthorized,
    PaymentSettled,
    PartiallyFulfilled,
    Fulfilled,
    Cancelled
}
