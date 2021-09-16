/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.eventbus;

import io.geekstore.email.EmailDetails;
import io.geekstore.email.EmailSender;
import io.geekstore.email.PebbleTemplateService;
import io.geekstore.entity.AuthenticationMethodEntity;
import io.geekstore.eventbus.events.IdentifierChangeRequestEvent;
import io.geekstore.exception.InternalServerError;
import io.geekstore.exception.email.SendEmailException;
import io.geekstore.exception.email.TemplateException;
import io.geekstore.options.ConfigOptions;
import io.geekstore.service.UserService;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class IdentifierChangeRequestEventSubscriber {
    final static String IDENTIFICATION_CHANGE_REQUEST_EMAIL_TEMPLATE = "email-address-change";
    final static String IDENTIFICATION_CHANGE_REQUEST_EMAIL_SUBJECT = "Please verify your change of email address";

    private final ConfigOptions configOptions;
    private final PebbleTemplateService pebbleTemplateService;
    private final UserService userService;
    private final EventBus eventBus;
    private final EmailSender emailSender;

    @PostConstruct
    void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onEvent(IdentifierChangeRequestEvent event) {
        log.info("onEvent called event = " + event);

        AuthenticationMethodEntity nativeAuthMethod = null;
        try {
            nativeAuthMethod = userService.getNativeAuthMethodEntityByUserId(event.getUserEntity().getId());
        } catch (InternalServerError ise) {
            log.warn("Error to get native auth method, userId = { " + event.getUserEntity().getId() + " }", ise);
            return;
        }

        EmailDetails emailDetails = new EmailDetails(
                event,
                event.getUserEntity().getIdentifier(),
                this.configOptions.getEmailOptions().getDefaultFromEmail());

        emailDetails.setSubject(IDENTIFICATION_CHANGE_REQUEST_EMAIL_SUBJECT);

        Map<String, Object> model = ImmutableMap.of(
                "identifierChangeToken", nativeAuthMethod.getIdentifierChangeToken(),
                "changeEmailAddressUrl", configOptions.getEmailOptions().getChangeEmailAddressUrl());
        try {
            String body = this.pebbleTemplateService
                    .mergeTemplateIntoString(IDENTIFICATION_CHANGE_REQUEST_EMAIL_TEMPLATE, model);
            emailDetails.setBody(body);
        } catch (TemplateException te) {
            log.error("The template file cannot be processed", te);
            throw new SendEmailException("Error while processing the template file with the given model object.", te);
        }
        emailDetails.getModel().putAll(model);// 仅方便测试用
        emailDetails.getModel().put("pendingIdentifier", nativeAuthMethod.getPendingIdentifier()); // 测试用

        try {
            emailSender.send(emailDetails);
        } catch (Exception ex) {
            log.error("Exception to send the email", ex);
            throw new SendEmailException("Exception while sending the email.", ex);
        }
    }
}
