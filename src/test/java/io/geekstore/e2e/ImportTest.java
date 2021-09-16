/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.types.ImportInfo;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.product.*;
import io.geekstore.types.stock.StockMovement;
import io.geekstore.types.stock.StockMovementType;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphql.spring.boot.test.GraphQLResponse;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ImportTest {

    static final String IMPORT_PRODUCTS = "graphql/admin/import/import_products.graphqls";
    static final String GET_PRODUCTS = "graphql/admin/import/get_products.graphqls";
    static final String PRODUCT_IMPORT_CSV_FILE = "src/test/resources/fixtures/product-import.csv";

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(0).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-empty.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    @Test
    public void import_products() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.uploadSingleFile(
                IMPORT_PRODUCTS,
                PRODUCT_IMPORT_CSV_FILE
        );
        assertThat(graphQLResponse.isOk());

        ImportInfo importInfo = graphQLResponse.get("$.data.importProducts", ImportInfo.class);
        assertThat(importInfo.getImported()).isEqualTo(4);
        assertThat(importInfo.getProcessed()).isEqualTo(4);

        graphQLResponse = adminClient.perform(GET_PRODUCTS, null);
        assertThat(graphQLResponse.isOk());
        ProductList productResult = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productResult.getTotalItems()).isEqualTo(4);

        Product paperStretcher = productResult.getItems().stream()
                .filter(p -> Objects.equals(p.getName(), "Perfect Paper Stretcher")).findFirst().get();
        verifyPaperStretcher(paperStretcher);

        Product easel = productResult.getItems().stream()
                .filter(p -> Objects.equals(p.getName(), "Mabef M/02 Studio Easel")).findFirst().get();
        verifyEasel(easel);

        Product pencils = productResult.getItems().stream()
                .filter(p -> Objects.equals(p.getName(), "Giotto Mega Pencils")).findFirst().get();
        verifyPencils(pencils);

        Product smock = productResult.getItems().stream()
                .filter(p -> Objects.equals(p.getName(), "Artists Smock")).findFirst().get();
        verifySmock(smock);

        assertThat(paperStretcher.getFacetValues()).isEmpty();
        assertThat(easel.getFacetValues()).isEmpty();
        assertThat(pencils.getFacetValues()).isEmpty();
        assertThat(smock.getFacetValues().stream().map(FacetValue::getName).sorted().collect(Collectors.toList()))
                .containsExactly("Denim", "clothes");

        assertThat(paperStretcher.getVariants().get(0).getFacetValues().stream()
             .map(FacetValue::getName).sorted().collect(Collectors.toList())).containsExactly("Accessory", "KB");
        assertThat(paperStretcher.getVariants().get(1).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).containsExactly("Accessory", "KB");
        assertThat(paperStretcher.getVariants().get(2).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).containsExactly("Accessory", "KB");

        assertThat(paperStretcher.getVariants().get(0).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("half-imperial");
        assertThat(paperStretcher.getVariants().get(1).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("quarter-imperial");
        assertThat(paperStretcher.getVariants().get(2).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("full-imperial");

        assertThat(easel.getVariants().get(0).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).containsExactly("Easel", "Mabef");

        assertThat(pencils.getVariants().get(0).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).containsExactly("Xmas Sale");
        assertThat(pencils.getVariants().get(1).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).containsExactly("Xmas Sale");
        assertThat(pencils.getVariants().get(0).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("box-of-8");
        assertThat(pencils.getVariants().get(1).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("box-of-12");

        assertThat(smock.getVariants().get(0).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).isEmpty();
        assertThat(smock.getVariants().get(1).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).isEmpty();
        assertThat(smock.getVariants().get(2).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).isEmpty();
        assertThat(smock.getVariants().get(3).getFacetValues().stream()
                .map(FacetValue::getName).sorted().collect(Collectors.toList())).isEmpty();

        assertThat(smock.getVariants().get(0).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("beige", "small");
        assertThat(smock.getVariants().get(1).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("beige", "large");
        assertThat(smock.getVariants().get(2).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("navy", "small");
        assertThat(smock.getVariants().get(3).getOptions().stream()
                .map(ProductOption::getCode).sorted().collect(Collectors.toList())).containsExactly("large", "navy");
    }

    private void verifySmock(Product product) {
        assertThat(product.getAssets()).isEmpty();
        assertThat(product.getDescription()).isEqualTo("Keeps the paint off the clothes");
        assertThat(product.getFeaturedAsset()).isNull();
        assertThat(product.getId()).isEqualTo(4L);
        assertThat(product.getName()).isEqualTo("Artists Smock");

        assertThat(product.getOptionGroups()).hasSize(2);
        ProductOptionGroup optionGroup1 = product.getOptionGroups().get(0);
        assertThat(optionGroup1.getCode()).isEqualTo("artists-smock-size");
        assertThat(optionGroup1.getId()).isEqualTo(3L);
        assertThat(optionGroup1.getName()).isEqualTo("size");
        ProductOptionGroup optionGroup2 = product.getOptionGroups().get(1);
        assertThat(optionGroup2.getCode()).isEqualTo("artists-smock-colour");
        assertThat(optionGroup2.getId()).isEqualTo(4L);
        assertThat(optionGroup2.getName()).isEqualTo("colour");

        assertThat(product.getSlug()).isEqualTo("artists-smock");

        assertThat(product.getVariants()).hasSize(4);
        ProductVariant variant1 = product.getVariants().get(0);
        assertThat(variant1.getAssets()).isEmpty();
        assertThat(variant1.getFeaturedAsset()).isNull();
        assertThat(variant1.getId()).isEqualTo(7L);
        assertThat(variant1.getName()).isEqualTo("Artists Smock small beige");
        assertThat(variant1.getPrice()).isEqualTo(1199);
        assertThat(variant1.getSku()).isEqualTo("10112");
        assertThat(variant1.getStockMovements().getItems()).isEmpty();
        assertThat(variant1.getStockOnHand()).isEqualTo(0);
        assertThat(variant1.isTrackInventory()).isFalse();

        ProductVariant variant2 = product.getVariants().get(1);
        assertThat(variant2.getAssets()).isEmpty();
        assertThat(variant2.getFeaturedAsset()).isNull();
        assertThat(variant2.getId()).isEqualTo(8L);
        assertThat(variant2.getName()).isEqualTo("Artists Smock large beige");
        assertThat(variant2.getPrice()).isEqualTo(1199);
        assertThat(variant2.getSku()).isEqualTo("10113");
        assertThat(variant2.getStockMovements().getItems()).isEmpty();
        assertThat(variant2.getStockOnHand()).isEqualTo(0);
        assertThat(variant2.isTrackInventory()).isFalse();

        ProductVariant variant3 = product.getVariants().get(2);
        assertThat(variant3.getAssets()).isEmpty();
        assertThat(variant3.getFeaturedAsset()).isNull();
        assertThat(variant3.getId()).isEqualTo(9L);
        assertThat(variant3.getName()).isEqualTo("Artists Smock small navy");
        assertThat(variant3.getPrice()).isEqualTo(1199);
        assertThat(variant3.getSku()).isEqualTo("10114");
        assertThat(variant3.getStockMovements().getItems()).isEmpty();
        assertThat(variant3.getStockOnHand()).isEqualTo(0);
        assertThat(variant3.isTrackInventory()).isFalse();

        ProductVariant variant4 = product.getVariants().get(3);
        assertThat(variant4.getAssets()).isEmpty();
        assertThat(variant4.getFeaturedAsset()).isNull();
        assertThat(variant4.getId()).isEqualTo(10L);
        assertThat(variant4.getName()).isEqualTo("Artists Smock large navy");
        assertThat(variant4.getPrice()).isEqualTo(1199);
        assertThat(variant4.getSku()).isEqualTo("10115");
        assertThat(variant4.getStockMovements().getItems()).isEmpty();
        assertThat(variant4.getStockOnHand()).isEqualTo(0);
        assertThat(variant4.isTrackInventory()).isFalse();
    }

    private void verifyPencils(Product product) {
        assertThat(product.getAssets()).isEmpty();
        assertThat(product.getDescription()).isEqualTo("Really mega pencils");
        assertThat(product.getFeaturedAsset()).isNull();
        assertThat(product.getId()).isEqualTo(3L);
        assertThat(product.getName()).isEqualTo("Giotto Mega Pencils");
        assertThat(product.getOptionGroups()).hasSize(1);
        ProductOptionGroup productOptionGroup = product.getOptionGroups().get(0);
        assertThat(productOptionGroup.getCode()).isEqualTo("giotto-mega-pencils-box-size");
        assertThat(productOptionGroup.getId()).isEqualTo(2L);
        assertThat(productOptionGroup.getName()).isEqualTo("box size");
        assertThat(product.getSlug()).isEqualTo("giotto-mega-pencils");
        assertThat(product.getVariants()).hasSize(2);

        ProductVariant variant1 = product.getVariants().get(0);
        assertThat(variant1.getAssets()).hasSize(1);
        Asset asset = variant1.getAssets().get(0);
        assertThat(asset.getId()).isEqualTo(3);
        assertThat(asset.getName()).isEqualTo("box-of-8.jpg");
        assertThat(asset.getPreview()).isEqualTo("target/test-assets/box-of-8__preview.jpg");
        assertThat(asset.getSource()).isEqualTo("target/test-assets/box-of-8.jpg");

        Asset featuredAsset = variant1.getFeaturedAsset();
        assertThat(featuredAsset.getId()).isEqualTo(3L);
        assertThat(featuredAsset.getName()).isEqualTo("box-of-8.jpg");
        assertThat(featuredAsset.getPreview()).isEqualTo("target/test-assets/box-of-8__preview.jpg");
        assertThat(featuredAsset.getSource()).isEqualTo("target/test-assets/box-of-8.jpg");
        assertThat(variant1.getId()).isEqualTo(5L);
        assertThat(variant1.getName()).isEqualTo("Giotto Mega Pencils Box of 8");
        assertThat(variant1.getPrice()).isEqualTo(416);
        assertThat(variant1.getSku()).isEqualTo("225400");
        assertThat(variant1.getStockMovements().getItems()).isEmpty();
        assertThat(variant1.getStockOnHand()).isEqualTo(0);
        assertThat(variant1.isTrackInventory()).isFalse();

        ProductVariant variant2 = product.getVariants().get(1);
        assertThat(variant2.getAssets()).hasSize(1);
        Asset asset2 = variant2.getAssets().get(0);
        assertThat(asset2.getId()).isEqualTo(4);
        assertThat(asset2.getName()).isEqualTo("box-of-12.jpg");
        assertThat(asset2.getPreview()).isEqualTo("target/test-assets/box-of-12__preview.jpg");
        assertThat(asset2.getSource()).isEqualTo("target/test-assets/box-of-12.jpg");

        Asset featuredAsset2 = variant2.getFeaturedAsset();
        assertThat(featuredAsset2.getId()).isEqualTo(4L);
        assertThat(featuredAsset2.getName()).isEqualTo("box-of-12.jpg");
        assertThat(featuredAsset2.getPreview()).isEqualTo("target/test-assets/box-of-12__preview.jpg");
        assertThat(featuredAsset2.getSource()).isEqualTo("target/test-assets/box-of-12.jpg");
        assertThat(variant2.getId()).isEqualTo(6L);
        assertThat(variant2.getName()).isEqualTo("Giotto Mega Pencils Box of 12");
        assertThat(variant2.getPrice()).isEqualTo(624);
        assertThat(variant2.getSku()).isEqualTo("225600");
        assertThat(variant2.getStockMovements().getItems()).isEmpty();
        assertThat(variant2.getStockOnHand()).isEqualTo(0);
        assertThat(variant2.isTrackInventory()).isFalse();
    }

    private void verifyEasel(Product product) {
        assertThat(product.getAssets()).isEmpty();
        assertThat(product.getDescription()).isEqualTo("Mabef description");
        assertThat(product.getFeaturedAsset()).isNull();
        assertThat(product.getId()).isEqualTo(2L);
        assertThat(product.getName()).isEqualTo("Mabef M/02 Studio Easel");
        assertThat(product.getOptionGroups()).isEmpty();
        assertThat(product.getSlug()).isEqualTo("mabef-m02-studio-easel");

        assertThat(product.getVariants()).hasSize(1);
        ProductVariant variant = product.getVariants().get(0);
        assertThat(variant.getAssets()).isEmpty();
        assertThat(variant.getFeaturedAsset()).isNull();
        assertThat(variant.getId()).isEqualTo(4L);
        assertThat(variant.getName()).isEqualTo("Mabef M/02 Studio Easel");
        assertThat(variant.getPrice()).isEqualTo(91070);
        assertThat(variant.getSku()).isEqualTo("M02");
        assertThat(variant.getStockMovements().getItems()).hasSize(1);
        StockMovement stockMovement = variant.getStockMovements().getItems().get(0);
        assertThat(stockMovement.getId()).isEqualTo(2L);
        assertThat(stockMovement.getQuantity()).isEqualTo(100);
        assertThat(stockMovement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(variant.getStockOnHand()).isEqualTo(100);
        assertThat(variant.isTrackInventory()).isEqualTo(false);
    }

    private void verifyPaperStretcher(Product product) {
        assertThat(product.getAssets()).hasSize(2);
        Asset asset1 = product.getAssets().get(0);
        assertThat(asset1.getId()).isEqualTo(1L);
        assertThat(asset1.getName()).isEqualTo("pps1.jpg");
        assertThat(asset1.getPreview()).isEqualTo("target/test-assets/pps1__preview.jpg");
        assertThat(asset1.getSource()).isEqualTo("target/test-assets/pps1.jpg");
        Asset asset2 = product.getAssets().get(1);
        assertThat(asset2.getId()).isEqualTo(2L);
        assertThat(asset2.getName()).isEqualTo("pps2.jpg");
        assertThat(asset2.getPreview()).isEqualTo("target/test-assets/pps2__preview.jpg");
        assertThat(asset2.getSource()).isEqualTo("target/test-assets/pps2.jpg");

        assertThat(product.getDescription()).isEqualTo("A great device for stretching paper.");
        Asset featuredAsset = product.getFeaturedAsset();
        assertThat(featuredAsset.getId()).isEqualTo(1L);
        assertThat(featuredAsset.getName()).isEqualTo("pps1.jpg");
        assertThat(featuredAsset.getPreview()).isEqualTo("target/test-assets/pps1__preview.jpg");
        assertThat(featuredAsset.getSource()).isEqualTo("target/test-assets/pps1.jpg");

        assertThat(product.getId()).isEqualTo(1L);
        assertThat(product.getName()).isEqualTo("Perfect Paper Stretcher");

        assertThat(product.getOptionGroups()).hasSize(1);
        ProductOptionGroup optionGroup = product.getOptionGroups().get(0);
        assertThat(optionGroup.getCode()).isEqualTo("perfect-paper-stretcher-size");
        assertThat(optionGroup.getId()).isEqualTo(1L);
        assertThat(optionGroup.getName()).isEqualTo("size");

        assertThat(product.getSlug()).isEqualTo("perfect-paper-stretcher");

        assertThat(product.getVariants()).hasSize(3);

        ProductVariant variant1 = product.getVariants().get(0);
        assertThat(variant1.getAssets()).isEmpty();
        assertThat(variant1.getFeaturedAsset()).isNull();
        assertThat(variant1.getId()).isEqualTo(1L);
        assertThat(variant1.getName()).isEqualTo("Perfect Paper Stretcher Half Imperial");
        assertThat(variant1.getPrice()).isEqualTo(4530);
        assertThat(variant1.getSku()).isEqualTo("PPS12");
        assertThat(variant1.getStockMovements()).isNotNull();
        assertThat(variant1.getStockMovements().getItems()).isEmpty();
        assertThat(variant1.getStockOnHand()).isEqualTo(0);
        assertThat(variant1.isTrackInventory()).isEqualTo(false);

        ProductVariant variant2 = product.getVariants().get(1);
        assertThat(variant2.getAssets()).isEmpty();
        assertThat(variant2.getFeaturedAsset()).isNull();
        assertThat(variant2.getId()).isEqualTo(2L);
        assertThat(variant2.getName()).isEqualTo("Perfect Paper Stretcher Quarter Imperial");
        assertThat(variant2.getPrice()).isEqualTo(3250);
        assertThat(variant2.getSku()).isEqualTo("PPS14");
        assertThat(variant2.getStockMovements()).isNotNull();
        assertThat(variant2.getStockMovements().getItems()).isEmpty();
        assertThat(variant2.getStockOnHand()).isEqualTo(0);
        assertThat(variant2.isTrackInventory()).isEqualTo(false);

        ProductVariant variant3 = product.getVariants().get(2);
        assertThat(variant3.getAssets()).isEmpty();
        assertThat(variant3.getFeaturedAsset()).isNull();
        assertThat(variant3.getId()).isEqualTo(3L);
        assertThat(variant3.getName()).isEqualTo("Perfect Paper Stretcher Full Imperial");
        assertThat(variant3.getPrice()).isEqualTo(5950);
        assertThat(variant3.getSku()).isEqualTo("PPSF");
        assertThat(variant3.getStockMovements()).isNotNull();
        assertThat(variant3.getStockMovements().getItems()).hasSize(1);
        StockMovement stockMovement = variant3.getStockMovements().getItems().get(0);
        assertThat(stockMovement.getId()).isEqualTo(1L);
        assertThat(stockMovement.getQuantity()).isEqualTo(-10);
        assertThat(stockMovement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(variant3.getStockOnHand()).isEqualTo(-10);
        assertThat(variant3.isTrackInventory()).isEqualTo(false);
    }

}
