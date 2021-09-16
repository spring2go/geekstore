/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.common.Constant;
import io.geekstore.config.TestConfig;
import io.geekstore.config.collection.CollectionFilter;
import io.geekstore.types.asset.*;
import io.geekstore.types.collection.*;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.common.*;
import io.geekstore.types.facet.FacetList;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.product.*;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class CollectionTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    static final String GET_ASSET_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_asset_list");
    static final String FACET_VALUE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_value_fragment");
    static final String CREATE_COLLECTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_collection");
    static final String COLLECTION_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "collection_fragment");
    static final String CONFIGURABLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "configurable_fragment");
    static final String UPDATE_COLLECTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_collection");
    static final String UPDATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product");
    static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    static final String UPDATE_PRODUCT_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product_variants");
    static final String DELETE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "delete_product");

    static final String COLLECTION_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/collection/%s.graphqls";
    static final String GET_FACET_VALUES =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_facet_values");
    static final String GET_COLLECTION_BREADCRUMBS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_collection_breadcrumbs");
    static final String GET_COLLECTION =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_collection");
    static final String GET_PRODUCT_COLLECTIONS_WITH_PARENT =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_product_collections_with_parent");
    static final String GET_COLLECTIONS_WITH_ASSETS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_collections_with_assets");
    static final String MOVE_COLLECTION =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "move_collection");
    static final String GET_COLLECTIONS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_collections");
    static final String GET_COLLECTION_PRODUCT_VARIANTS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_collection_product_variants");
    static final String DELETE_COLLECTION =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "delete_collection");
    static final String GET_PRODUCT_COLLECTIONS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_product_collections");
    static final String CREATE_COLLECTION_SELECT_VARIANTS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "create_collection_select_variants");
    static final String GET_PRODUCTS_WITH_VARIANTS_IDS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_products_with_variant_ids");
    static final String GET_COLLECTIONS_FOR_PRODUCTS =
            String.format(COLLECTION_GRAPHQL_RESOURCE_TEMPLATE, "get_collections_for_products");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    List<Asset> assets;
    List<FacetValue> facetValues;
    Collection electronicsCollection;
    Collection computersCollection;
    Collection pearCollection;

    @Autowired
    @Qualifier("facetValueCollectionFilter")
    CollectionFilter facetValueCollectionFilter;

    @Autowired
    @Qualifier("variantNameCollectionFilter")
    CollectionFilter variantNameCollectionFilter;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-collections.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        AssetListOptions options = new AssetListOptions();
        AssetSortParameter sortParameter = new AssetSortParameter();
        sortParameter.setName(SortOrder.ASC);
        options.setSort(sortParameter);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ASSET_LIST, variables, Arrays.asList(ASSET_FRAGMENT));
        AssetList assetList = graphQLResponse.get("$.data.assets", AssetList.class);
        assertThat(assetList.getTotalItems()).isEqualTo(4);

        assets = assetList.getItems();

        graphQLResponse = adminClient.perform(GET_FACET_VALUES, null, Arrays.asList(FACET_VALUE_FRAGMENT));
        FacetList facetList = graphQLResponse.get("$.data.facets", FacetList.class);

        facetValues = new ArrayList<>();
        facetList.getItems().forEach(facet -> facetValues.addAll(facet.getValues()));
    }

    @Test
    @Order(1)
    public void collection_breadcrumbs_works_after_bootstrap() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_BREADCRUMBS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getBreadcrumbs()).isNotEmpty();
        assertThat(collection.getBreadcrumbs().get(0).getName()).isEqualTo(Constant.ROOT_COLLECTION_NAME);
    }

    /**
     * createCollection
     */

    @Test
    @Order(2)
    public void creates_a_root_collection() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();
        createCollectionInput.getAssetIds().add(assets.get(0).getId());
        createCollectionInput.getAssetIds().add(assets.get(1).getId());
        createCollectionInput.setFeaturedAssetId(assets.get(1).getId());

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + this.getFacetValueId("electronics") + "\"]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("false");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("Electronics");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("electronics");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        electronicsCollection = graphQLResponse.get("$.data.createCollection", Collection.class);
        assertThat(electronicsCollection.getParent().getName()).isEqualTo(Constant.ROOT_COLLECTION_NAME);
        verifyElectronicsCollection(electronicsCollection);
    }

    @Test
    @Order(3)
    public void creates_a_nested_collection() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();
        createCollectionInput.setParentId(electronicsCollection.getId());

        createCollectionInput.setName("Computers");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("computers");

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + this.getFacetValueId("computers") + "\"]");
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
        computersCollection = graphQLResponse.get("$.data.createCollection", Collection.class);
        assertThat(computersCollection.getParent().getName()).isEqualTo(electronicsCollection.getName());
    }

    @Test
    @Order(4)
    public void creates_a_2nd_level_nested_collection() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();
        createCollectionInput.setParentId(computersCollection.getId());

        createCollectionInput.setName("Pear");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("pear");

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + this.getFacetValueId("pear") + "\"]");
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
        pearCollection = graphQLResponse.get("$.data.createCollection", Collection.class);
        assertThat(pearCollection.getParent().getName()).isEqualTo(computersCollection.getName());
    }

    @Test
    @Order(5)
    public void slug_is_normalized_to_be_url_safe() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        createCollectionInput.setName("Zubehör");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("Zubehör!");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.createCollection", Collection.class);
        assertThat(collection.getSlug()).isEqualTo("zubehor");
    }

    @Test
    @Order(6)
    public void create_with_duplicate_slug_is_renamed_to_be_unique() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        createCollectionInput.setName("Zubehör");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("Zubehör!");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.createCollection", Collection.class);
        assertThat(collection.getSlug()).isEqualTo("zubehor-2");
    }

    /**
     * updateCollection
     */
    @Test
    @Order(7)
    public void updates_with_assets() throws IOException {
        UpdateCollectionInput input = new UpdateCollectionInput();
        input.setId(pearCollection.getId());
        input.getAssetIds().add(assets.get(1).getId());
        input.getAssetIds().add(assets.get(2).getId());
        input.setFeaturedAssetId(assets.get(1).getId());
        input.setDescription("Apple stuff ");
        input.setSlug("apple-stuff");

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        pearCollection = graphQLResponse.get("$.data.updateCollection", Collection.class);
        verifyPearCollection(pearCollection);
    }

    @Test
    @Order(8)
    public void updating_existing_assets() throws IOException {
        UpdateCollectionInput input = new UpdateCollectionInput();
        input.setId(pearCollection.getId());
        input.getAssetIds().add(assets.get(3).getId());
        input.getAssetIds().add(assets.get(0).getId());
        input.setFeaturedAssetId(assets.get(3).getId());

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        Collection updatedCollection = graphQLResponse.get("$.data.updateCollection", Collection.class);
        assertThat(updatedCollection.getAssets().stream().map(a -> a.getId()).collect(Collectors.toList()))
                .containsExactly(assets.get(3).getId(), assets.get(0).getId());
    }

    @Test
    @Order(8)
    public void remove_all_assets() throws IOException {
        UpdateCollectionInput input = new UpdateCollectionInput();
        input.setId(pearCollection.getId());

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        Collection updatedCollection = graphQLResponse.get("$.data.updateCollection", Collection.class);
        assertThat(updatedCollection.getAssets()).isEmpty();
        assertThat(updatedCollection.getFeaturedAsset()).isNull();
    }

    @Test
    @Order(9)
    public void collection_by_id() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", computersCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getId()).isEqualTo(computersCollection.getId());
    }

    @Test
    @Order(10)
    public void collection_by_slug() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("slug", computersCollection.getSlug());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getId()).isEqualTo(computersCollection.getId());
    }

    @Test
    @Order(11)
    public void throws_if_neither_id_nor_slug_provided() throws IOException {
        try {
            adminClient.perform(GET_COLLECTION, null,
                    Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Either the Collection id or slug must be provided");
        }
    }

    @Test
    @Order(12)
    public void throws_if_id_and_slug_do_not_refer_to_the_same_product() throws IOException {
        try {
            ObjectNode variables = objectMapper.createObjectNode();
            variables.put("id", computersCollection.getId());
            variables.put("slug", pearCollection.getSlug());

            adminClient.perform(GET_COLLECTION, variables,
                    Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("The provided id and slug refer to different Collections");
        }
    }

    @Test
    @Order(13)
    public void parent_field() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", computersCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getParent().getName()).isEqualTo("Electronics");
    }

    @Test
    @Order(14)
    public void parent_field_resolved_by_collection_resolver() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_COLLECTIONS_WITH_PARENT, variables);
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);

        assertThat(product.getCollections()).hasSize(3);
        List<Collection> sortedCollections = product.getCollections().stream()
                .sorted(Comparator.comparing(Collection::getId)).collect(Collectors.toList());
        Collection collection1 = sortedCollections.get(0);
        assertThat(collection1.getId()).isEqualTo(3L);
        assertThat(collection1.getName()).isEqualTo("Electronics");
        assertThat(collection1.getParent().getId()).isEqualTo(1L);
        assertThat(collection1.getParent().getName()).isEqualTo(Constant.ROOT_COLLECTION_NAME);

        Collection collection2 = sortedCollections.get(1);
        assertThat(collection2.getId()).isEqualTo(4L);
        assertThat(collection2.getName()).isEqualTo("Computers");
        assertThat(collection2.getParent().getId()).isEqualTo(3L);
        assertThat(collection2.getParent().getName()).isEqualTo("Electronics");

        Collection collection3 = sortedCollections.get(2);
        assertThat(collection3.getId()).isEqualTo(5L);
        assertThat(collection3.getName()).isEqualTo("Pear");
        assertThat(collection3.getParent().getId()).isEqualTo(4L);
        assertThat(collection3.getParent().getName()).isEqualTo("Computers");
    }

    @Test
    @Order(15)
    public void children_field() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", electronicsCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getChildren()).hasSize(1);
        assertThat(collection.getChildren().get(0).getName()).isEqualTo("Computers");
    }

    @Test
    @Order(16)
    public void breadcrumbs() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", pearCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_BREADCRUMBS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getBreadcrumbs()).hasSize(4);

        CollectionBreadcrumb breadcrumb1 = collection.getBreadcrumbs().get(0);
        assertThat(breadcrumb1.getId()).isEqualTo(1);
        assertThat(breadcrumb1.getName()).isEqualTo(Constant.ROOT_COLLECTION_NAME);
        assertThat(breadcrumb1.getSlug()).isEqualTo(Constant.ROOT_COLLECTION_NAME);

        CollectionBreadcrumb breadcrumb2 = collection.getBreadcrumbs().get(1);
        assertThat(breadcrumb2.getId()).isEqualTo(electronicsCollection.getId());
        assertThat(breadcrumb2.getName()).isEqualTo(electronicsCollection.getName());
        assertThat(breadcrumb2.getSlug()).isEqualTo(electronicsCollection.getSlug());

        CollectionBreadcrumb breadcrumb3 = collection.getBreadcrumbs().get(2);
        assertThat(breadcrumb3.getId()).isEqualTo(computersCollection.getId());
        assertThat(breadcrumb3.getName()).isEqualTo(computersCollection.getName());
        assertThat(breadcrumb3.getSlug()).isEqualTo(computersCollection.getSlug());

        CollectionBreadcrumb breadcrumb4 = collection.getBreadcrumbs().get(3);
        assertThat(breadcrumb4.getId()).isEqualTo(pearCollection.getId());
        assertThat(breadcrumb4.getName()).isEqualTo(pearCollection.getName());
        assertThat(breadcrumb4.getSlug()).isEqualTo(pearCollection.getSlug());
    }

    @Test
    @Order(17)
    public void breadcrumbs_for_root_collection() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_BREADCRUMBS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        CollectionBreadcrumb breadcrumb = collection.getBreadcrumbs().get(0);
        assertThat(breadcrumb.getId()).isEqualTo(1L);
        assertThat(breadcrumb.getName()).isEqualTo(Constant.ROOT_COLLECTION_NAME);
        assertThat(breadcrumb.getSlug()).isEqualTo(Constant.ROOT_COLLECTION_NAME);
    }

    @Test
    @Order(18)
    public void get_collections_with_assets() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTIONS_WITH_ASSETS, null);
        CollectionList collectionList = graphQLResponse.get("$.data.adminCollections", CollectionList.class);
        assertThat(collectionList.getItems()).isNotEmpty();
        assertThat(collectionList.getItems().stream()
                .filter(c -> "Electronics".equals(c.getName())).findFirst().get().getAssets()).isNotEmpty();
    }

    /**
     * moveCollection
     */

    @Test
    @Order(19)
    public void moves_a_collection_to_a_new_parent() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(pearCollection.getId());
        input.setParentId(electronicsCollection.getId());
        input.setIndex(0);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                MOVE_COLLECTION, variables, Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.moveCollection", Collection.class);
        assertThat(collection.getParent().getId()).isEqualTo(electronicsCollection.getId());

        List<Pair> pairs = this.getChildrenOf(electronicsCollection.getId());
        assertThat(pairs.stream().map(p -> p.getValue()).collect(Collectors.toList()))
                .containsExactly(pearCollection.getId(), computersCollection.getId());
    }

    @Test
    @Order(20)
    public void re_evaluates_collection_contents_on_move() throws IOException {
        testHelper.awaitRunningTasks();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", pearCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                        "Laptop 13 inch 8GB",
                        "Laptop 15 inch 8GB",
                        "Laptop 13 inch 16GB",
                        "Laptop 15 inch 16GB",
                        "Instant Camera"
        );
    }

    @Test
    @Order(21)
    public void alters_the_position_in_the_current_parent_1() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(computersCollection.getId());
        input.setParentId(electronicsCollection.getId());
        input.setIndex(0);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        adminClient.perform(
                MOVE_COLLECTION, variables, Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        List<Pair> afterResults = this.getChildrenOf(electronicsCollection.getId());
        assertThat(afterResults.stream().map(p -> p.getValue()).collect(Collectors.toList()))
                .containsExactly(computersCollection.getId(), pearCollection.getId());
    }

    @Test
    @Order(22)
    public void alters_the_position_in_the_current_parent_2() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(pearCollection.getId());
        input.setParentId(electronicsCollection.getId());
        input.setIndex(0);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        adminClient.perform(
                MOVE_COLLECTION, variables, Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        List<Pair> afterResults = this.getChildrenOf(electronicsCollection.getId());
        assertThat(afterResults.stream().map(p -> p.getValue()).collect(Collectors.toList()))
                .containsExactly(pearCollection.getId(), computersCollection.getId());
    }

    @Test
    @Order(23)
    public void corrects_an_out_of_bounds_negative_index_value() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(pearCollection.getId());
        input.setParentId(electronicsCollection.getId());
        input.setIndex(-3);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        adminClient.perform(
                MOVE_COLLECTION, variables, Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        List<Pair> afterResults = this.getChildrenOf(electronicsCollection.getId());
        assertThat(afterResults.stream().map(p -> p.getValue()).collect(Collectors.toList()))
                .containsExactly(pearCollection.getId(), computersCollection.getId());
    }

    @Test
    @Order(24)
    public void corrects_an_out_of_bounds_positive_index_value() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(pearCollection.getId());
        input.setParentId(electronicsCollection.getId());
        input.setIndex(10);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        adminClient.perform(
                MOVE_COLLECTION, variables, Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        List<Pair> afterResults = this.getChildrenOf(electronicsCollection.getId());
        assertThat(afterResults.stream().map(p -> p.getValue()).collect(Collectors.toList()))
                .containsExactly(computersCollection.getId(), pearCollection.getId());
    }

    @Test
    @Order(25)
    public void throws_if_attempting_to_move_into_self() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(pearCollection.getId());
        input.setParentId(pearCollection.getId());
        input.setIndex(0);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        try {
            adminClient.perform(MOVE_COLLECTION, variables,
                    Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Cannot move a Collection into itself");
        }
    }

    @Test
    @Order(25)
    public void throws_if_attempting_to_move_into_a_decendant_of_self() throws IOException {
        MoveCollectionInput input = new MoveCollectionInput();
        input.setCollectionId(electronicsCollection.getId());
        input.setParentId(pearCollection.getId());
        input.setIndex(0);

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        try {
            adminClient.perform(MOVE_COLLECTION, variables,
                    Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Cannot move a Collection into itself");
        }
    }

    /**
     * deleteCollection
     */
    Collection collectionToDeleteParent;
    Collection collectionToDeleteChild;
    Long laptopProductId;

    private void before_test_26() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();
        createCollectionInput.getAssetIds().add(1L);

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(variantNameCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("operator");
        configArgInput.setValue("contains");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("term");
        configArgInput.setValue("Laptop");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("Delete Me Parent");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("delete-me-parent");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        collectionToDeleteParent = graphQLResponse.get("$.data.createCollection", Collection.class);

        createCollectionInput = new CreateCollectionInput();
        createCollectionInput.getAssetIds().add(2L);

        createCollectionInput.setName("Delete Me Child");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("delete-me-child");
        createCollectionInput.setParentId(collectionToDeleteParent.getId());

        optionsNode = objectMapper.valueToTree(createCollectionInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        collectionToDeleteChild = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();
    }

    @Test
    @Order(26)
    public void throws_for_invalid_collection_id() throws IOException {
        before_test_26();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 999L);

        try {
            adminClient.perform(DELETE_COLLECTION, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("No CollectionEntity with the id '999' could be found");
        }
    }

    @Test
    @Order(27)
    public void collection_and_product_related_prior_to_deletion() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collectionToDeleteParent.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB", "Laptop 15 inch 8GB", "Laptop 13 inch 16GB", "Laptop 15 inch 16GB");

        laptopProductId = collection.getProductVariants().getItems().get(0).getProductId();

        variables = objectMapper.createObjectNode();
        variables.put("id", laptopProductId);

        graphQLResponse = adminClient.perform(GET_PRODUCT_COLLECTIONS, variables);
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product.getCollections()).hasSize(5);

        Collection collection1 = product.getCollections().get(0);
        assertThat(collection1.getId()).isEqualTo(3L);
        assertThat(collection1.getName()).isEqualTo("Electronics");

        Collection collection2 = product.getCollections().get(1);
        assertThat(collection2.getId()).isEqualTo(4L);
        assertThat(collection2.getName()).isEqualTo("Computers");

        Collection collection3 = product.getCollections().get(2);
        assertThat(collection3.getId()).isEqualTo(5L);
        assertThat(collection3.getName()).isEqualTo("Pear");

        Collection collection4 = product.getCollections().get(3);
        assertThat(collection4.getId()).isEqualTo(8L);
        assertThat(collection4.getName()).isEqualTo("Delete Me Parent");

        Collection collection5 = product.getCollections().get(4);
        assertThat(collection5.getId()).isEqualTo(9L);
        assertThat(collection5.getName()).isEqualTo("Delete Me Child");
    }

    @Test
    @Order(28)
    public void delete_collection_works() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collectionToDeleteParent.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(DELETE_COLLECTION, variables);
        DeletionResponse deletionResponse =
                graphQLResponse.get("$.data.deleteCollection", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);
    }

    @Test
    @Order(29)
    public void deleted_parent_collection_is_null() throws IOException {
        verifyCollectionDeleted(collectionToDeleteParent.getId());
    }

    @Test
    @Order(30)
    public void deleted_child_collection_is_null() throws IOException {
        verifyCollectionDeleted(collectionToDeleteChild.getId());
    }

    @Test
    @Order(31)
    public void product_no_longer_lists_collection() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", laptopProductId);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_COLLECTIONS, variables);
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        assertThat(product.getCollections()).hasSize(3);

        Collection collection1 = product.getCollections().get(0);
        assertThat(collection1.getId()).isEqualTo(3L);
        assertThat(collection1.getName()).isEqualTo("Electronics");

        Collection collection2 = product.getCollections().get(1);
        assertThat(collection2.getId()).isEqualTo(4L);
        assertThat(collection2.getName()).isEqualTo("Computers");

        Collection collection3 = product.getCollections().get(2);
        assertThat(collection3.getId()).isEqualTo(5L);
        assertThat(collection3.getName()).isEqualTo("Pear");
    }

    /**
     * filters
     */

    @Test
    @Order(32)
    public void collection_with_no_filters_has_no_product_variants() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        createCollectionInput.setName("Empty");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("empty");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION_SELECT_VARIANTS, variables);
        Collection collection = graphQLResponse.get("$.data.createCollection", Collection.class);

        assertThat(collection.getProductVariants().getTotalItems()).isEqualTo(0);
    }

    /**
     * facetValue filter
     */
    @Test
    @Order(33)
    public void electronics() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", electronicsCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Gaming PC i7-8700 240GB SSD",
                "Gaming PC R7-2700 240GB SSD",
                "Gaming PC i7-8700 120GB SSD",
                "Gaming PC R7-2700 120GB SSD",
                "Hard Drive 1TB",
                "Hard Drive 2TB",
                "Hard Drive 3TB",
                "Hard Drive 4TB",
                "Hard Drive 6TB",
                "Clacky Keyboard",
                "USB Cable",
                "Instant Camera",
                "Camera Lens",
                "Tripod",
                "SLR Camera"
        );
    }

    @Test
    @Order(34)
    public void computers() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", computersCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Gaming PC i7-8700 240GB SSD",
                "Gaming PC R7-2700 240GB SSD",
                "Gaming PC i7-8700 120GB SSD",
                "Gaming PC R7-2700 120GB SSD",
                "Hard Drive 1TB",
                "Hard Drive 2TB",
                "Hard Drive 3TB",
                "Hard Drive 4TB",
                "Hard Drive 6TB",
                "Clacky Keyboard",
                "USB Cable"
        );
    }

    @Test
    @Order(35)
    public void photo_and_pear() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + this.getFacetValueId("pear") +"\",\"" + this.getFacetValueId("photo")+ "\"]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("false");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("Photo AND Pear");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("photo-and-pear");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION_SELECT_VARIANTS, variables);
        Collection result = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", result.getId());

        graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(ProductVariant::getName)).containsExactly("Instant Camera");
    }

    @Test
    @Order(36)
    public void photo_or_pear() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + this.getFacetValueId("pear") +"\",\"" + this.getFacetValueId("photo")+ "\"]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("true");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("Photo OR Pear");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("photo-or-pear");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION_SELECT_VARIANTS, variables);
        Collection result = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", result.getId());

        graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(ProductVariant::getName)).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Instant Camera",
                "Camera Lens",
                "Tripod",
                "SLR Camera",
                "Hat"
        );
    }

    @Test
    @Order(37)
    public void bell_or_pear_in_computers() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue(
                "[\"" + this.getFacetValueId("pear") +"\",\"" + this.getFacetValueId("bell")+ "\"]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("true");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("Bell OR Pear Computers");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("bell-or-pear");
        createCollectionInput.setParentId(computersCollection.getId());

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION_SELECT_VARIANTS, variables);
        Collection result = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", result.getId());

        graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection.getProductVariants().getItems().stream()
                .map(ProductVariant::getName)).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch"
        );
    }

    @Test
    @Order(38)
    public void contains_operator() throws IOException {
        Collection collection = createVariantNameFilteredCollection("contains", "camera");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Instant Camera",
                "Camera Lens",
                "SLR Camera"
        );
    }

    @Test
    @Order(39)
    public void starts_with_operator() throws IOException {
        Collection collection = createVariantNameFilteredCollection("startsWith", "camera");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Camera Lens"
        );
    }

    @Test
    @Order(40)
    public void ends_with_operator() throws IOException {
        Collection collection = createVariantNameFilteredCollection("endsWith", "camera");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Instant Camera",
                "SLR Camera"
        );
    }

    @Test
    @Order(41)
    public void does_not_contain_operator() throws IOException {
        Collection collection = createVariantNameFilteredCollection("doesNotContain", "camera");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", collection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Gaming PC i7-8700 240GB SSD",
                "Gaming PC R7-2700 240GB SSD",
                "Gaming PC i7-8700 120GB SSD",
                "Gaming PC R7-2700 120GB SSD",
                "Hard Drive 1TB",
                "Hard Drive 2TB",
                "Hard Drive 3TB",
                "Hard Drive 4TB",
                "Hard Drive 6TB",
                "Clacky Keyboard",
                "USB Cable",
                "Tripod",
                "Hat"
        );
    }

    List<Product> products;

    private void before_test_42() throws IOException {
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_PRODUCTS_WITH_VARIANTS_IDS, null);
        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        products = productList.getItems();
    }

    @Test
    @Order(41)
    public void updates_contents_when_product_is_updated() throws IOException {
        before_test_42();

        UpdateProductInput input = new UpdateProductInput();
        input.setId(products.get(1).getId());
        input.setFacetValueIds(new ArrayList<>());
        input.getFacetValueIds().add(getFacetValueId("electronics"));
        input.getFacetValueIds().add(getFacetValueId("computers"));
        input.getFacetValueIds().add(getFacetValueId("pear"));

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        this.adminClient.perform(UPDATE_PRODUCT, variables, Arrays.asList(
                ASSET_FRAGMENT, PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT
        ));

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", pearCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Instant Camera"
        );
    }

    @Test
    @Order(42)
    public void updates_contents_when_product_variant_is_updated() throws IOException {
        ProductVariant gamingPc240GB = products.stream()
                .filter(p -> Objects.equals(p.getName(), "Gaming PC"))
                .findFirst().get()
                .getVariants().stream()
                .filter(v -> v.getName().contains("240GB"))
                .findFirst().get();

        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(gamingPc240GB.getId());
        input.getFacetValueIds().add(getFacetValueId("pear"));

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables, Arrays.asList(
                ASSET_FRAGMENT, PRODUCT_VARIANT_FRAGMENT
        ));

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", pearCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Gaming PC i7-8700 240GB SSD",
                "Instant Camera"
        );
    }

    @Test
    @Order(43)
    public void correctly_filters_when_product_variant_and_product_both_have_matching_facet_value() throws IOException {
        ProductVariant gamingPc240GB = products.stream()
                .filter(p -> Objects.equals(p.getName(), "Gaming PC"))
                .findFirst().get()
                .getVariants().stream()
                .filter(v -> v.getName().contains("240GB"))
                .findFirst().get();

        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(gamingPc240GB.getId());
        input.getFacetValueIds().add(getFacetValueId("electronics"));
        input.getFacetValueIds().add(getFacetValueId("pear"));

        JsonNode optionsNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables, Arrays.asList(
                ASSET_FRAGMENT, PRODUCT_VARIANT_FRAGMENT
        ));

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", pearCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Gaming PC i7-8700 240GB SSD",
                "Instant Camera"
        );
    }

    @Test
    @Order(44)
    public void filter_inheritance_of_nested_collections() throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();
        createCollectionInput.setParentId(electronicsCollection.getId());

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValueCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"" + this.getFacetValueId("pear") +"\"]");
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("containsAny");
        configArgInput.setValue("false");
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName("pear electronics");
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug("pear-electronics");

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION_SELECT_VARIANTS, variables);
        Collection pearElectronics = graphQLResponse.get("$.data.createCollection", Collection.class);

        testHelper.awaitRunningTasks();

        variables = objectMapper.createObjectNode();
        variables.put("id", pearElectronics.getId());

        graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Curvy Monitor 24 inch",
                "Curvy Monitor 27 inch",
                "Gaming PC i7-8700 240GB SSD",
                "Instant Camera"
                // no "Hat"
        );
    }

    /**
     * Product collections property
     */
    @Test
    @Order(45)
    public void returns_all_collections_to_which_the_product_belongs() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("term", "camera");

        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_COLLECTIONS_FOR_PRODUCTS, variables);
        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        assertThat(productList.getItems().get(0).getCollections()).hasSize(7);

        List<Collection> collectionList = productList.getItems().get(0).getCollections();
        Collection collection1 = collectionList.get(0);
        assertThat(collection1.getId()).isEqualTo(3L);
        assertThat(collection1.getName()).isEqualTo("Electronics");

        Collection collection2 = collectionList.get(1);
        assertThat(collection2.getId()).isEqualTo(5L);
        assertThat(collection2.getName()).isEqualTo("Pear");

        Collection collection3 = collectionList.get(2);
        assertThat(collection3.getId()).isEqualTo(11L);
        assertThat(collection3.getName()).isEqualTo("Photo AND Pear");

        Collection collection4 = collectionList.get(3);
        assertThat(collection4.getId()).isEqualTo(12L);
        assertThat(collection4.getName()).isEqualTo("Photo OR Pear");

        Collection collection5 = collectionList.get(4);
        assertThat(collection5.getId()).isEqualTo(14L);
        assertThat(collection5.getName()).isEqualTo("contains camera");

        Collection collection6 = collectionList.get(5);
        assertThat(collection6.getId()).isEqualTo(16L);
        assertThat(collection6.getName()).isEqualTo("endsWith camera");

        Collection collection7 = collectionList.get(6);
        assertThat(collection7.getId()).isEqualTo(18L);
        assertThat(collection7.getName()).isEqualTo("pear electronics");
    }

    @Test
    @Order(46)
    public void collection_does_not_list_deleted_products() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L); // curvy monitor

        this.adminClient.perform(DELETE_PRODUCT, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", pearCollection.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION_PRODUCT_VARIANTS, variables);
        Collection resultCollection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(resultCollection.getProductVariants().getItems().stream()
                .map(v -> v.getName()).collect(Collectors.toList())).containsExactly(
                "Laptop 13 inch 8GB",
                "Laptop 15 inch 8GB",
                "Laptop 13 inch 16GB",
                "Laptop 15 inch 16GB",
                "Gaming PC i7-8700 240GB SSD",
                "Instant Camera"
        );
    }

    private Collection createVariantNameFilteredCollection(String operator, String term) throws IOException {
        CreateCollectionInput createCollectionInput = new CreateCollectionInput();

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(variantNameCollectionFilter.getCode());

        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("operator");
        configArgInput.setValue(operator);
        configurableOperationInput.getArguments().add(configArgInput);

        configArgInput = new ConfigArgInput();
        configArgInput.setName("term");
        configArgInput.setValue(term);
        configurableOperationInput.getArguments().add(configArgInput);

        createCollectionInput.getFilters().add(configurableOperationInput);

        createCollectionInput.setName(operator + " " +term);
        createCollectionInput.setDescription("");
        createCollectionInput.setSlug(operator + " " +term);

        JsonNode optionsNode = objectMapper.valueToTree(createCollectionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));

        testHelper.awaitRunningTasks();

        return graphQLResponse.get("$.data.createCollection", Collection.class);
    }

    private void verifyCollectionDeleted(Long id) throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", id);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTION, variables,
                Arrays.asList(COLLECTION_FRAGMENT, ASSET_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Collection collection = graphQLResponse.get("$.data.adminCollection", Collection.class);
        assertThat(collection).isNull();
    }

    private List<Pair> getChildrenOf(Long parentId) throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_COLLECTIONS, null);
        CollectionList collectionList = graphQLResponse.get("$.data.adminCollections", CollectionList.class);
        return collectionList.getItems().stream()
                .filter(c -> Objects.equals(c.getParent().getId(), parentId))
                .map(c -> Pair.of(c.getName(), c.getId()))
                .collect(Collectors.toList());
    }

    private void verifyPearCollection(Collection collection) {
        assertThat(collection.getAssets()).hasSize(2);

        Asset asset1 = collection.getAssets().get(0);
        assertThat(asset1.getFileSize()).isEqualTo(1680);
        assertThat(asset1.getId()).isEqualTo(1L);
        assertThat(asset1.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset1.getName()).isEqualTo("derick-david-409858-unsplash.jpg");
        assertThat(asset1.getPreview()).isEqualTo("target/test-assets/derick-david-409858-unsplash__preview.jpg");
        assertThat(asset1.getSource()).isEqualTo("target/test-assets/derick-david-409858-unsplash.jpg");
        assertThat(asset1.getType()).isEqualTo(AssetType.IMAGE);

        Asset asset2 = collection.getAssets().get(1);
        assertThat(asset2.getFileSize()).isEqualTo(1680);
        assertThat(asset2.getId()).isEqualTo(3L);
        assertThat(asset2.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset2.getName()).isEqualTo("florian-olivo-1166419-unsplash.jpg");
        assertThat(asset2.getPreview()).isEqualTo("target/test-assets/florian-olivo-1166419-unsplash__preview.jpg");
        assertThat(asset2.getSource()).isEqualTo("target/test-assets/florian-olivo-1166419-unsplash.jpg");
        assertThat(asset2.getType()).isEqualTo(AssetType.IMAGE);

        assertThat(collection.getChildren()).isEmpty();
        assertThat(collection.getDescription()).isEqualTo("Apple stuff ");

        Asset featuredAsset = collection.getFeaturedAsset();
        assertThat(featuredAsset.getFileSize()).isEqualTo(1680);
        assertThat(featuredAsset.getId()).isEqualTo(1L);
        assertThat(featuredAsset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(featuredAsset.getName()).isEqualTo("derick-david-409858-unsplash.jpg");
        assertThat(featuredAsset.getPreview()).isEqualTo("target/test-assets/derick-david-409858-unsplash__preview.jpg");
        assertThat(featuredAsset.getSource()).isEqualTo("target/test-assets/derick-david-409858-unsplash.jpg");
        assertThat(featuredAsset.getType()).isEqualTo(AssetType.IMAGE);

        assertThat(collection.getFilters()).hasSize(1);
        ConfigurableOperation configurableOperation = collection.getFilters().get(0);
        assertThat(configurableOperation.getCode()).isEqualTo("facet-value-filter");
        ConfigArg configArg1 = configurableOperation.getArgs().get(0);
        assertThat(configArg1.getName()).isEqualTo("facetValueIds");
        assertThat(configArg1.getValue()).isEqualTo("[\"3\"]");
        ConfigArg configArg2 = configurableOperation.getArgs().get(1);
        assertThat(configArg2.getName()).isEqualTo("containsAny");
        assertThat(configArg2.getValue()).isEqualTo("false");

        assertThat(collection.getId()).isEqualTo(5L);
        assertThat(collection.getPrivateOnly()).isFalse();
        assertThat(collection.getName()).isEqualTo("Pear");
        assertThat(collection.getParent().getId()).isEqualTo(4L);
        assertThat(collection.getParent().getName()).isEqualTo("Computers");

        assertThat(collection.getName()).isEqualTo("Pear");
        assertThat(collection.getSlug()).isEqualTo("apple-stuff");
    }

    private void verifyElectronicsCollection(Collection collection) {
        assertThat(collection.getAssets()).hasSize(2);

        Asset asset1 = collection.getAssets().get(0);
        assertThat(asset1.getFileSize()).isEqualTo(1680);
        assertThat(asset1.getId()).isEqualTo(2L);
        assertThat(asset1.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset1.getName()).isEqualTo("alexandru-acea-686569-unsplash.jpg");
        assertThat(asset1.getPreview()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash__preview.jpg");
        assertThat(asset1.getSource()).isEqualTo("target/test-assets/alexandru-acea-686569-unsplash.jpg");
        assertThat(asset1.getType()).isEqualTo(AssetType.IMAGE);

        Asset asset2 = collection.getAssets().get(1);
        assertThat(asset2.getFileSize()).isEqualTo(1680);
        assertThat(asset2.getId()).isEqualTo(1L);
        assertThat(asset2.getMimeType()).isEqualTo("image/jpeg");
        assertThat(asset2.getName()).isEqualTo("derick-david-409858-unsplash.jpg");
        assertThat(asset2.getPreview()).isEqualTo("target/test-assets/derick-david-409858-unsplash__preview.jpg");
        assertThat(asset2.getSource()).isEqualTo("target/test-assets/derick-david-409858-unsplash.jpg");
        assertThat(asset2.getType()).isEqualTo(AssetType.IMAGE);

        assertThat(collection.getChildren()).isEmpty();
        assertThat(collection.getDescription()).isEmpty();

        Asset featuredAsset = collection.getFeaturedAsset();
        assertThat(featuredAsset.getFileSize()).isEqualTo(1680);
        assertThat(featuredAsset.getId()).isEqualTo(1L);
        assertThat(featuredAsset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(featuredAsset.getName()).isEqualTo("derick-david-409858-unsplash.jpg");
        assertThat(featuredAsset.getPreview()).isEqualTo("target/test-assets/derick-david-409858-unsplash__preview.jpg");
        assertThat(featuredAsset.getSource()).isEqualTo("target/test-assets/derick-david-409858-unsplash.jpg");
        assertThat(featuredAsset.getType()).isEqualTo(AssetType.IMAGE);

        assertThat(collection.getFilters()).hasSize(1);
        ConfigurableOperation configurableOperation = collection.getFilters().get(0);
        assertThat(configurableOperation.getCode()).isEqualTo("facet-value-filter");
        ConfigArg configArg1 = configurableOperation.getArgs().get(0);
        assertThat(configArg1.getName()).isEqualTo("facetValueIds");
        assertThat(configArg1.getValue()).isEqualTo("[\"1\"]");
        ConfigArg configArg2 = configurableOperation.getArgs().get(1);
        assertThat(configArg2.getName()).isEqualTo("containsAny");
        assertThat(configArg2.getValue()).isEqualTo("false");

        assertThat(collection.getId()).isEqualTo(3L);
        assertThat(collection.getPrivateOnly()).isFalse();
        assertThat(collection.getName()).isEqualTo("Electronics");
        assertThat(collection.getParent().getId()).isEqualTo(1L);
        assertThat(collection.getParent().getName()).isEqualTo(Constant.ROOT_COLLECTION_NAME);

        assertThat(collection.getSlug()).isEqualTo("electronics");
    }

    private Long getFacetValueId(String code) {
        Optional<FacetValue> match = facetValues.stream().filter(fv -> Objects.equals(fv.getCode(), code)).findFirst();
        if (!match.isPresent()) {
            throw new RuntimeException("Could not find a FacetValue with the code " + code);
        }
        return match.get().getId();
    }
}
