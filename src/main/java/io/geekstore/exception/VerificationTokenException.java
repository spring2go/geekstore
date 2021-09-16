/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when the verification token (used to verify a Customer's email
 * address) is neither invalid or does not match any expected tokens.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class VerificationTokenException extends AbstractGraphqlException {
    public VerificationTokenException() {
        super(ErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    public VerificationTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
