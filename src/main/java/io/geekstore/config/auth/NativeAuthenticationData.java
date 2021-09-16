/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.auth;

import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class NativeAuthenticationData {
    private String username;
    private String password;
}
