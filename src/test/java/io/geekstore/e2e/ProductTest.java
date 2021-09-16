/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.asset.AssetList;
import io.geekstore.types.asset.AssetType;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.common.SortOrder;
import io.geekstore.types.common.StringOperators;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.product.*;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ProductTest {
    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_PRODUCT_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_list");
    static final String GET_PRODUCT_LIST_BY_SHOP =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_list_by_shop");
    static final String GET_PRODUCT_SIMPLE =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_simple");
    static final String GET_PRODUCT_WITH_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_with_variants");
    static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    static final String CREATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_product");
    static final String GET_ASSET_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_asset_list");
    static final String UPDATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product");
    static final String CREATE_PRODUCT_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_product_variants");
    static final String UPDATE_PRODUCT_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product_variants");
    static final String DELETE_PRODUCT_VARIANT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "delete_product_variant");
    static final String DELETE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "delete_product");

    static final String PRODUCT_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/product/%s.graphqls";
    static final String GET_PRODUCT_VARIANT =
            String.format(PRODUCT_GRAPHQL_RESOURCE_TEMPLATE, "get_product_variant");
    static final String ADD_OPTION_GROUP_TO_PRODUCT =
            String.format(PRODUCT_GRAPHQL_RESOURCE_TEMPLATE, "add_option_group_to_product");
    static final String REMOVE_OPTION_GROUP_FROM_PRODUCT =
            String.format(PRODUCT_GRAPHQL_RESOURCE_TEMPLATE, "remove_option_group_from_product");
    static final String GET_OPTION_GROUP =
            String.format(PRODUCT_GRAPHQL_RESOURCE_TEMPLATE, "get_option_group");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    @Qualifier(TestConfig.SHOP_CLIENT_BEAN)
    ApiClient shopClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    /**
     * products list query
     */

    @Test
    @Order(1)
    public void returns_all_products_when_no_options_passed() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, null);
        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productList.getItems()).hasSize(20);
        assertThat(productList.getTotalItems()).isEqualTo(20);
    }

    @Test
    @Order(2)
    public void limits_result_set_with_current_page_and_size() throws IOException {
        ProductListOptions options = new ProductListOptions();
        options.setCurrentPage(1);
        options.setPageSize(3);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, variables);

        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productList.getItems()).hasSize(3);
        assertThat(productList.getTotalItems()).isEqualTo(20);
    }

    @Test
    @Order(3)
    public void filters_by_name_admin() throws IOException {
        ProductListOptions options = new ProductListOptions();
        ProductFilterParameter filterParameter = new ProductFilterParameter();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setContains("skateboard");
        filterParameter.setName(stringOperators);
        options.setFilter(filterParameter);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, variables);

        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productList.getItems()).hasSize(1);
        assertThat(productList.getItems().get(0).getName()).isEqualTo("Cruiser Skateboard");
    }

    @Test
    @Order(4)
    public void filters_multiple_admin() throws IOException {
        ProductListOptions options = new ProductListOptions();
        ProductFilterParameter filterParameter = new ProductFilterParameter();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setContains("skateboard");
        filterParameter.setName(stringOperators);

        stringOperators = new StringOperators();
        stringOperators.setContains("tent");
        filterParameter.setSlug(stringOperators);

        options.setFilter(filterParameter);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, variables);

        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productList.getItems()).isEmpty();
    }

    @Test
    @Order(5)
    public void sorts_by_name_admin() throws IOException {
        ProductListOptions options = new ProductListOptions();

        ProductSortParameter sortParameter = new ProductSortParameter();
        sortParameter.setName(SortOrder.ASC);

        options.setSort(sortParameter);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, variables);

        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productList.getItems().stream()
                .map(p -> p.getName()).collect(Collectors.toList()))
                .containsExactly(
                        "Bonsai Tree",
                        "Boxing Gloves",
                        "Camera Lens",
                        "Clacky Keyboard",
                        "Cruiser Skateboard",
                        "Curvy Monitor",
                        "Football",
                        "Gaming PC",
                        "Hard Drive",
                        "Instant Camera",
                        "Laptop",
                        "Orchid",
                        "Road Bike",
                        "Running Shoe",
                        "Skipping Rope",
                        "Slr Camera",
                        "Spiky Cactus",
                        "Tent",
                        "Tripod",
                        "USB Cable"
                );
    }

    @Test
    @Order(6)
    public void filters_by_name_shop() throws IOException {
        ProductListOptions options = new ProductListOptions();
        ProductFilterParameter filterParameter = new ProductFilterParameter();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setContains("skateboard");
        filterParameter.setName(stringOperators);
        options.setFilter(filterParameter);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST_BY_SHOP, variables);

        ProductList productList = graphQLResponse.get("$.data.products", ProductList.class);
        assertThat(productList.getItems()).hasSize(1);
        assertThat(productList.getItems().get(0).getName()).isEqualTo("Cruiser Skateboard");
    }

    @Test
    @Order(7)
    public void sorts_by_name_shop() throws IOException {
        ProductListOptions options = new ProductListOptions();

        ProductSortParameter sortParameter = new ProductSortParameter();
        sortParameter.setName(SortOrder.ASC);

        options.setSort(sortParameter);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_LIST_BY_SHOP, variables);

        ProductList productList = graphQLResponse.get("$.data.products", ProductList.class);
        assertThat(productList.getItems().stream()
                .map(p -> p.getName()).collect(Collectors.toList()))
                .containsExactly(
                        "Bonsai Tree",
                        "Boxing Gloves",
                        "Camera Lens",
                        "Clacky Keyboard",
                        "Cruiser Skateboard",
                        "Curvy Monitor",
                        "Football",
                        "Gaming PC",
                        "Hard Drive",
                        "Instant Camera",
                        "Laptop",
                        "Orchid",
                        "Road Bike",
                        "Running Shoe",
                        "Skipping Rope",
                        "Slr Camera",
                        "Spiky Cactus",
                        "Tent",
                        "Tripod",
                        "USB Cable"
                );
    }

    /**
     * product query
     */

    @Test
    @Order(8)
    public void by_id() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_SIMPLE, variables);
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product.getId()).isEqualTo(2L);
    }

    @Test
    @Order(9)
    public void by_slug() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("slug", "curvy-monitor");

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_SIMPLE, variables);
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product.getSlug()).isEqualTo("curvy-monitor");
    }

    @Test
    @Order(10)
    public void throws_if_neither_id_nor_slug_provided() throws IOException {
        try {
            adminClient.perform(GET_PRODUCT_SIMPLE, null);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Either the Product id or slug must be provided");
        }
    }

    @Test
    @Order(11)
    public void throws_if_id_and_slug_do_not_refer_to_the_same_Product() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        variables.put("slug", "laptop");

        try {
            adminClient.perform(GET_PRODUCT_SIMPLE, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("The provided id and slug refer to different Products");
        }
    }

    @Test
    @Order(12)
    public void returns_expected_properties() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product.getAssets()).hasSize(1);
        Asset asset = product.getAssets().get(0);
        assertThat(asset.getFileSize()).isEqualTo(1680);
        assertThat(asset.getId()).isEqualTo(2L);
        assertThat(asset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset.getName()).isEqualTo("alexandru-acea-686569-unsplash.jpg");
        assertThat(asset.getPreview()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash__preview.jpg");
        assertThat(asset.getSource()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash.jpg");
        assertThat(asset.getType()).isEqualTo(AssetType.IMAGE);

        assertThat(product.getDescription()).isEqualTo(
                "Discover a truly immersive viewing experience with this monitor curved more deeply than any other. " +
                        "Wrapping around your field of vision the 1,800 R screencreates a wider field of view, " +
                        "enhances depth perception, and minimises peripheral distractions to draw you deeper " +
                        "in to your content."
        );
        assertThat(product.getEnabled()).isTrue();
        assertThat(product.getFacetValues()).hasSize(2);
        FacetValue facetValue1 = product.getFacetValues().get(0);
        assertThat(facetValue1.getCode()).isEqualTo("electronics");
        assertThat(facetValue1.getId()).isEqualTo(1L);
        assertThat(facetValue1.getName()).isEqualTo("electronics");
        assertThat(facetValue1.getFacet().getId()).isEqualTo(1);
        assertThat(facetValue1.getFacet().getName()).isEqualTo("category");

        FacetValue facetValue2 = product.getFacetValues().get(1);
        assertThat(facetValue2.getCode()).isEqualTo("computers");
        assertThat(facetValue2.getId()).isEqualTo(2L);
        assertThat(facetValue2.getName()).isEqualTo("computers");
        assertThat(facetValue2.getFacet().getId()).isEqualTo(1);
        assertThat(facetValue2.getFacet().getName()).isEqualTo("category");

        Asset featuredAsset = product.getFeaturedAsset();
        assertThat(featuredAsset.getFileSize()).isEqualTo(1680);
        assertThat(featuredAsset.getId()).isEqualTo(2L);
        assertThat(featuredAsset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(featuredAsset.getName()).isEqualTo("alexandru-acea-686569-unsplash.jpg");
        assertThat(featuredAsset.getPreview()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash__preview.jpg");
        assertThat(featuredAsset.getSource()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash.jpg");
        assertThat(featuredAsset.getType()).isEqualTo(AssetType.IMAGE);

        assertThat(product.getId()).isEqualTo(2L);
        assertThat(product.getName()).isEqualTo("Curvy Monitor");
        assertThat(product.getOptionGroups()).hasSize(1);
        ProductOptionGroup productOptionGroup = product.getOptionGroups().get(0);
        assertThat(productOptionGroup.getCode()).isEqualTo("curvy-monitor-monitor-size");
        assertThat(productOptionGroup.getId()).isEqualTo(3L);
        assertThat(productOptionGroup.getName()).isEqualTo("monitor size");

        assertThat(product.getSlug()).isEqualTo("curvy-monitor");
    }

    @Test
    @Order(13)
    public void ProductVariant_price_properties_are_correct() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product.getVariants().get(0).getPrice()).isEqualTo(14374);
    }

    @Test
    @Order(14)
    public void returns_null_when_id_not_found() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 999L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product).isNull();
    }

    @Test
    @Order(15)
    public void productVariant_query_by_id() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_VARIANT, variables);
        ProductVariant productVariant = graphQLResponse.get("$.data.productVariant", ProductVariant.class);
        assertThat(productVariant.getId()).isEqualTo(1L);
        assertThat(productVariant.getName()).isEqualTo("Laptop 13 inch 8GB");
    }

    @Test
    @Order(16)
    public void productVariant_returns_null_when_id_not_found() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 999L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_VARIANT, variables);
        ProductVariant productVariant = graphQLResponse.get("$.data.productVariant", ProductVariant.class);
        assertThat(productVariant).isNull();
    }

    /**
     * product mutation
     */

    Product newProduct;
    Product newProductWithAssets;

    @Test
    @Order(17)
    public void createProduct_creates_a_new_Product() throws IOException {
        CreateProductInput createProductInput = new CreateProductInput();
        createProductInput.setName("en Baked Potato");
        createProductInput.setSlug("en Baked Potato");
        createProductInput.setDescription("A baked potato");

        JsonNode inputNode = objectMapper.valueToTree(createProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        newProduct = graphQLResponse.get("$.data.createProduct", Product.class);

        assertThat(newProduct.getAssets()).isEmpty();
        assertThat(newProduct.getEnabled()).isTrue();
        assertThat(newProduct.getFacetValues()).isEmpty();
        assertThat(newProduct.getFeaturedAsset()).isNull();
        assertThat(newProduct.getId()).isEqualTo(21L);
        assertThat(newProduct.getName()).isEqualTo("en Baked Potato");
        assertThat(newProduct.getSlug()).isEqualTo("en-baked-potato");
        assertThat(newProduct.getVariants()).isEmpty();
        assertThat(newProduct.getDescription()).isEqualTo("A baked potato");
    }

    @Test
    @Order(18)
    public void createProduct_creates_a_new_Product_with_assets() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ASSET_LIST, null, Arrays.asList(ASSET_FRAGMENT));
        AssetList assetList = graphQLResponse.get("$.data.assets", AssetList.class);
        List<Long> assetIds = assetList.getItems().stream()
                .map(Asset::getId).collect(Collectors.toList()).subList(0, 2);
        Long featuredAssetId = assetList.getItems().get(0).getId();

        CreateProductInput createProductInput = new CreateProductInput();
        createProductInput.setAssetIds(assetIds);
        createProductInput.setFeaturedAssetId(featuredAssetId);
        createProductInput.setName("en Has Assets");
        createProductInput.setSlug("en-has-assets");
        createProductInput.setDescription("A product with assets");

        JsonNode inputNode = objectMapper.valueToTree(createProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(CREATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product createdProduct = graphQLResponse.get("$.data.createProduct", Product.class);

        assertThat(createdProduct.getAssets().stream().map(Asset::getId).collect(Collectors.toList()))
                .containsExactly(assetIds.toArray(new Long[0]));
        assertThat(createdProduct.getFeaturedAsset().getId()).isEqualTo(featuredAssetId);
        newProductWithAssets = createdProduct;
    }

    @Test
    @Order(19)
    public void updateProduct_updates_a_Product() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setName("en Mashed Potato");
        updateProductInput.setSlug("en-mashed-potato");
        updateProductInput.setDescription("A blob of mashed potato");

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);
        assertThat(updatedProduct.getDescription()).isEqualTo(
                "A blob of mashed potato"
        );
    }

    @Test
    @Order(20)
    public void slug_is_normalized_to_be_url_safe() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setName("en Mashed Potato");
        updateProductInput.setSlug("A (very) nice potato!!");
        updateProductInput.setDescription("A blob of mashed potato");

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);
        assertThat(updatedProduct.getSlug()).isEqualTo("a-very-nice-potato");
    }

    @Test
    @Order(21)
    public void create_with_duplicate_slug_is_renamed_to_be_unique() throws IOException {
        CreateProductInput createProductInput = new CreateProductInput();
        createProductInput.setName("Another baked potato");
        createProductInput.setSlug("a-very-nice-potato");
        createProductInput.setDescription("Another baked potato but a bit different");

        JsonNode inputNode = objectMapper.valueToTree(createProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product createdProduct = graphQLResponse.get("$.data.createProduct", Product.class);
        assertThat(createdProduct.getSlug()).isEqualTo("a-very-nice-potato-2");
    }

    @Test
    @Order(22)
    public void update_with_duplicate_slug_is_renamed_to_be_unique() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setName("Yet another baked potato");
        updateProductInput.setSlug("a-very-nice-potato-2");
        updateProductInput.setDescription("Possibly the final baked potato");

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);
        assertThat(updatedProduct.getSlug()).isEqualTo("a-very-nice-potato-3");
    }

    @Test
    @Order(23)
    public void slug_duplicate_check_does_not_include_self() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setSlug("a-very-nice-potato-3");

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);
        assertThat(updatedProduct.getSlug()).isEqualTo("a-very-nice-potato-3");
    }

    @Test
    @Order(24)
    public void updateProduct_accepts_partial_input() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setName("en Very Mashed Potato");

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);
        assertThat(updatedProduct.getName()).isEqualTo("en Very Mashed Potato");
        assertThat(updatedProduct.getDescription()).isEqualTo("Possibly the final baked potato");
    }

    @Test
    @Order(25)
    public void updateProduct_adds_Assets_to_a_product_and_sets_featured_asset() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ASSET_LIST, null, Arrays.asList(ASSET_FRAGMENT));
        AssetList assetList = graphQLResponse.get("$.data.assets", AssetList.class);
        List<Long> assetIds = assetList.getItems().stream()
                .map(Asset::getId).collect(Collectors.toList());
        Long featuredAssetId = assetList.getItems().get(2).getId();

        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setAssetIds(assetIds);
        updateProductInput.setFeaturedAssetId(featuredAssetId);

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);

        assertThat(updatedProduct.getAssets().stream().map(Asset::getId).collect(Collectors.toList()))
                .containsExactly(assetIds.toArray(new Long[0]));
        assertThat(updatedProduct.getFeaturedAsset().getId()).isEqualTo(featuredAssetId);
    }

    @Test
    @Order(26)
    public void updateProduct_sets_a_featured_asset() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", newProduct.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);

        List<Asset> assets = product.getAssets();

        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setFeaturedAssetId(assets.get(0).getId());

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);

        assertThat(updatedProduct.getFeaturedAsset().getId()).isEqualTo(assets.get(0).getId());
    }

    @Test
    @Order(27)
    public void updateProduct_updates_assets() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setFeaturedAssetId(1L);
        updateProductInput.setFacetValueIds(new ArrayList<>());
        updateProductInput.setAssetIds(new ArrayList<>());
        updateProductInput.getAssetIds().addAll(Arrays.asList(1L, 2L));

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);

        assertThat(updatedProduct.getAssets().stream().map(Asset::getId).collect(Collectors.toList()))
                .containsExactly(1L, 2L);
    }

    @Test
    @Order(28)
    public void updateProduct_updates_FacetValues() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(newProduct.getId());
        updateProductInput.setFacetValueIds(new ArrayList<>());
        updateProductInput.getFacetValueIds().add(1L);

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product updatedProduct = graphQLResponse.get("$.data.updateProduct", Product.class);

        assertThat(updatedProduct.getFacetValues().stream().map(FacetValue::getId).collect(Collectors.toList()))
                .containsExactly(1L);
    }

    @Test
    @Order(29)
    public void updateProduct_errors_with_an_invalid_productId() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(999L);
        updateProductInput.setName("en Mashed Potato");
        updateProductInput.setSlug("en-mashed-potato");
        updateProductInput.setDescription("A blob of mashed potato");

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(UPDATE_PRODUCT, variables,
                    Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("No ProductEntity with the id '999' could be found");
        }
    }

    @Test
    @Order(30)
    public void addOptionGroupToProduct_adds_an_option_group() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", newProduct.getId());
        variables.put("optionGroupId", 2L);

        GraphQLResponse graphQLResponse = adminClient.perform(ADD_OPTION_GROUP_TO_PRODUCT, variables);
        Product product = graphQLResponse.get("$.data.addOptionGroupToProduct", Product.class);
        assertThat(product.getOptionGroups()).hasSize(1);
        assertThat(product.getOptionGroups().get(0).getId()).isEqualTo(2L);
    }

    @Test
    @Order(31)
    public void addOptionGroupToProduct_errors_with_an_invalid_productId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", 999);
        variables.put("optionGroupId", 1L);

        try {
            adminClient.perform(ADD_OPTION_GROUP_TO_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("No ProductEntity with the id '999' could be found");
        }
    }

    @Test
    @Order(32)
    public void addOptionGroupToProduct_errors_with_an_invalid_optionGroupId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", newProduct.getId());
        variables.put("optionGroupId", 999L);

        try {
            adminClient.perform(ADD_OPTION_GROUP_TO_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No ProductOptionGroupEntity with the id '999' could be found");
        }
    }

    @Test
    @Order(33)
    public void removeOptionGroupFromProduct_removes_an_option_group() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", newProductWithAssets.getId());
        variables.put("optionGroupId", 1L);

        GraphQLResponse graphQLResponse = adminClient.perform(ADD_OPTION_GROUP_TO_PRODUCT, variables);
        Product product = graphQLResponse.get("$.data.addOptionGroupToProduct", Product.class);
        assertThat(product.getOptionGroups()).hasSize(1);

        variables = objectMapper.createObjectNode();
        variables.put("productId", newProductWithAssets.getId());
        variables.put("optionGroupId", 1L);

        graphQLResponse = adminClient.perform(REMOVE_OPTION_GROUP_FROM_PRODUCT, variables);
        product = graphQLResponse.get("$.data.removeOptionGroupFromProduct", Product.class);
        assertThat(product.getId()).isEqualTo(newProductWithAssets.getId());
        assertThat(product.getOptionGroups()).isEmpty();
    }

    @Test
    @Order(34)
    public void removeOptionGroupFromProduct_errors_if_the_optionGroup_is_being_used_by_variants() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", 2L);
        variables.put("optionGroupId", 3L);

        try {
            adminClient.perform(REMOVE_OPTION_GROUP_FROM_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot remove ProductOptionGroup \"{ curvy-monitor-monitor-size }\" as it is used by" +
                            " { 2 } ProductVariant(s)"
            );
        }
    }

    @Test
    @Order(35)
    public void removeOptionGroupFromProduct_errors_with_an_invalid_productId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", 999L);
        variables.put("optionGroupId", 1L);

        try {
            adminClient.perform(REMOVE_OPTION_GROUP_FROM_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("No ProductEntity with the id '999' could be found");
        }
    }

    @Test
    @Order(36)
    public void removeOptionGroupFromProduct_errors_with_an_invalid_optionGroupId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", newProduct.getId());
        variables.put("optionGroupId", 999L);

        try {
            adminClient.perform(REMOVE_OPTION_GROUP_FROM_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No ProductOptionGroupEntity with the id '999' could be found");
        }
    }

    /**
     * variants
     */
    List<ProductVariant> variants;
    ProductOptionGroup optionGroup2;
    ProductOptionGroup optionGroup3;

    private void before_test_37() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", newProduct.getId());
        variables.put("optionGroupId", 3L);

        adminClient.perform(ADD_OPTION_GROUP_TO_PRODUCT, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_OPTION_GROUP, variables);
        optionGroup2 = graphQLResponse.get("$.data.productOptionGroup", ProductOptionGroup.class);

        variables = objectMapper.createObjectNode();
        variables.put("id", 3L);

        graphQLResponse = adminClient.perform(GET_OPTION_GROUP, variables);
        optionGroup3 = graphQLResponse.get("$.data.productOptionGroup", ProductOptionGroup.class);
    }

    @Test
    @Order(37)
    public void createProductVariants_throws_if_optionIds_not_compatible_with_product() throws IOException {
        before_test_37();

        CreateProductVariantInput createProductVariantInput = new CreateProductVariantInput();
        createProductVariantInput.setProductId(newProduct.getId());
        createProductVariantInput.setSku("PV1");
        createProductVariantInput.setName("Variant 1");

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(createProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(CREATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                  "ProductVariant optionIds must include one optionId " +
                          "from each of the groups: { curvy-monitor-monitor-size, laptop-ram }"
            );
        }
    }

    @Test
    @Order(38)
    public void createProductVariants_throws_if_optionIds_are_duplicated() throws IOException {
        CreateProductVariantInput createProductVariantInput = new CreateProductVariantInput();
        createProductVariantInput.setProductId(newProduct.getId());
        createProductVariantInput.setSku("PV1");
        createProductVariantInput.getOptionIds().addAll(
                Arrays.asList(optionGroup2.getOptions().get(0).getId(), optionGroup2.getOptions().get(1).getId()));
        createProductVariantInput.setName("Variant 1");

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(createProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(CREATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "ProductVariant optionIds must include one optionId " +
                            "from each of the groups: { curvy-monitor-monitor-size, laptop-ram }"
            );
        }
    }

    @Test
    @Order(39)
    public void createProductVariants_works() throws IOException {
        CreateProductVariantInput createProductVariantInput = new CreateProductVariantInput();
        createProductVariantInput.setProductId(newProduct.getId());
        createProductVariantInput.setSku("PV1");
        createProductVariantInput.getOptionIds().addAll(
                Arrays.asList(optionGroup2.getOptions().get(0).getId(), optionGroup3.getOptions().get(0).getId()));
        createProductVariantInput.setName("Variant 1");

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(createProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> createdProductVariants =
                graphQLResponse.getList("$.data.createProductVariants", ProductVariant.class);
        assertThat(createdProductVariants.get(0).getName()).isEqualTo("Variant 1");
        assertThat(createdProductVariants.get(0).getOptions().stream().map(ProductOption::getId)
                .collect(Collectors.toList()))
                .containsExactly(optionGroup2.getOptions().get(0).getId(), optionGroup3.getOptions().get(0).getId());

    }

    @Test
    @Order(40)
    public void createProductVariants_adds_multiple_variants_at_once() throws IOException {
        CreateProductVariantInput createProductVariantInput1 = new CreateProductVariantInput();
        createProductVariantInput1.setProductId(newProduct.getId());
        createProductVariantInput1.setSku("PV2");
        createProductVariantInput1.getOptionIds().addAll(
                Arrays.asList(optionGroup2.getOptions().get(1).getId(), optionGroup3.getOptions().get(0).getId()));
        createProductVariantInput1.setName("Variant 2");

        CreateProductVariantInput createProductVariantInput2 = new CreateProductVariantInput();
        createProductVariantInput2.setProductId(newProduct.getId());
        createProductVariantInput2.setSku("PV3");
        createProductVariantInput2.getOptionIds().addAll(
                Arrays.asList(optionGroup2.getOptions().get(1).getId(), optionGroup3.getOptions().get(1).getId()));
        createProductVariantInput2.setName("Variant 3");

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(createProductVariantInput1, createProductVariantInput2));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> createdProductVariants =
                graphQLResponse.getList("$.data.createProductVariants", ProductVariant.class);
        ProductVariant variant2 = createdProductVariants.stream().filter(v -> Objects.equals("Variant 2", v.getName()))
                .findFirst().get();
        ProductVariant variant3 = createdProductVariants.stream().filter(v -> Objects.equals("Variant 3", v.getName()))
                .findFirst().get();
        assertThat(variant2.getOptions().stream().map(ProductOption::getId).collect(Collectors.toList()))
                .containsExactly(optionGroup2.getOptions().get(1).getId(), optionGroup3.getOptions().get(0).getId());
        assertThat(variant3.getOptions().stream().map(ProductOption::getId).collect(Collectors.toList()))
                .containsExactly(optionGroup2.getOptions().get(1).getId(), optionGroup3.getOptions().get(1).getId());

        variants = createdProductVariants.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Test
    @Order(41)
    public void createProductVariants_throws_if_options_combination_already_exists() throws IOException {
        CreateProductVariantInput createProductVariantInput = new CreateProductVariantInput();
        createProductVariantInput.setProductId(newProduct.getId());
        createProductVariantInput.setSku("PV2");
        createProductVariantInput.getOptionIds().addAll(
                Arrays.asList(optionGroup2.getOptions().get(0).getId(), optionGroup3.getOptions().get(0).getId()));
        createProductVariantInput.setName("Variant 2");

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(createProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(CREATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "A ProductVariant already exists with the options: { 24-inch, 8gb }"
            );
        }
    }

    @Test
    @Order(42)
    public void updateProductVariants_updates_variants() throws IOException {
        ProductVariant firstVariant = variants.get(0);

        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(firstVariant.getId());
        updateProductVariantInput.setSku("ABC");
        updateProductVariantInput.setPrice(432);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);
        ProductVariant updatedVariant = updatedProductVariants.get(0);
        assertThat(updatedVariant.getSku()).isEqualTo("ABC");
        assertThat(updatedVariant.getPrice()).isEqualTo(432);
    }

    @Test
    @Order(43)
    public void updateProductVariants_updates_assets() throws IOException {
        ProductVariant firstVariant = variants.get(0);

        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(firstVariant.getId());
        updateProductVariantInput.getAssetIds().addAll(Arrays.asList(1L, 2L));
        updateProductVariantInput.setFeaturedAssetId(2L);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);
        ProductVariant updatedVariant = updatedProductVariants.get(0);
        assertThat(updatedVariant.getAssets().stream().map(Asset::getId).collect(Collectors.toList()))
                .containsExactly(1L, 2L);
        assertThat(updatedVariant.getFeaturedAsset().getId()).isEqualTo(2L);
    }

    @Test
    @Order(44)
    public void updateProductVariants_updates_assets_again() throws IOException {
        ProductVariant firstVariant = variants.get(0);

        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(firstVariant.getId());
        updateProductVariantInput.getAssetIds().addAll(Arrays.asList(4L, 3L));
        updateProductVariantInput.setFeaturedAssetId(4L);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);
        ProductVariant updatedVariant = updatedProductVariants.get(0);
        assertThat(updatedVariant.getAssets().stream().map(Asset::getId).collect(Collectors.toList()))
                .containsExactly(3L, 4L);
        assertThat(updatedVariant.getFeaturedAsset().getId()).isEqualTo(4L);
    }

    @Test
    @Order(45)
    public void updateProductVariants_updates_facetValues() throws IOException {
        ProductVariant firstVariant = variants.get(0);

        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(firstVariant.getId());
        updateProductVariantInput.getFacetValueIds().add(1L);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);
        ProductVariant updatedVariant = updatedProductVariants.get(0);
        assertThat(updatedVariant.getFacetValues()).hasSize(1);
        assertThat(updatedVariant.getFacetValues().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @Order(46)
    public void updateProductVariants_throws_with_an_invalid_variant_id() throws IOException {
        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(999L);
        updateProductVariantInput.setSku("ABC");
        updateProductVariantInput.setPrice(432);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No ProductVariantEntity with the id '999' could be found"
            );
        }
    }

    ProductVariant deletedVariant;

    @Test
    @Order(47)
    public void deleteProductVariant() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", newProduct.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product1 = graphQLResponse.get("$.data.adminProduct", Product.class);
        List<Long> sortedVariantIds = product1.getVariants().stream()
                .map(ProductVariant::getId).sorted().collect(Collectors.toList());
        assertThat(sortedVariantIds).containsExactly(35L, 36L, 37L);

        variables = objectMapper.createObjectNode();
        variables.put("id", sortedVariantIds.get(0));
        graphQLResponse = adminClient.perform(DELETE_PRODUCT_VARIANT, variables);
        DeletionResponse deletionResponse =
                graphQLResponse.get("$.data.deleteProductVariant", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        variables = objectMapper.createObjectNode();
        variables.put("id", newProduct.getId());

        graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product2 = graphQLResponse.get("$.data.adminProduct", Product.class);
        sortedVariantIds = product2.getVariants().stream()
                .map(ProductVariant::getId).sorted().collect(Collectors.toList());
        assertThat(sortedVariantIds).containsExactly(36L, 37L);

        deletedVariant = product1.getVariants().stream().filter(v -> Objects.equals(v.getId(), 35L))
                .findFirst().get();
    }

    @Test
    @Order(48)
    public void createProductVariants_ignores_deleted_variants_when_checking_for_existing_combinations()
            throws IOException {
        CreateProductVariantInput createProductVariantInput = new CreateProductVariantInput();
        createProductVariantInput.setProductId(newProduct.getId());
        createProductVariantInput.setSku("RE1");
        createProductVariantInput.getOptionIds().addAll(
                Arrays.asList(deletedVariant.getOptions().get(0).getId(), deletedVariant.getOptions().get(1).getId()));
        createProductVariantInput.setName("Re-created Variant");

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(createProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> createdProductVariants =
                graphQLResponse.getList("$.data.createProductVariants", ProductVariant.class);
        assertThat(createdProductVariants).hasSize(1);
        assertThat(createdProductVariants.get(0).getOptions().stream().map(
                o -> o.getCode()).collect(Collectors.toList())).containsExactly(deletedVariant.getOptions()
                        .stream().map(o-> o.getCode()).collect(Collectors.toList()).toArray(new String[0]));
    }

    /**
     * deletion
     */
    List<Product> allProducts;
    Product productToDelete;

    private void before_test_49() throws IOException {
        ProductListOptions options = new ProductListOptions();

        ProductSortParameter sortParameter = new ProductSortParameter();
        sortParameter.setId(SortOrder.ASC);

        options.setSort(sortParameter);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, variables);

        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        allProducts = productList.getItems();
    }

    @Test
    @Order(49)
    public void deletes_a_product() throws IOException {
        before_test_49();

        productToDelete = allProducts.get(0);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", productToDelete.getId());
        GraphQLResponse graphQLResponse = adminClient.perform(DELETE_PRODUCT, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteProduct", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);
    }

    @Test
    @Order(50)
    public void cannot_get_a_deleted_product() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", productToDelete.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product).isNull();
    }

    @Test
    @Order(51)
    public void deleted_product_omitted_from_lsit() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_LIST, null);

        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);

        assertThat(productList.getItems()).hasSize(allProducts.size() - 1);
        assertThat(productList.getItems().stream()
                .map(c -> c.getId()).anyMatch(id -> Objects.equals(id, productToDelete.getId()))).isFalse();
    }

    @Test
    @Order(52)
    public void updateProduct_throws_for_deleted_product() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(productToDelete.getId());
        updateProductInput.setFacetValueIds(new ArrayList<>());
        updateProductInput.getFacetValueIds().add(1L);

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

       try {
           adminClient.perform(UPDATE_PRODUCT, variables,
                   Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
       } catch (ApiException apiEx) {
           assertThat(apiEx.getErrorMessage()).isEqualTo(
                  "No ProductEntity with the id '1' could be found"
           );
       }
    }

    @Test
    @Order(53)
    public void addOptionGroupToProduct_throws_for_deleted_product() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", productToDelete.getId());
        variables.put("optionGroupId", 1L);

        try{
            adminClient.perform(ADD_OPTION_GROUP_TO_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                 "No ProductEntity with the id '1' could be found"
            );
        }
    }

    @Test
    @Order(54)
    public void removeOptionGroupFromProduct_throws_for_deleted_product() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productId", productToDelete.getId());
        variables.put("optionGroupId", 1L);

        try{
            adminClient.perform(REMOVE_OPTION_GROUP_FROM_PRODUCT, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No ProductEntity with the id '1' could be found"
            );
        }
    }
}
