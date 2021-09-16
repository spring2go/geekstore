/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class DefaultAssetNamingStrategyTest {
    /**
     * generateSourceFileName test suite
     */
    @Test
    public void normalizes_file_names() {
        AssetNamingStrategy strategy = new DefaultAssetNamingStrategy();
        assertThat(strategy.generateSourceFileName("foo.jpg", null))
                .isEqualTo("foo.jpg");
        assertThat(strategy.generateSourceFileName("curaçao.jpg", null))
                .isEqualTo("curacao.jpg");
        assertThat(strategy.generateSourceFileName("dấu hỏi.jpg", null))
                .isEqualTo("dau-hoi.jpg");
    }

    @Test
    public void increments_conflicting_file_names() {
        AssetNamingStrategy strategy = new DefaultAssetNamingStrategy();

        assertThat(strategy.generateSourceFileName("foo.jpg", "foo.jpg"))
                .isEqualTo("foo__02.jpg");
        assertThat(strategy.generateSourceFileName("foo.jpg", "foo__02.jpg"))
                .isEqualTo("foo__03.jpg");
        assertThat(strategy.generateSourceFileName("foo.jpg", "foo__09.jpg"))
                .isEqualTo("foo__10.jpg");
        assertThat(strategy.generateSourceFileName("foo.jpg", "foo__99.jpg"))
                .isEqualTo("foo__100.jpg");
        assertThat(strategy.generateSourceFileName("foo.jpg", "foo__999.jpg"))
                .isEqualTo("foo__1000.jpg");
    }

    @Test
    public void increments_conflicting_file_names_with_no_extension() {
        AssetNamingStrategy strategy = new DefaultAssetNamingStrategy();

        assertThat(strategy.generateSourceFileName(
                "ext45000000000505", "ext45000000000505"))
                .isEqualTo("ext45000000000505__02");
        assertThat(strategy.generateSourceFileName(
                "ext45000000000505", "ext45000000000505__02"))
                .isEqualTo("ext45000000000505__03");
        assertThat(strategy.generateSourceFileName(
                "ext45000000000505", "ext45000000000505__09"))
                .isEqualTo("ext45000000000505__10");
    }

    /**
     * generatePreviewFileName test suite
     */

    @Test
    public void adds_the_preview_suffix() {
        AssetNamingStrategy strategy = new DefaultAssetNamingStrategy();

        assertThat(strategy.generatePreviewFileName(
                "foo.jpg", null)).isEqualTo("foo__preview.jpg");
    }


    @Test
    public void preserves_the_extension_of_supported_image_files() {
        AssetNamingStrategy strategy = new DefaultAssetNamingStrategy();

        assertThat(strategy.generatePreviewFileName(
                "foo.jpg", null)).isEqualTo("foo__preview.jpg");
        assertThat(strategy.generatePreviewFileName(
                "foo.jpeg", null)).isEqualTo("foo__preview.jpeg");
        assertThat(strategy.generatePreviewFileName(
                "foo.png", null)).isEqualTo("foo__preview.png");
        assertThat(strategy.generatePreviewFileName(
                "foo.webp", null)).isEqualTo("foo__preview.webp");
        assertThat(strategy.generatePreviewFileName(
                "foo.tiff", null)).isEqualTo("foo__preview.tiff");
    }

    @Test
    public void adds_a_png_extension_for_unsupported_images_and_other_files() {
        AssetNamingStrategy strategy = new DefaultAssetNamingStrategy();

        assertThat(strategy.generatePreviewFileName(
                "foo.svg", null)).isEqualTo("foo__preview.svg.png");
        assertThat(strategy.generatePreviewFileName(
                "foo.gif", null)).isEqualTo("foo__preview.gif.png");
        assertThat(strategy.generatePreviewFileName(
                "foo.pdf", null)).isEqualTo("foo__preview.pdf.png");
    }
}
