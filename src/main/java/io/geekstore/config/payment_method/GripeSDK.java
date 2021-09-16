/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.payment_method;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A dummy API to simulate an SDK provided by a popular payments service.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class GripeSDK {
    public static CompletableFuture<String> create(Object any) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        completableFuture.complete(UUID.randomUUID().toString());
        return completableFuture;
    }

    public static boolean capture(String transactionId) {
        return true;
    }
}
