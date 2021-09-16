/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created on Nov, 2020 by @author bobo
 */
@ConfigurationProperties(prefix = "geekstore")
@Data
public class ConfigOptions {
    /**
     * Configuration for authorization.
     */
    private AuthOptions authOptions = new AuthOptions();

    /**
     * Configuration for email.
     */
    private EmailOptions emailOptions = new EmailOptions();

    /**
     * Configuration for assets.
     */
    private AssetOptions assetOptions = new AssetOptions();

    /**
     * Configuration settings for data import and export.
     */
    private ImportExportOptions importExportOptions = new ImportExportOptions();

    /**
     * Configuration settings governing how orders are handled.
     */
    private OrderOptions orderOptions = new OrderOptions();
}
