/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when a user attempts to access a resource which is outsite of
 * his or her privileges.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class ForbiddenException extends AbstractGraphqlException {
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
