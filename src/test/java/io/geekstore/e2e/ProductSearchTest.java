/*
 * Copyright (c) 2021 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.ApiClient;
import io.geekstore.GeekStoreGraphQLTest;
import io.geekstore.MockDataService;
import io.geekstore.PopulateOptions;
import io.geekstore.config.TestConfig;
import io.geekstore.config.collection.CollectionFilter;
import io.geekstore.types.asset.Coordinate;
import io.geekstore.types.asset.CoordinateInput;
import io.geekstore.types.asset.UpdateAssetInput;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.collection.CreateCollectionInput;
import io.geekstore.types.collection.UpdateCollectionInput;
import io.geekstore.types.common.*;
import io.geekstore.types.facet.CreateFacetInput;
import io.geekstore.types.facet.CreateFacetValueWithFacetInput;
import io.geekstore.types.facet.Facet;
import io.geekstore.types.product.UpdateProductInput;
import io.geekstore.types.product.UpdateProductVariantInput;
import io.geekstore.types.search.FacetValueResult;
import io.geekstore.types.search.SearchResponse;
import io.geekstore.types.search.SearchResult;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Jan, 2021 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ProductSearchTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String CREATE_FACET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_facet");
    static final String FACET_WITH_VALUES_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_with_values_fragment");
    static final String FACET_VALUE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_value_fragment");
    static final String UPDATE_PRODUCT_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product_variants");
    static final String DELETE_PRODUCT_VARIANT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "delete_product_variant");
    static final String DELETE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "delete_product");
    static final String COLLECTION_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "collection_fragment");
    static final String CONFIGURABLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "configurable_fragment");
    static final String UPDATE_COLLECTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_collection");
    static final String CREATE_COLLECTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_collection");
    public static final String UPDATE_ASSET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_asset");

    static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    static final String UPDATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product");

    static final String SHOP_SEARCH_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/search/%s.graphqls";
    static final String SEARCH_PRODUCTS_SHOP =
            String.format(SHOP_SEARCH_GRAPHQL_RESOURCE_TEMPLATE, "search_products_shop");
    static final String SEARCH_GET_FACET_VALUES_BY_SHOP =
            String.format(SHOP_SEARCH_GRAPHQL_RESOURCE_TEMPLATE, "search_get_facet_values");

    static final String ADMIN_SEARCH_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/search/%s.graphqls";
    static final String SEARCH_PRODUCTS_ADMIN =
            String.format(ADMIN_SEARCH_GRAPHQL_RESOURCE_TEMPLATE, "search_products_admin");
    static final String SEARCH_GET_ASSETS =
            String.format(ADMIN_SEARCH_GRAPHQL_RESOURCE_TEMPLATE, "search_get_assets");
    public static final String SEARCH_GET_PRICES =
            String.format(ADMIN_SEARCH_GRAPHQL_RESOURCE_TEMPLATE, "search_get_prices");
    public static final String REINDEX =
            String.format(ADMIN_SEARCH_GRAPHQL_RESOURCE_TEMPLATE, "reindex");

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

    @Autowired
    @Qualifier("facetValueCollectionFilter")
    CollectionFilter facetValueCollectionFilter;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    void testTotalItems(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getTotalItems()).isEqualTo(34);
    }

    void testMatchSearchTerm(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();
        input.setTerm("camera");
        SearchResultSortParameter sortParameter = new SearchResultSortParameter();
        sortParameter.setName(SortOrder.ASC);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Camera Lens", "Instant Camera", "Slr Camera");
    }

    void testMatchFacetIdsAnd(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();
        input.getFacetValueIds().addAll(Arrays.asList(1L, 2L));
        input.setFacetValueOperator(LogicalOperator.AND);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Laptop",
                        "Curvy Monitor",
                        "Gaming PC",
                        "Hard Drive",
                        "Clacky Keyboard",
                        "USB Cable");
    }

    void testMatchFacetIdsOr(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();
        input.getFacetValueIds().addAll(Arrays.asList(1L, 5L));
        input.setPageSize(100);
        input.setFacetValueOperator(LogicalOperator.OR);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Laptop",
                        "Curvy Monitor",
                        "Gaming PC",
                        "Hard Drive",
                        "Clacky Keyboard",
                        "USB Cable",
                        "Instant Camera",
                        "Camera Lens",
                        "Tripod",
                        "Slr Camera",
                        "Spiky Cactus",
                        "Orchid",
                        "Bonsai Tree"
                );
    }


    void testMatchCollectionId(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();
        input.setCollectionId(2L);
        input.setPageSize(100);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Spiky Cactus",
                        "Orchid",
                        "Bonsai Tree"
                );
    }

    void testMatchCollectionSlug(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();
        input.setCollectionSlug("plants");
        input.setPageSize(100);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Spiky Cactus",
                        "Orchid",
                        "Bonsai Tree"
                );
    }

    void testPrices(ApiClient client) throws IOException {
        SearchInput input = new SearchInput();
        input.setPageSize(3);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getPrice()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        129900, 139900, 219900
                );
    }

    void testSorting(ApiClient client, String sortBy) throws IOException {
        SearchInput input = new SearchInput();
        SearchResultSortParameter sortParameter = new SearchResultSortParameter();
        if ("name".equals(sortBy)) {
            sortParameter.setName(SortOrder.DESC);
        } else {
            sortParameter.setPrice(SortOrder.DESC);
        }
        input.setSort(sortParameter);
        input.setPageSize(3);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        boolean admin = client == adminClient;
        GraphQLResponse graphQLResponse =
                client.perform(admin ? SEARCH_PRODUCTS_ADMIN : SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse =
                graphQLResponse.get(admin ? "$.data.searchByAdmin" : "$.data.search", SearchResponse.class);

        List<String> variantNames =
                searchResponse.getItems().stream().map(i -> i.getProductVariantName()).collect(Collectors.toList());
        if ("name".equals(sortBy)) {
            assertThat(variantNames).containsExactlyInAnyOrder("USB Cable", "Tripod", "Tent");
        } else {
            assertThat(variantNames)
                    .containsExactlyInAnyOrder("Road Bike", "Laptop 15 inch 16GB", "Laptop 13 inch 16GB");
        }
    }


    /**
     * shop api
     */
    @Test
    @Order(1)
    public void total_items_by_shop() throws IOException {
        testTotalItems(shopClient);
    }

    @Test
    @Order(2)
    public void matches_search_term_by_shop() throws IOException {
        testMatchSearchTerm(shopClient);
    }

    @Test
    @Order(3)
    public void matches_by_facetId_with_AND_operator_by_shop() throws IOException {
        testMatchFacetIdsAnd(shopClient);
    }

    @Test
    @Order(4)
    public void matches_by_facetId_with_OR_operator_by_shop() throws IOException {
        testMatchFacetIdsOr(shopClient);
    }

    @Test
    @Order(5)
    public void matches_by_collectionId_by_shop() throws IOException {
        testMatchCollectionId(shopClient);
    }

    @Test
    @Order(6)
    public void matches_by_collectionSlug_by_shop() throws IOException {
        testMatchCollectionSlug(shopClient);
    }

    @Test
    @Order(7)
    public void prices_by_shop() throws IOException {
        testPrices(shopClient);
    }

    @Test
    @Order(8)
    public void returns_correct_facetValues_by_shop() throws IOException {
        SearchInput input = new SearchInput();

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SEARCH_GET_FACET_VALUES_BY_SHOP, variables);
        SearchResponse searchResponse = graphQLResponse.get("$.data.search", SearchResponse.class);

        assertThat(searchResponse.getFacetValues()).hasSize(6);

        FacetValueResult facetValueResult = searchResponse.getFacetValues().get(0);
        assertThat(facetValueResult.getCount()).isEqualTo(21);
        assertThat(facetValueResult.getFacetValue().getId()).isEqualTo(1L);
        assertThat(facetValueResult.getFacetValue().getName()).isEqualTo("electronics");

        facetValueResult = searchResponse.getFacetValues().get(1);
        assertThat(facetValueResult.getCount()).isEqualTo(17);
        assertThat(facetValueResult.getFacetValue().getId()).isEqualTo(2L);
        assertThat(facetValueResult.getFacetValue().getName()).isEqualTo("computers");

        facetValueResult = searchResponse.getFacetValues().get(2);
        assertThat(facetValueResult.getCount()).isEqualTo(4);
        assertThat(facetValueResult.getFacetValue().getId()).isEqualTo(3L);
        assertThat(facetValueResult.getFacetValue().getName()).isEqualTo("photo");

        facetValueResult = searchResponse.getFacetValues().get(3);
        assertThat(facetValueResult.getCount()).isEqualTo(10);
        assertThat(facetValueResult.getFacetValue().getId()).isEqualTo(4L);
        assertThat(facetValueResult.getFacetValue().getName()).isEqualTo("sports equipment");

        facetValueResult = searchResponse.getFacetValues().get(4);
        assertThat(facetValueResult.getCount()).isEqualTo(3);
        assertThat(facetValueResult.getFacetValue().getId()).isEqualTo(5L);
        assertThat(facetValueResult.getFacetValue().getName()).isEqualTo("home & garden");

        facetValueResult = searchResponse.getFacetValues().get(5);
        assertThat(facetValueResult.getCount()).isEqualTo(3);
        assertThat(facetValueResult.getFacetValue().getId()).isEqualTo(6L);
        assertThat(facetValueResult.getFacetValue().getName()).isEqualTo("plants");
    }

    @Test
    @Order(9)
    public void omits_facetValues_of_private_facets_by_shop() throws IOException {
        CreateFacetInput input = new CreateFacetInput();
        input.setCode("profit-margin");
        input.setPrivateOnly(true);
        input.setName("Profit Margin");
        CreateFacetValueWithFacetInput createFacetValueWithFacetInput = new CreateFacetValueWithFacetInput();
        createFacetValueWithFacetInput.setCode("massive");
        createFacetValueWithFacetInput.setName("massive");
        input.getValues().add(createFacetValueWithFacetInput);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(CREATE_FACET, variables, Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet createdFacet = graphQLResponse.get("$.data.createFacet", Facet.class);

        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(2L);
        updateProductInput.setFacetValueIds(new ArrayList<>());
        // 1 & 2 are the existing facetValues (electronics & photo)
        updateProductInput.getFacetValueIds().addAll(Arrays.asList(1L, 2L, createdFacet.getValues().get(0).getId()));

        inputNode = objectMapper.valueToTree(updateProductInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT, variables,
                        Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        testHelper.awaitRunningTasks();

        returns_correct_facetValues_by_shop();
    }

    @Test
    @Order(10)
    public void encodes_the_productId_and_productVariantId_by_shop() throws IOException {
        SearchInput input = new SearchInput();
        input.setPageSize(1);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse = graphQLResponse.get("$.data.search", SearchResponse.class);

        SearchResult searchResult = searchResponse.getItems().get(0);
        assertThat(searchResult.getProductId()).isEqualTo(1);
        assertThat(searchResult.getProductVariantId()).isEqualTo(1);
    }


    @Test
    @Order(11)
    public void sort_name_by_shop() throws IOException {
        testSorting(shopClient, "name");
    }

    @Test
    @Order(12)
    public void sort_price_by_shop() throws IOException {
        testSorting(shopClient, "price");
    }

    @Test
    @Order(13)
    public void omits_results_for_disabled_ProductVariants_by_shop() throws IOException {
        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(3L);
        input.setEnabled(false);

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(input));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables, Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        testHelper.awaitRunningTasks();

        SearchInput searchInput = new SearchInput();
        searchInput.setPageSize(3);

        inputNode = objectMapper.valueToTree(searchInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse = graphQLResponse.get("$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductVariantId()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L, 2L, 4L);
    }

    @Test
    @Order(14)
    public void encodes_collectionIds_by_shop() throws IOException {
        SearchInput input = new SearchInput();
        input.setPageSize(1);
        input.setTerm("cactus");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SEARCH_PRODUCTS_SHOP, variables);
        SearchResponse searchResponse = graphQLResponse.get("$.data.search", SearchResponse.class);

        assertThat(searchResponse.getItems().get(0).getCollectionIds()).containsExactly(2L);
    }

//    void before_test_15() throws IOException {
//        UpdateProductVariantInput input = new UpdateProductVariantInput();
//        input.setId(3L);
//        input.setEnabled(true); // 恢复成true
//
//        JsonNode inputNode = objectMapper.valueToTree(input);
//        ObjectNode variables = objectMapper.createObjectNode();
//        variables.set("input", inputNode);
//
//        adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables, Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
//
//        UpdateProductInput updateProductInput = new UpdateProductInput();
//        updateProductInput.setId(2L);
//        updateProductInput.setFacetValueIds(new ArrayList<>());
//        // 1 & 2 are the existing facetValues (electronics & photo)
//        updateProductInput.getFacetValueIds().addAll(Arrays.asList(1L, 2L));
//
//        inputNode = objectMapper.valueToTree(updateProductInput);
//        variables = objectMapper.createObjectNode();
//        variables.set("input", inputNode);
//
//        adminClient.perform(UPDATE_PRODUCT, variables,
//                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
//
//        testHelper.awaitRunningTasks();
//    }

    /**
     * admin api
     */
    @Test
    @Order(15)
    public void total_items_by_admin() throws IOException {
        testTotalItems(adminClient);
    }

    @Test
    @Order(16)
    public void matches_search_term_by_admin() throws IOException {
        testMatchSearchTerm(adminClient);
    }

    @Test
    @Order(17)
    public void matches_by_facetId_with_AND_operator_by_admin() throws IOException {
        testMatchFacetIdsAnd(adminClient);
    }

    @Test
    @Order(18)
    public void matches_by_facetId_with_OR_operator_by_admin() throws IOException {
        testMatchFacetIdsOr(adminClient);
    }

    @Test
    @Order(19)
    public void matches_by_collectionId_by_admin() throws IOException {
        testMatchCollectionId(adminClient);
    }

    @Test
    @Order(20)
    public void matches_by_collectionSlug_by_admin() throws IOException {
        testMatchCollectionId(adminClient);
    }

    @Test
    @Order(21)
    public void prices_by_admin() throws IOException {
        testPrices(adminClient);
    }

    @Test
    @Order(22)
    public void sort_name_by_admin() throws IOException {
        testSorting(adminClient, "name");
    }

    @Test
    @Order(23)
    public void sort_price_by_admin() throws IOException {
        testSorting(adminClient, "price");
    }

    /**
     * updating the index
     */

    SearchResponse doAdminSearchQuery(SearchInput input) throws IOException {
        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(SEARCH_PRODUCTS_ADMIN, variables);
        SearchResponse searchResponse =
                graphQLResponse.get("$.data.searchByAdmin", SearchResponse.class);

        return searchResponse;
    }

    @Test
    @Order(24)
    public void updates_index_when_ProductVariants_are_changed() throws IOException {
        testHelper.awaitRunningTasks();

        SearchInput input = new SearchInput();
        input.setTerm("drive");

        SearchResponse searchResponse = doAdminSearchQuery(input);

        assertThat(searchResponse.getItems().stream().map(i -> i.getSku()).collect(Collectors.toList()))
                .containsExactly(
                        "IHD455T1",
                        "IHD455T2",
                        "IHD455T3",
                        "IHD455T4",
                        "IHD455T6"
                );

        List<UpdateProductVariantInput> inputs = new ArrayList<>();
        searchResponse.getItems().stream().forEach(i -> {
            UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
            updateProductVariantInput.setId(i.getProductVariantId());
            updateProductVariantInput.setSku(i.getSku() + "_updated");
            inputs.add(updateProductVariantInput);
        });

        JsonNode inputNode = objectMapper.valueToTree(inputs);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables, Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        testHelper.awaitRunningTasks();

        input = new SearchInput();
        input.setTerm("drive");

        searchResponse = doAdminSearchQuery(input);

        assertThat(searchResponse.getItems().stream().map(i -> i.getSku()).collect(Collectors.toList()))
                .containsExactly(
                        "IHD455T1_updated",
                        "IHD455T2_updated",
                        "IHD455T3_updated",
                        "IHD455T4_updated",
                        "IHD455T6_updated"
                );
    }

    @Test
    @Order(25)
    public void updates_index_when_ProductVariants_are_deleted() throws IOException {
        testHelper.awaitRunningTasks();

        SearchInput input = new SearchInput();
        input.setTerm("drive");

        SearchResponse searchResponse = doAdminSearchQuery(input);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", searchResponse.getItems().get(0).getProductVariantId());

        adminClient.perform(DELETE_PRODUCT_VARIANT, variables);

        testHelper.awaitRunningTasks();

        input = new SearchInput();
        input.setTerm("drive");

        searchResponse = doAdminSearchQuery(input);

        assertThat(searchResponse.getItems().stream().map(i -> i.getSku()).collect(Collectors.toList()))
                .containsExactly(
                        "IHD455T2_updated",
                        "IHD455T3_updated",
                        "IHD455T4_updated",
                        "IHD455T6_updated"
                );
    }

    @Test
    @Order(26)
    public void updates_index_when_a_Product_is_changed() throws IOException {
        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(1L);
        updateProductInput.setFacetValueIds(new ArrayList<>()); // 清除掉现有的facetValueIds

        ObjectNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        testHelper.awaitRunningTasks();

        SearchInput input = new SearchInput();
        input.getFacetValueIds().add(2L);

        SearchResponse searchResponse = doAdminSearchQuery(input);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactly(
                        "Curvy Monitor",
                        "Gaming PC",
                        "Hard Drive",
                        "Clacky Keyboard",
                        "USB Cable"
                );
    }

    @Test
    @Order(27)
    public void updates_index_when_a_Product_is_deleted() throws IOException {
        SearchInput input = new SearchInput();
        input.getFacetValueIds().add(2L);

        SearchResponse searchResponse = doAdminSearchQuery(input);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductId()).collect(Collectors.toSet()))
                .containsExactly(2L, 3L, 4L, 5L, 6L);


        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 5L);

        adminClient.perform(DELETE_PRODUCT, variables);
        testHelper.awaitRunningTasks();

        input = new SearchInput();
        input.getFacetValueIds().add(2L);

        searchResponse = doAdminSearchQuery(input);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductId()).collect(Collectors.toSet()))
                .containsExactly(2L, 3L, 4L, 6L);
    }

    @Test
    @Order(28)
    public void updates_index_when_a_Collection_is_changed() throws IOException {
        UpdateCollectionInput input = new UpdateCollectionInput();
        input.setId(2L);

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());
        ConfigArgInput configArgInput1 = new ConfigArgInput();
        configArgInput1.setName("facetValueIds");
        configArgInput1.setValue("[4]");
        configurableOperationInput.getArguments().add(configArgInput1);
        ConfigArgInput configArgInput2 = new ConfigArgInput();
        configArgInput2.setName("containsAny");
        configArgInput2.setValue("false");
        configurableOperationInput.getArguments().add(configArgInput2);
        input.getFilters().add(configurableOperationInput);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        adminClient.perform(UPDATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        testHelper.awaitRunningTasks();
        // add an additional check for the collection filters to update
        testHelper.awaitRunningTasks();

        SearchInput searchInput = new SearchInput();
        searchInput.setCollectionId(2L);

        SearchResponse searchResponse = doAdminSearchQuery(searchInput);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Road Bike",
                        "Skipping Rope",
                        "Boxing Gloves",
                        "Tent",
                        "Cruiser Skateboard",
                        "Football",
                        "Running Shoe"
                );

        searchInput = new SearchInput();
        searchInput.setCollectionSlug("plants");

        searchResponse = doAdminSearchQuery(searchInput);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Road Bike",
                        "Skipping Rope",
                        "Boxing Gloves",
                        "Tent",
                        "Cruiser Skateboard",
                        "Football",
                        "Running Shoe"
                );
    }

    @Test
    @Order(29)
    public void updates_index_when_a_Collection_created() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        createCollectionInput.setName("Photo");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("photo");

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[3]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("false");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection createdCollection = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();
        // add an additional check for the collection filters to update
        testHelper.awaitRunningTasks();

        SearchInput searchInput = new SearchInput();
        searchInput.setCollectionId(createdCollection.getId());

        SearchResponse searchResponse = doAdminSearchQuery(searchInput);

        assertThat(searchResponse.getItems().stream().map(i -> i.getProductName()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        "Instant Camera",
                        "Camera Lens",
                        "Tripod",
                        "Slr Camera"
                );
    }

    /**
     * asset changes
     */
    SearchResponse searchForLaptop() throws IOException {
        SearchInput input = new SearchInput();
        input.setTerm("laptop");
        input.setPageSize(1);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(SEARCH_GET_ASSETS, variables);
        SearchResponse searchResponse =
                graphQLResponse.get("$.data.searchByAdmin", SearchResponse.class);

        return searchResponse;
    }

    @Test
    @Order(30)
    public void updates_index_when_asset_focalPoint_is_changed() throws IOException {
        SearchResponse search1 = searchForLaptop();

        assertThat(search1.getItems().get(0).getProductAsset().getId()).isEqualTo(1L);
        assertThat(search1.getItems().get(0).getProductAsset().getFocalPoint()).isNull();

        UpdateAssetInput input = new UpdateAssetInput();
        input.setId(1L);

        CoordinateInput coordinateInput = new CoordinateInput();
        coordinateInput.setX(0.42F);
        coordinateInput.setY(0.42F);
        input.setFocalPoint(coordinateInput);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_ASSET, variables, Arrays.asList(ASSET_FRAGMENT));

        testHelper.awaitRunningTasks();

        SearchResponse search2 = searchForLaptop();
        assertThat(search2.getItems().get(0).getProductAsset().getId()).isEqualTo(1L);
        Coordinate focalPoint = search2.getItems().get(0).getProductAsset().getFocalPoint();
        assertThat(focalPoint.getX()).isEqualTo(0.42F);
        assertThat(focalPoint.getY()).isEqualTo(0.42F);
    }

    @Test
    @Order(31)
    public void does_not_include_deleted_ProductVariants_in_index() throws IOException {
        SearchInput input = new SearchInput();
        input.setTerm("hard drive");

        SearchResponse s1 = doAdminSearchQuery(input);
        assertThat(s1.getItems().stream().map(SearchResult::getPrice).collect(Collectors.toSet()))
                .hasSize(4);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", s1.getItems().get(0).getProductVariantId());
        Integer price = s1.getItems().get(0).getPrice();

        adminClient.perform(DELETE_PRODUCT_VARIANT, variables);

        testHelper.awaitRunningTasks();

        input = new SearchInput();
        input.setTerm("hard drive");

        ObjectNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(SEARCH_GET_PRICES, variables);
        SearchResponse search =
                graphQLResponse.get("$.data.searchByAdmin", SearchResponse.class);
        assertThat(search.getItems().stream().map(SearchResult::getPrice).collect(Collectors.toSet()))
                .hasSize(3);
        assertThat(search.getItems().stream().map(SearchResult::getPrice).collect(Collectors.toSet()))
                .doesNotContain(price);
    }

    @Test
    @Order(32)
    public void returns_enabled_field() throws IOException {
        SearchInput input = new SearchInput();
        input.setPageSize(3);

        SearchResponse searchResponse = doAdminSearchQuery(input);
        assertThat(searchResponse.getItems()).hasSize(3);

        SearchResult searchResult1 = searchResponse.getItems().get(0);
        assertThat(searchResult1.getProductVariantId()).isEqualTo(1L);
        assertThat(searchResult1.getEnabled()).isTrue();

        SearchResult searchResult2 = searchResponse.getItems().get(1);
        assertThat(searchResult2.getProductVariantId()).isEqualTo(2L);
        assertThat(searchResult2.getEnabled()).isTrue();

        SearchResult searchResult3 = searchResponse.getItems().get(2);
        assertThat(searchResult3.getProductVariantId()).isEqualTo(3L);
        assertThat(searchResult3.getEnabled()).isFalse();
    }

    @Test
    @Order(33)
    public void enabled_status_survives_reindex() throws IOException {
        adminClient.perform(REINDEX, null);

        testHelper.awaitRunningTasks();

        SearchInput input = new SearchInput();
        input.setPageSize(3);

        SearchResponse searchResponse = doAdminSearchQuery(input);
        assertThat(searchResponse.getItems()).hasSize(3);

        SearchResult searchResult1 = searchResponse.getItems().get(0);
        assertThat(searchResult1.getProductVariantId()).isEqualTo(1L);
        assertThat(searchResult1.getEnabled()).isTrue();

        SearchResult searchResult2 = searchResponse.getItems().get(1);
        assertThat(searchResult2.getProductVariantId()).isEqualTo(2L);
        assertThat(searchResult2.getEnabled()).isTrue();

        SearchResult searchResult3 = searchResponse.getItems().get(2);
        assertThat(searchResult3.getProductVariantId()).isEqualTo(3L);
        assertThat(searchResult3.getEnabled()).isFalse();
    }
}
