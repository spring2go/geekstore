/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception.email;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class SendEmailException extends RuntimeException {
    public SendEmailException() {
        super();
    }

    public SendEmailException(final String message) {
        super(message);
    }

    public SendEmailException(final Throwable cause) {
        super(cause);
    }

    public SendEmailException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
