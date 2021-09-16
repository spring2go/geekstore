/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import io.geekstore.common.Constant;
import lombok.Data;

/**
 * These credentials will be used to create the Superadmin user & administrator
 * when GeekStore first bootstraps.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class SuperadminCredentials {
    /**
     * The identifier to be used to create a superadmin account
     *
     * @default 'superadmin'
     */
    private String identifier = Constant.SUPER_ADMIN_USER_IDENTIFIER;

    /**
     * The password to be used to create a superadmin account
     * @default 'superadmin'
     */
    private String password = Constant.SUPER_ADMIN_USER_PASSWORD;
}
