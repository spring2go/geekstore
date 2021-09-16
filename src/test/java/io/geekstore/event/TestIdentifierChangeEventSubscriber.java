/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.event;

import io.geekstore.email.EmailDetails;
import io.geekstore.email.EmailSender;
import io.geekstore.eventbus.events.IdentifierChangeEvent;
import io.geekstore.exception.email.SendEmailException;
import io.geekstore.options.ConfigOptions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TestIdentifierChangeEventSubscriber {
    private final EventBus eventBus;
    private final EmailSender emailSender;
    private final ConfigOptions configOptions;

    @PostConstruct
    void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onEvent(IdentifierChangeEvent event) {
        EmailDetails emailDetails = new EmailDetails(
                event,
                event.getUserEntity().getIdentifier(),
                this.configOptions.getEmailOptions().getDefaultFromEmail());

        try {
            emailSender.send(emailDetails);
        } catch (Exception ex) {
            log.error("Exception to send the email", ex);
            throw new SendEmailException("Exception while sending the email.", ex);
        }
    }
}
