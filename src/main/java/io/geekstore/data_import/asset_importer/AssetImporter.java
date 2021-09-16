/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import.asset_importer;

import io.geekstore.entity.AssetEntity;
import io.geekstore.service.AssetService;
import io.geekstore.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class AssetImporter {
    private Map<String, AssetEntity> assetMap = new HashMap<>();

    private final ConfigService configService;
    private final AssetService assetService;

    /**
     * Creates Asset entities for the given paths, using the assetMap cache to prevent
     * the creation of duplicates.
     */
    public AssetImportResult getAssets(List<String> assetPaths) {
        AssetImportResult result = new AssetImportResult();
        String importAssetDir = this.configService.getImportExportOptions().getImportAssetsDir();
        Set<String> uniqueAssetPaths = new HashSet<>(assetPaths);
        for(String assetPath : uniqueAssetPaths) {
            AssetEntity cachedAsset = this.assetMap.get(assetPath);
            if (cachedAsset != null) {
                result.getAssets().add(cachedAsset);
            } else {
                Path filePath = Paths.get(importAssetDir, assetPath);
                File file = filePath.toFile();
                if (file.exists()) {
                    if (file.isFile()) {
                        try {
                            InputStream stream = new FileInputStream(file);
                            AssetEntity asset =
                                    this.assetService.createFromFileStream(stream, filePath.toFile().getName());
                            this.assetMap.put(assetPath, asset);
                            result.getAssets().add(asset);
                        } catch (Exception ex) {
                            result.getErrors().add(ex.toString());
                        }
                    }
                } else {
                    result.getErrors().add("File " + filePath.toString() + " does not exist");
                }
            }
        }

        return result;
    }
}
