/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This exception is thrown when the coupon code is associated with a Promotion that has expired.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class CouponCodeExpiredException extends AbstractGraphqlException {
    public CouponCodeExpiredException(String couponCode) {
        super(String.format("Coupon code \"{ %s }\" has expired", couponCode),
                ErrorCode.COUPON_CODE_EXPIRED);
    }
}
