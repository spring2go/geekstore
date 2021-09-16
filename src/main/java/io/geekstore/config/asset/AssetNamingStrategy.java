/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

/**
 * The AssetNamingStrategy determines how file names are generated based on the uploaded source file name,
 * as well as how to handle naming conflicts.
 *
 * Created on Nov, 2020 by @author bobo
 */

public interface AssetNamingStrategy {
    /**
     * Given the original file name of the uploaded file, generate a file name to
     * be stored on the server. Operations like normalization and time-stamping can
     * be performed in this method.
     *
     * The output will be checked for a naming conflict with an existing file. If a conflict
     * exists, this method will be invoked again with the second argument passed in an a new, unique
     * file name should then be generated. This process will repeat until a unique file name has
     * been returned.
     */
    String generateSourceFileName(String originalFileName, String conflictFileName);

    /**
     * Given the source file name generated in the `generateSourceFileName` method, this method
     * should generate the file name of the preview image.
     *
     * The same mechanism of checking for conflicts is used as described above.
     */
    String generatePreviewFileName(String sourceFileName, String conflictFileName);
}
