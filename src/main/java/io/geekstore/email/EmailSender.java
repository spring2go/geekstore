/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.email;

/**
 * Created on Nov, 2020 by @author bobo
 */
public interface EmailSender {
    void send(EmailDetails emailDetails) throws Exception;
}
