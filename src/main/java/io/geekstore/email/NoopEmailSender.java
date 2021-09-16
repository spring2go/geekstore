/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.email;

import lombok.extern.slf4j.Slf4j;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
public class NoopEmailSender implements EmailSender {
    @Override
    public void send(EmailDetails emailDetails) throws Exception {
        log.debug("To be sent email details - ");
        log.debug(emailDetails.toString());
    }
}
