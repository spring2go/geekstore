/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.utils;

import java.text.Normalizer;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class NormalizeUtil {
    /**
     * A simple normalization for email addresses. Lowercases the whole address,
     * even though technically the local part (before the '@') is case-sensitive
     * per the spec. In practice, however, it seems safe to treat emails as
     * case-insensitive to allow for users who might vary the usage of
     * upper/lower case. See more discussion here: https://ux.stackexchange.com/a/16849
     */
    public static String normalizeEmailAddress(String input) {
        return input.trim().toLowerCase();
    }

    public static String normalizeString(String input) {
        return normalizeString(input, " ");
    }

    public static String normalizeString(String input, String spaceReplacer) {
        if (input == null) input = "";
        input = Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        input = input.toLowerCase();
        input = input.replaceAll("\\s+", spaceReplacer);
        input = input.replaceAll("[^-a-zA-Z0-9\\.]", "");
        return input;
    }
}
