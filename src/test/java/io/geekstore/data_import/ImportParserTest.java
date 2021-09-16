/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.data_import;

import io.geekstore.data_import.parser.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
@RunWith(SpringRunner.class)
public class ImportParserTest {

    public static final String TEST_FIXTURES_PATH = "test_fixtures/";

    /**
     * parseProducts
     */

    @Test
    public void single_product_with_a_single_variant() throws IOException {
        ImportParser importParser = new ImportParser();

        Reader reader = loadTestFixture("single-product-single-variant.csv");
        ParseResult<ParsedProductWithVariants> parseResult = importParser.parseProducts(reader);
        assertThat(parseResult.getErrors()).isEmpty();
        assertThat(parseResult.getProcessed()).isEqualTo(1L);
        assertThat(parseResult.getResults()).hasSize(1);
        ParsedProduct product = parseResult.getResults().get(0).getProduct();
        assertThat(product.getAssetPaths()).containsExactlyInAnyOrder("pps1.jpg", "pps2.jpg");
        assertThat(product.getDescription()).isEqualTo("A great device for stretching paper.");
        assertThat(product.getFacets()).containsExactlyInAnyOrder(
                new StringFacet("brand", "KB"),
                new StringFacet("type", "Accessory")
        );
        assertThat(product.getName()).isEqualTo("Perfect Paper Stretcher");
        assertThat(product.getOptionGroups()).isEmpty();
        assertThat(product.getSlug()).isEqualTo("perfect-paper-stretcher");

        assertThat(parseResult.getResults().get(0).getVariants()).hasSize(1);
        ParsedProductVariant variant = parseResult.getResults().get(0).getVariants().get(0);
        assertThat(variant.getAssetPaths()).isEmpty();
        assertThat(variant.getFacets()).containsExactlyInAnyOrder(
                new StringFacet("material", "Wood")
        );
        assertThat(variant.getOptionValues()).isEmpty();
        assertThat(variant.getPrice()).isEqualTo(45.3F);
        assertThat(variant.getSku()).isEqualTo("PPS12");
        assertThat(variant.getStockOnHand()).isEqualTo(10);
        assertThat(variant.getTrackInventory()).isEqualTo(false);
    }

    @Test
    public void single_product_with_a_multiple_variants() throws IOException {
        ImportParser importParser = new ImportParser();

        Reader reader = loadTestFixture("single-product-multiple-variants.csv");
        ParseResult<ParsedProductWithVariants> parseResult = importParser.parseProducts(reader);
        assertThat(parseResult.getErrors()).isEmpty();
        assertThat(parseResult.getProcessed()).isEqualTo(1L);
        assertThat(parseResult.getResults()).hasSize(1);
        ParsedProduct product = parseResult.getResults().get(0).getProduct();
        assertThat(product.getAssetPaths()).isEmpty();
        assertThat(product.getDescription()).isEqualTo("A great device for stretching paper.");
        assertThat(product.getFacets()).isEmpty();
        assertThat(product.getName()).isEqualTo("Perfect Paper Stretcher");
        StringOptionGroup expectedOptionGroup = new StringOptionGroup();
        expectedOptionGroup.setName("size");
        expectedOptionGroup.getValues().addAll(Arrays.asList("Half Imperial", "Quarter Imperial", "Full Imperial"));
        assertThat(product.getOptionGroups()).containsExactly(expectedOptionGroup);
        assertThat(product.getSlug()).isEqualTo("perfect-paper-stretcher");

        List<ParsedProductVariant> variants = parseResult.getResults().get(0).getVariants();
        assertThat(variants).hasSize(3);

        ParsedProductVariant variant1 = variants.get(0);
        assertThat(variant1.getAssetPaths()).isEmpty();
        assertThat(variant1.getFacets()).isEmpty();
        assertThat(variant1.getOptionValues()).containsExactly("Half Imperial");
        assertThat(variant1.getPrice()).isEqualTo(45.3F);
        assertThat(variant1.getSku()).isEqualTo("PPS12");
        assertThat(variant1.getStockOnHand()).isEqualTo(10);
        assertThat(variant1.getTrackInventory()).isFalse();

        ParsedProductVariant variant2 = variants.get(1);
        assertThat(variant2.getAssetPaths()).isEmpty();
        assertThat(variant2.getFacets()).isEmpty();
        assertThat(variant2.getOptionValues()).containsExactly("Quarter Imperial");
        assertThat(variant2.getPrice()).isEqualTo(32.5F);
        assertThat(variant2.getSku()).isEqualTo("PPS14");
        assertThat(variant2.getStockOnHand()).isEqualTo(10);
        assertThat(variant2.getTrackInventory()).isFalse();

        ParsedProductVariant variant3 = variants.get(2);
        assertThat(variant3.getAssetPaths()).isEmpty();
        assertThat(variant3.getFacets()).isEmpty();
        assertThat(variant3.getOptionValues()).containsExactly("Full Imperial");
        assertThat(variant3.getPrice()).isEqualTo(59.5F);
        assertThat(variant3.getSku()).isEqualTo("PPSF");
        assertThat(variant3.getStockOnHand()).isEqualTo(10);
        assertThat(variant3.getTrackInventory()).isFalse();
    }

    @Test
    public void multiple_products_with_multiple_variants() throws IOException {
        ImportParser importParser = new ImportParser();

        Reader reader = loadTestFixture("multiple-products-multiple-variants.csv");
        ParseResult<ParsedProductWithVariants> parseResult = importParser.parseProducts(reader);
        assertThat(parseResult.getErrors()).isEmpty();
        assertThat(parseResult.getProcessed()).isEqualTo(4L);
        assertThat(parseResult.getResults()).hasSize(4);

        ParsedProduct product1 = parseResult.getResults().get(0).getProduct();
        assertThat(product1.getAssetPaths()).isEmpty();
        assertThat(product1.getDescription()).isEqualTo("A great device for stretching paper.");
        assertThat(product1.getFacets()).isEmpty();
        assertThat(product1.getName()).isEqualTo("Perfect Paper Stretcher");
        StringOptionGroup expectedOptionGroup = new StringOptionGroup();
        expectedOptionGroup.setName("size");
        expectedOptionGroup.getValues().addAll(Arrays.asList("Half Imperial", "Quarter Imperial", "Full Imperial"));
        assertThat(product1.getOptionGroups()).containsExactly(expectedOptionGroup);
        assertThat(product1.getSlug()).isEqualTo("Perfect-paper-stretcher");

        List<ParsedProductVariant> variants1 = parseResult.getResults().get(0).getVariants();
        assertThat(variants1).hasSize(3);

        ParsedProductVariant variant11 = variants1.get(0);
        assertThat(variant11.getAssetPaths()).isEmpty();
        assertThat(variant11.getFacets()).isEmpty();
        assertThat(variant11.getOptionValues()).containsExactly("Half Imperial");
        assertThat(variant11.getPrice()).isEqualTo(45.3F);
        assertThat(variant11.getSku()).isEqualTo("PPS12");
        assertThat(variant11.getStockOnHand()).isEqualTo(10);
        assertThat(variant11.getTrackInventory()).isFalse();

        ParsedProductVariant variant12 = variants1.get(1);
        assertThat(variant12.getAssetPaths()).isEmpty();
        assertThat(variant12.getFacets()).isEmpty();
        assertThat(variant12.getOptionValues()).containsExactly("Quarter Imperial");
        assertThat(variant12.getPrice()).isEqualTo(32.5F);
        assertThat(variant12.getSku()).isEqualTo("PPS14");
        assertThat(variant12.getStockOnHand()).isEqualTo(11);
        assertThat(variant12.getTrackInventory()).isTrue();

        ParsedProductVariant variant13 = variants1.get(2);
        assertThat(variant13.getAssetPaths()).isEmpty();
        assertThat(variant13.getFacets()).isEmpty();
        assertThat(variant13.getOptionValues()).containsExactly("Full Imperial");
        assertThat(variant13.getPrice()).isEqualTo(59.5F);
        assertThat(variant13.getSku()).isEqualTo("PPSF");
        assertThat(variant13.getStockOnHand()).isEqualTo(12);
        assertThat(variant13.getTrackInventory()).isFalse();

        ParsedProduct product2 = parseResult.getResults().get(1).getProduct();
        assertThat(product2.getAssetPaths()).isEmpty();
        assertThat(product2.getDescription()).isEqualTo("Mabef description");
        assertThat(product2.getFacets()).isEmpty();
        assertThat(product2.getName()).isEqualTo("Mabef M/02 Studio Easel");
        assertThat(product2.getOptionGroups()).isEmpty();
        assertThat(product2.getSlug()).isEqualTo("mabef-m02-studio-easel");

        List<ParsedProductVariant> variants2 = parseResult.getResults().get(1).getVariants();
        assertThat(variants2).hasSize(1);

        ParsedProductVariant variant21 = variants2.get(0);
        assertThat(variant21.getAssetPaths()).isEmpty();
        assertThat(variant21.getFacets()).isEmpty();
        assertThat(variant21.getOptionValues()).isEmpty();
        assertThat(variant21.getPrice()).isEqualTo(910.7F);
        assertThat(variant21.getSku()).isEqualTo("M02");
        assertThat(variant21.getStockOnHand()).isEqualTo(13);
        assertThat(variant21.getTrackInventory()).isTrue();

        ParsedProduct product3 = parseResult.getResults().get(2).getProduct();
        assertThat(product3.getAssetPaths()).isEmpty();
        assertThat(product3.getDescription()).isEqualTo("Really mega pencils");
        assertThat(product3.getFacets()).isEmpty();
        assertThat(product3.getName()).isEqualTo("Giotto Mega Pencils");
        expectedOptionGroup = new StringOptionGroup();
        expectedOptionGroup.setName("box size");
        expectedOptionGroup.getValues().addAll(Arrays.asList("Box of 8", "Box of 12"));
        assertThat(product3.getOptionGroups()).containsExactly(expectedOptionGroup);
        assertThat(product3.getSlug()).isEqualTo("giotto-mega-pencils");

        List<ParsedProductVariant> variants3 = parseResult.getResults().get(2).getVariants();
        assertThat(variants3).hasSize(2);

        ParsedProductVariant variant31 = variants3.get(0);
        assertThat(variant31.getAssetPaths()).isEmpty();
        assertThat(variant31.getFacets()).isEmpty();
        assertThat(variant31.getOptionValues()).containsExactly("Box of 8");
        assertThat(variant31.getPrice()).isEqualTo(4.16F);
        assertThat(variant31.getSku()).isEqualTo("225400");
        assertThat(variant31.getStockOnHand()).isEqualTo(14);
        assertThat(variant31.getTrackInventory()).isFalse();

        ParsedProductVariant variant32 = variants3.get(1);
        assertThat(variant32.getAssetPaths()).isEmpty();
        assertThat(variant32.getFacets()).isEmpty();
        assertThat(variant32.getOptionValues()).containsExactly("Box of 12");
        assertThat(variant32.getPrice()).isEqualTo(6.24F);
        assertThat(variant32.getSku()).isEqualTo("225600");
        assertThat(variant32.getStockOnHand()).isEqualTo(15);
        assertThat(variant32.getTrackInventory()).isTrue();

        ParsedProduct product4 = parseResult.getResults().get(3).getProduct();
        assertThat(product4.getAssetPaths()).isEmpty();
        assertThat(product4.getDescription()).isEqualTo("Keeps the paint off the clothes");
        assertThat(product4.getFacets()).isEmpty();
        assertThat(product4.getName()).isEqualTo("Artists Smock");
        StringOptionGroup expectedOptionGroup1 = new StringOptionGroup();
        expectedOptionGroup1.setName("size");
        expectedOptionGroup1.getValues().addAll(Arrays.asList("small", "large"));
        StringOptionGroup expectedOptionGroup2 = new StringOptionGroup();
        expectedOptionGroup2.setName("colour");
        expectedOptionGroup2.getValues().addAll(Arrays.asList("beige", "navy"));
        assertThat(product4.getOptionGroups()).containsExactly(expectedOptionGroup1, expectedOptionGroup2);
        assertThat(product4.getSlug()).isEqualTo("artists-smock");

        List<ParsedProductVariant> variants4 = parseResult.getResults().get(3).getVariants();
        assertThat(variants4).hasSize(4);

        ParsedProductVariant variant41 = variants4.get(0);
        assertThat(variant41.getAssetPaths()).isEmpty();
        assertThat(variant41.getFacets()).isEmpty();
        assertThat(variant41.getOptionValues()).containsExactly("small", "beige");
        assertThat(variant41.getPrice()).isEqualTo(11.99F);
        assertThat(variant41.getSku()).isEqualTo("10112");
        assertThat(variant41.getStockOnHand()).isEqualTo(16);
        assertThat(variant41.getTrackInventory()).isFalse();

        ParsedProductVariant variant42 = variants4.get(1);
        assertThat(variant42.getAssetPaths()).isEmpty();
        assertThat(variant42.getFacets()).isEmpty();
        assertThat(variant42.getOptionValues()).containsExactly("large", "beige");
        assertThat(variant42.getPrice()).isEqualTo(11.99F);
        assertThat(variant42.getSku()).isEqualTo("10113");
        assertThat(variant42.getStockOnHand()).isEqualTo(17);
        assertThat(variant42.getTrackInventory()).isTrue();

        ParsedProductVariant variant43 = variants4.get(2);
        assertThat(variant43.getAssetPaths()).isEmpty();
        assertThat(variant43.getFacets()).isEmpty();
        assertThat(variant43.getOptionValues()).containsExactly("small", "navy");
        assertThat(variant43.getPrice()).isEqualTo(11.99F);
        assertThat(variant43.getSku()).isEqualTo("10114");
        assertThat(variant43.getStockOnHand()).isEqualTo(18);
        assertThat(variant43.getTrackInventory()).isFalse();

        ParsedProductVariant variant44 = variants4.get(3);
        assertThat(variant44.getAssetPaths()).isEmpty();
        assertThat(variant44.getFacets()).isEmpty();
        assertThat(variant44.getOptionValues()).containsExactly("large", "navy");
        assertThat(variant44.getPrice()).isEqualTo(11.99F);
        assertThat(variant44.getSku()).isEqualTo("10115");
        assertThat(variant44.getStockOnHand()).isEqualTo(19);
        assertThat(variant44.getTrackInventory()).isTrue();
    }

    /**
     * error conditions
     */

    @Test
    public void reports_errors_on_invalid_option_values() throws IOException {
        ImportParser importParser = new ImportParser();

        Reader reader = loadTestFixture("invalid-option-values.csv");
        ParseResult<ParsedProductWithVariants> parseResult = importParser.parseProducts(reader);
        assertThat(parseResult.getErrors()).hasSize(4);

        assertThat(parseResult.getErrors()).containsExactly(
                "The number of optionValues must match the number of optionGroups on line 2",
                "The number of optionValues must match the number of optionGroups on line 3",
                "The number of optionValues must match the number of optionGroups on line 4",
                "The number of optionValues must match the number of optionGroups on line 5"
        );
    }

    @Test
    public void reports_error_on_invalid_columns() throws IOException {
        ImportParser importParser = new ImportParser();

        Reader reader = loadTestFixture("invalid-columns.csv");
        ParseResult<ParsedProductWithVariants> parseResult = importParser.parseProducts(reader);
        assertThat(parseResult.getResults()).isEmpty();
        assertThat(parseResult.getErrors()).hasSize(1);

        assertThat(parseResult.getErrors()).containsExactly(
                "The import file is missing the following columns: \"slug\", \"assets\", \"variantFacets\""
        );
    }

    @Test
    public void reports_error_on_invalid_row_length() throws IOException {
        ImportParser importParser = new ImportParser();

        Reader reader = loadTestFixture("invalid-row-length.csv");
        ParseResult<ParsedProductWithVariants> parseResult = importParser.parseProducts(reader);
        assertThat(parseResult.getErrors()).hasSize(1);
    }

    private Reader loadTestFixture(String fileName) throws IOException {
        Resource resource =
                new DefaultResourceLoader().getResource("classpath:" + TEST_FIXTURES_PATH + fileName);
        Reader reader = Files.newBufferedReader(Paths.get(resource.getURI()));
        return reader;
    }
}
