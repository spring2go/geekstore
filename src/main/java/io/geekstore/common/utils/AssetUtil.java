/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.utils;

import io.geekstore.types.asset.AssetType;

/**
 * Created on Nov, 2020 by @author bobo
 */
public abstract class AssetUtil {
    /**
     * Returns the AssetType based on the mime type
     */
    public static AssetType getAssetType(String mimeType) {
        String type = mimeType.split("/")[0];
        switch (type) {
            case "image":
                return AssetType.IMAGE;
            case "video":
                return AssetType.VIDEO;
            default:
                return AssetType.BINARY;
        }
    }
}
