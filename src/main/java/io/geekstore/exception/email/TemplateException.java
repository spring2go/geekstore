/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception.email;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class TemplateException extends Exception {
    public TemplateException() {
        super();
    }

    public TemplateException(final String message) {
        super(message);
    }

    public TemplateException(final Throwable cause) {
        super(cause);
    }

    public TemplateException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
