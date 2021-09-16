/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when an error occurs trying to reset a Customer's password
 * by reason of the token having expired.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class PasswordResetTokenExpiredException extends AbstractGraphqlException {
    public PasswordResetTokenExpiredException() {
        super(ErrorCode.EXPIRED_PASSWORD_RESET_TOKEN);
    }
}
