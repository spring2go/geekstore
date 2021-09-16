/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

/**
 * The AssetPreviewStrategy determines how preview images for assets are created. For image
 * assets, this would usually typically involve resizing to sensible dimensions. Other file types
 * could be previewed in a variety of ways, e.g.:
 * - waveform images generated for audio files
 * - preview images generated for pdf documents
 * - watermarks added to preview images.
 *
 * Created on Nov, 2020 by @author bobo
 */
public interface AssetPreviewStrategy {
    byte[] generatePreviewImage(String mimeType, byte[] data);
}
