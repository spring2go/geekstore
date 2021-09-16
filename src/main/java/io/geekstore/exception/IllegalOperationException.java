/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when an operation is attempted which is not allowed.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class IllegalOperationException extends AbstractGraphqlException {
    public IllegalOperationException(String message) {
        super(message, ErrorCode.ILLEGAL_OPERATION);
    }
}
