/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * The AssetConfig define how assets (images and other files) are named and stored, and how preview images are generated.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
@Configuration
public class AssetConfig {
    /**
     * Defines how asset files and preview images are named before being saved.
     */
    @Autowired
    private AssetNamingStrategy assetNamingStrategy;

    /**
     * Defines the strategy used for creating preview images of uploaded assets.
     */
    @Autowired
    private AssetPreviewStrategy assetPreviewStrategy;

    /**
     *  Defines the strategy used for storing uploaded binary files.
     */
    @Autowired
    private AssetStorageStrategy assetStorageStrategy;
}
