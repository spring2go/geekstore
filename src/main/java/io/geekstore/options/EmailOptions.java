/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import lombok.Data;

/**
 * Defines how transactional emails (account verification, order confirmation etc) are generated and sent.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class EmailOptions {
    private EmailTransport transport = EmailTransport.file;

    /**
     * The directory in which the emails will be save.
     *
     * Applicable only if EmailTransport.file is used.
     */
    private String outputPath;

    /**
     * Default from email address.
     */
    private String defaultFromEmail;

    /**
     * Global Template Variables
     */
    private String verifyEmailAddressUrl;

    private String passwordResetUrl;

    private String changeEmailAddressUrl;
}
