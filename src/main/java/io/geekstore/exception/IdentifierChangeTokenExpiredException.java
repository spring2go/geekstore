/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class IdentifierChangeTokenExpiredException extends AbstractGraphqlException {
    public IdentifierChangeTokenExpiredException() {
        super(ErrorCode.EXPIRED_IDENTIFIER_CHANGE_TOKEN);
    }
}
