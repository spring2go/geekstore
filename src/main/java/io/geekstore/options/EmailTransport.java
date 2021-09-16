/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

/**
 * Created on Nov, 2020 by @author bobo
 */
public enum EmailTransport {
    /**
     * Send the email using Spring built in JavaMailSender
     */
    smtp("smtp"),
    /**
     * Outputs the email as an HTML file for development purpose.
     */
    file("file"),
    /**
     * Does nothing with the generated email. Mainly intended for use in testing where we don't care about
     * the email transport.
     */
    none("none");

    private final String name;

    EmailTransport(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
