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
public class CouponCodeLimitException extends AbstractGraphqlException {
    public CouponCodeLimitException(int limit) {
        super(String.format("Coupon code cannot be used more than %d time(s) per customer", limit),
                ErrorCode.COUPON_CODE_EXPIRED);
    }
}
