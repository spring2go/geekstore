/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.config.collection.CollectionFilter;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.collection.CollectionList;
import io.geekstore.types.collection.CreateCollectionInput;
import io.geekstore.types.collection.UpdateCollectionInput;
import io.geekstore.types.common.ConfigArgInput;
import io.geekstore.types.common.ConfigurableOperationInput;
import io.geekstore.types.facet.*;
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
public class ShopCatalogTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_PRODUCT_WITH_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_with_variants");
    static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    static final String UPDATE_PRODUCT_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product_variants");
    static final String CREATE_FACET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_facet");
    static final String FACET_WITH_VALUES_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_with_values_fragment");
    static final String FACET_VALUE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_value_fragment");
    static final String UPDATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product");
    static final String GET_FACET_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_facet_list");
    static final String CREATE_COLLECTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_collection");
    static final String COLLECTION_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "collection_fragment");
    static final String CONFIGURABLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "configurable_fragment");
    static final String UPDATE_COLLECTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_collection");

    static final String SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/catalog/%s.graphqls";
    static final String DISABLE_PRODUCT  =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "disable_product");
    static final String GET_PRODUCTS  =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_products");
    static final String GET_PRODUCT_SIMPLE =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_product_simple");
    static final String GET_PRODUCT_TO_VARIANTS =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_product_to_variants");
    static final String GET_PRODUCT_FACET_VALUES =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_product_facet_values");
    static final String GET_PRODUCT_VARIANT_FACET_VALUES =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_product_variant_facet_values");
    static final String GET_COLLECTION_VARIANTS =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_collection_variants");
    static final String GET_COLLECTION_LIST =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_collection_list");
    static final String GET_PRODUCT_COLLECTION =
            String.format(SHOP_CATALOG_GRAPHQL_RESOURCE_TEMPLATE, "get_product_collection");

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

    /**
     * products
     */
    private void before_test_1() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        adminClient.perform(DISABLE_PRODUCT, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product monitorProduct = graphQLResponse.get("$.data.adminProduct", Product.class);

        if (monitorProduct != null) {
            UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
            updateProductVariantInput.setId(monitorProduct.getVariants().get(0).getId());
            updateProductVariantInput.setEnabled(false);

            JsonNode inputNode = objectMapper.valueToTree(
                    Arrays.asList(updateProductVariantInput));
            variables = objectMapper.createObjectNode();
            variables.set("input", inputNode);

            this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                    Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        }
    }


    @Test
    @Order(1)
    public void products_list_omits_disabled_products() throws IOException {
        before_test_1();

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCTS, null);
        ProductList productList = graphQLResponse.get("$.data.products", ProductList.class);
        assertThat(productList.getItems().stream().map(Product::getId)).containsExactly(
                2L, 3L, 4L
        );
    }

    @Test
    @Order(2)
    public void query_by_id() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_SIMPLE, variables);
        Product product = graphQLResponse.get("$.data.product", Product.class);
        assertThat(product.getId()).isEqualTo(2L);
    }

    @Test
    @Order(3)
    public void query_by_slug() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("slug", "curvy-monitor");

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_SIMPLE, variables);
        Product product = graphQLResponse.get("$.data.product", Product.class);
        assertThat(product.getSlug()).isEqualTo("curvy-monitor");
    }

    @Test
    @Order(4)
    public void throws_if_neither_id_nor_slug_provided() throws IOException {
        try {
            shopClient.perform(GET_PRODUCT_SIMPLE, null);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Either the Product id or slug must be provided");
        }
    }

    @Test
    @Order(5)
    public void product_returns_null_for_disabled_product() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_SIMPLE, variables);
        Product product = graphQLResponse.get("$.data.product", Product.class);
        assertThat(product).isNull();
    }

    @Test
    @Order(6)
    public void omits_disabled_variants_from_product_response() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_TO_VARIANTS, variables);
        Product product = graphQLResponse.get("$.data.product", Product.class);
        assertThat(product.getVariants()).hasSize(1);
        assertThat(product.getVariants().get(0).getId()).isEqualTo(6L);
        assertThat(product.getVariants().get(0).getName()).isEqualTo("Curvy Monitor 27 inch");
    }

    /**
     * facets
     */

    FacetValue facetValue;

    private void before_test_7() throws IOException {
        CreateFacetInput createFacetInput = new CreateFacetInput();
        createFacetInput.setCode("profit-margin");
        createFacetInput.setPrivateOnly(true);
        createFacetInput.setName("Profit Margin");

        CreateFacetValueWithFacetInput createFacetValueWithFacetInput = new CreateFacetValueWithFacetInput();
        createFacetValueWithFacetInput.setCode("massive");
        createFacetValueWithFacetInput.setName("massive");

        createFacetInput.getValues().add(createFacetValueWithFacetInput);

        JsonNode inputNode = objectMapper.valueToTree(createFacetInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_FACET, variables,
                Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet createdFacet = graphQLResponse.get("$.data.createFacet", Facet.class);

        facetValue = createdFacet.getValues().get(0);

        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(2L);
        updateProductInput.setFacetValueIds(new ArrayList<>());
        updateProductInput.getFacetValueIds().add(facetValue.getId());

        inputNode = objectMapper.valueToTree(updateProductInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(6L);
        updateProductVariantInput.getFacetValueIds().add(facetValue.getId());

        inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
    }

    @Test
    @Order(7)
    public void omits_private_Product_facetValues() throws IOException {
        before_test_7();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_FACET_VALUES, variables);
        Product product = graphQLResponse.get("$.data.product", Product.class);

        assertThat(product.getFacetValues()).isEmpty();
    }

    @Test
    @Order(8)
    public void omits_private_ProductVariant_facetValues() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_VARIANT_FACET_VALUES, variables);
        Product product = graphQLResponse.get("$.data.product", Product.class);

        assertThat(product.getVariants().get(0).getFacetValues()).isEmpty();
    }

    /**
     * collections
     */

    Collection collection;

    private void before_test_9() throws IOException {
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_FACET_LIST, null,
                        Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        FacetList facetList = graphQLResponse.get("$.data.facets", FacetList.class);
        Facet category = facetList.getItems().get(0);
        FacetValue sportsEquipment = category.getValues().stream()
                .filter(v -> Objects.equals("sports-equipment", v.getCode())).findFirst().get();

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + sportsEquipment.getId() + "\"]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("false");
        configurableOperationInput.getArguments().add(configArgInput);

        CreateCollectionInput createCollectionInput = new CreateCollectionInput();
        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("My Collection");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("my-collection");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        collection = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();
    }

    @Test
    @Order(9)
    public void returns_collection_with_variants() throws IOException {
        before_test_9();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = shopClient.perform(GET_COLLECTION_VARIANTS, variables);
        Collection foundCollection = graphQLResponse.get("$.data.collection", Collection.class);

        List<ProductVariant> variants = foundCollection.getProductVariants().getItems();
        assertThat(variants).hasSize(10);
        assertThat(variants.stream().map(ProductVariant::getId).collect(Collectors.toList())).containsExactly(
                22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L, 31L
        );
        assertThat(variants.stream().map(ProductVariant::getName).collect(Collectors.toList())).containsExactly(
                "Road Bike",
                "Skipping Rope",
                "Boxing Gloves",
                "Tent",
                "Cruiser Skateboard",
                "Football",
                "Running Shoe Size 40",
                "Running Shoe Size 42",
                "Running Shoe Size 44",
                "Running Shoe Size 46"
        );
    }

    @Test
    @Order(10)
    public void collection_by_slug() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("slug", collection.getSlug());

        GraphQLResponse graphQLResponse = shopClient.perform(GET_COLLECTION_VARIANTS, variables);
        Collection foundCollection = graphQLResponse.get("$.data.collection", Collection.class);
        assertThat(foundCollection.getId()).isEqualTo(collection.getId());
    }

    @Test
    @Order(11)
    public void omits_variants_from_disabled_products() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 17);

        adminClient.perform(DISABLE_PRODUCT, variables);

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = shopClient.perform(GET_COLLECTION_VARIANTS, variables);
        Collection foundCollection = graphQLResponse.get("$.data.collection", Collection.class);

        List<ProductVariant> variants = foundCollection.getProductVariants().getItems();
        assertThat(variants).hasSize(6);
        assertThat(variants.stream().map(ProductVariant::getId).collect(Collectors.toList())).containsExactly(
                22L, 23L, 24L, 25L, 26L, 27L
        );
        assertThat(variants.stream().map(ProductVariant::getName).collect(Collectors.toList())).containsExactly(
                "Road Bike",
                "Skipping Rope",
                "Boxing Gloves",
                "Tent",
                "Cruiser Skateboard",
                "Football"
        );
    }

    @Test
    @Order(12)
    public void omits_disabled_product_variants() throws IOException {
        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(22L);
        updateProductVariantInput.setEnabled(false);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = shopClient.perform(GET_COLLECTION_VARIANTS, variables);
        Collection foundCollection = graphQLResponse.get("$.data.collection", Collection.class);

        List<ProductVariant> variants = foundCollection.getProductVariants().getItems();
        assertThat(variants).hasSize(5);
        assertThat(variants.stream().map(ProductVariant::getId).collect(Collectors.toList())).containsExactly(
                23L, 24L, 25L, 26L, 27L
        );
        assertThat(variants.stream().map(ProductVariant::getName).collect(Collectors.toList())).containsExactly(
                "Skipping Rope",
                "Boxing Gloves",
                "Tent",
                "Cruiser Skateboard",
                "Football"
        );
    }

    @Test
    @Order(13)
    public void collection_list() throws IOException {
        GraphQLResponse graphQLResponse = this.shopClient.perform(GET_COLLECTION_LIST, null);
        CollectionList collectionList = graphQLResponse.get("$.data.collections", CollectionList.class);

        assertThat(collectionList.getItems()).hasSize(2);
    }

    @Test
    @Order(14)
    public void omits_private_collections() throws IOException {
        UpdateCollectionInput input = new UpdateCollectionInput();
        input.setId(collection.getId());
        input.setPrivateOnly(true);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        adminClient.perform(UPDATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        testHelper.awaitRunningTasks();

        GraphQLResponse graphQLResponse = this.shopClient.perform(GET_COLLECTION_LIST, null);
        CollectionList collectionList = graphQLResponse.get("$.data.collections", CollectionList.class);

        assertThat(collectionList.getItems()).hasSize(1);
        Collection collection = collectionList.getItems().get(0);
        assertThat(collection.getId()).isEqualTo(2L);
        assertThat(collection.getName()).isEqualTo("Plants");
    }

    @Test
    @Order(15)
    public void returns_null_for_private_collection() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = shopClient.perform(GET_COLLECTION_VARIANTS, variables);
        Collection foundCollection = graphQLResponse.get("$.data.collection", Collection.class);
        assertThat(foundCollection).isNull();
    }

    @Test
    @Order(16)
    public void product_collections_list_omits_private_collections() throws IOException {
        GraphQLResponse graphQLResponse = shopClient.perform(GET_PRODUCT_COLLECTION, null);
        Product product = graphQLResponse.get("$.data.product", Product.class);
        assertThat(product.getCollections()).isEmpty();
    }
}
