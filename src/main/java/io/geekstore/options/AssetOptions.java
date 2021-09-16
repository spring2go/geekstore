/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.options;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class AssetOptions {
    /**
     * An array of the permitted file types that may be uploaded as Assets. Each entry
     * should be in the form of a valid
     * [unique file type specifier](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/file#Unique_file_type_specifiers)
     * i.e. either a file extension (".pdf") or a mime type ("image/*", "audio/mpeg" etc.).
     *
     * @default image, audio, video MIME types plus PDFs
     */
    private List<String> permittedFileTypes = Arrays.asList("image/*", "video/*", "audio/*", "application/pdf");

    /**
     * The max file size in bytes for uploaded assets.
     */
    private Integer uploadMaxFileSize = 20971520;
}
