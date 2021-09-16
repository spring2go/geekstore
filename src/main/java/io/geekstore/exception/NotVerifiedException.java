/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when `requireVerification` in {@link io.geekstore.options.AuthOptions}
 * is set to `true` and an unverified user attempts to authenticate.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class NotVerifiedException extends AbstractGraphqlException {
    public NotVerifiedException() {
        super(ErrorCode.NOT_VERIFIED);
    }
}
