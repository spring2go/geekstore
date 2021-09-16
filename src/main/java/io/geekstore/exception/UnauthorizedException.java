/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when the user's authentication credentials do not match.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class UnauthorizedException extends AbstractGraphqlException {
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
