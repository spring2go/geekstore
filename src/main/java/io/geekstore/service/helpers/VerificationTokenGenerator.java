/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.common.utils.IdUtil;
import io.geekstore.common.utils.TimeSpanUtil;
import io.geekstore.exception.VerificationTokenException;
import io.geekstore.service.ConfigService;
import com.google.common.primitives.Longs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationTokenGenerator {
    private final ConfigService configService;

    public String generateVerificationToken() {
        long now = System.currentTimeMillis();
        byte[] nowBytes = Longs.toByteArray(now);
        String base64Now = Base64Utils.encodeToString(nowBytes);
        String id = IdUtil.generatePublicId();
        return base64Now + "_" + id;
    }

    public boolean verifyVerificationToken(String token) {
        try {
            long duration = TimeSpanUtil.toMs(configService.getAuthOptions().getVerificationTokenDuration());
            String generateOnString = token.split("_")[0];
            byte[] dateBytes = Base64Utils.decodeFromString(generateOnString);
            long dateLong = Longs.fromByteArray(dateBytes);
            long elapsed = System.currentTimeMillis() - dateLong;
            return elapsed < duration;
        } catch (Exception ex) {
            log.warn("Fail to parse verification token", ex);
            throw new VerificationTokenException();
        }
    }
}
