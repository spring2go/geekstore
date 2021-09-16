/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config;

import io.geekstore.config.asset.AssetStorageStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A mock storage strategy which does not actually persist the assets anywhere.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
public class TestAssetStorageStrategy implements AssetStorageStrategy {

    private final String outputPath = "target/test-assets";

    @Override
    public String writeFileFromBuffer(String fileName, byte[] buffer) {
        return outputPath + "/" + fileName;
    }

    @Override
    public String writeFileFromStream(String fileName, InputStream stream) throws IOException {
        File file = Paths.get(outputPath, fileName).toFile();

        byte[] buffer = new byte[stream.available()];
        stream.read(buffer);

        com.google.common.io.Files.write(buffer, file);

        return outputPath + "/" + fileName;
    }

    @Override
    public byte[] readFileToBuffer(String identifier) {
        return TestAssetPreviewStrategy.getTestImageBuffer();
    }

    @Override
    public InputStream readFileToStream(String identifier) {
        return new ByteArrayInputStream(identifier.getBytes());
    }

    @Override
    public void deleteFile(String identifier) {

    }

    @Override
    public boolean fileExists(String fileName) {
        return false;
    }

    @Override
    public String toAbsoluteUrl(String identifier) {
        return identifier.startsWith(outputPath) ? identifier : outputPath + "/" + identifier;
    }

    @PostConstruct
    void init() {
        try {
            Path path = Paths.get(outputPath);
            if (!Files.exists(path)) {
                log.info("test path [" + path + "] created.");
                Files.createDirectories(path);
            } else {
                log.info("test files cleaned in [" + path + "]");
                FileUtils.cleanDirectory(path.toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
