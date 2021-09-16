/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

import io.geekstore.common.utils.NormalizeUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The default strategy normalizes the file names to remove unwanted chars and
 * in the case of conflicts, increments a counter suffix.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class DefaultAssetNamingStrategy implements AssetNamingStrategy {

    private static Pattern numberingRe = Pattern.compile("__(\\d+)(\\.[^.]+)?$");

    @Override
    public String generateSourceFileName(String originalFileName, String conflictFileName) {
        String normalized = NormalizeUtil.normalizeString(originalFileName, "-");
        if (conflictFileName == null) {
            return normalized;
        } else {
            return this.incrementOrdinalSuffix(normalized, conflictFileName);
        }
    }

    @Override
    public String generatePreviewFileName(String sourceFileName, String conflictFileName) {
        String previewSuffix = "__preview";
        String previewFileName = this.isSupportedImageFormat(sourceFileName) ?
                this.addSuffix(sourceFileName, previewSuffix) : this.addSuffix(sourceFileName, previewSuffix) + ".png";

        if (conflictFileName == null) {
            return previewFileName;
        } else {
            return this.incrementOrdinalSuffix(previewFileName, conflictFileName);
        }

    }
    /**
     * A "supported format" means that the Sharp library can transform it and output the same
     * file type. Unsupported images and other non-image files will be converted to png.
     *
     * See http://sharp.pixelplumbing.com/en/stable/api-output/#tobuffer
     */
    private boolean isSupportedImageFormat(String fileName) {
        List<String> imageExtensions = Arrays.asList("jpg", "jpeg", "png", "webp", "tiff");
        String ext = FilenameUtils.getExtension(fileName);
        return imageExtensions.contains(ext);
    }

    private String incrementOrdinalSuffix(String baseFileName, String conflictFileName) {
        Matcher matcher = numberingRe.matcher(conflictFileName);
        int ord = matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
        return this.addOrdinalSuffix(baseFileName, ord + 1);
    }

    private String addOrdinalSuffix(String fileName, int order) {
        String paddedOrder = String.valueOf(order);
        if (paddedOrder.length() < 2) paddedOrder = "0" + paddedOrder;
        return this.addSuffix(fileName, "__" + paddedOrder);
    }

    private String addSuffix(String fileName, String suffix) {
        String ext = FilenameUtils.getExtension(fileName);
        String baseName = FilenameUtils.getBaseName(fileName);
        return baseName + suffix + (StringUtils.isEmpty(ext) ? "" : "." + ext);
    }
}
