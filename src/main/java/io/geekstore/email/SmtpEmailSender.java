/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.email;

import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender javaMailSender;

    public SmtpEmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(EmailDetails emailDetails) throws Exception {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        mimeMessage.setSentDate(new Date());
        mimeMessage.setFrom(emailDetails.getFrom());
        mimeMessage.addRecipients(Message.RecipientType.TO, emailDetails.getRecipient());
        mimeMessage.setSubject(emailDetails.getSubject());
        mimeMessage.setText(emailDetails.getBody());
        mimeMessage.saveChanges();

        javaMailSender.send(mimeMessage);
    }
}
