/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.utils;

import java.security.SecureRandom;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class TokenUtil {
    private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    public static String generateNewToken(int size) {
        byte[] randomBytes = new byte[size];
        secureRandom.nextBytes(randomBytes);
        return HexUtil.bytesToHex(randomBytes);
    }
}
