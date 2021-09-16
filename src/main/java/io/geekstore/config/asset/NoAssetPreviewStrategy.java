/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

import io.geekstore.exception.ErrorCode;
import io.geekstore.exception.InternalServerError;

/**
 * A placeholder strategy which will simply throw an error when used.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class NoAssetPreviewStrategy implements AssetPreviewStrategy {
    @Override
    public byte[] generatePreviewImage(String mimeType, byte[] data) {
        throw new InternalServerError(ErrorCode.NO_ASSET_PREVIEW_STRATEGY_CONFIGURED);
    }
}
