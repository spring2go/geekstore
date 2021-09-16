/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when an error occurs trying to reset a Customer's password.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class PasswordResetTokenException extends AbstractGraphqlException {
    public PasswordResetTokenException() {
        super(ErrorCode.BAD_PASSWORD_REST_TOKEN);
    }
}
