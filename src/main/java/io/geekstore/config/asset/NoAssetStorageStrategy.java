/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

import io.geekstore.exception.ErrorCode;
import io.geekstore.exception.InternalServerError;

import java.io.IOException;
import java.io.InputStream;

/**
 * A placeholder strategy which will simply throw an error when used.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class NoAssetStorageStrategy implements AssetStorageStrategy {
    @Override
    public String writeFileFromBuffer(String fileName, byte[] buffer) {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }

    @Override
    public String writeFileFromStream(String fileName, InputStream stream) throws IOException {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }

    @Override
    public byte[] readFileToBuffer(String identifier) {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }

    @Override
    public InputStream readFileToStream(String identifier) {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }

    @Override
    public void deleteFile(String identifier) {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }

    @Override
    public boolean fileExists(String fileName) {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }

    @Override
    public String toAbsoluteUrl(String identifier) {
        throw new InternalServerError(ErrorCode.NO_ASSET_STORAGE_STRATEGY_CONFIGURED);
    }
}
