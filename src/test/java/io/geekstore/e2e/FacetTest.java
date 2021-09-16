/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.facet.*;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductList;
import io.geekstore.types.product.UpdateProductInput;
import io.geekstore.types.product.UpdateProductVariantInput;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class FacetTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String CREATE_FACET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_facet");
    static final String UPDATE_FACET =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_facet");
    static final String FACET_WITH_VALUES_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_with_values_fragment");
    static final String FACET_VALUE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_value_fragment");
    static final String GET_FACET_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_facet_list");
    static final String UPDATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product");
    static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    static final String UPDATE_PRODUCT_VARIANTS =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product_variants");

    static final String ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/facet/%s.graphqls";
    static final String CREATE_FACET_VALUES =
            String.format(ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE, "create_facet_values");
    static final String UPDATE_FACET_VALUES =
            String.format(ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE, "update_facet_values");
    static final String GET_FACET_WITH_VALUES =
            String.format(ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE, "get_facet_with_values");
    static final String GET_PRODUCTS_LIST_WITH_VARIANTS =
            String.format(ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE, "get_products_list_with_variants");
    static final String DELETE_FACET_VALUES =
            String.format(ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE, "delete_facet_values");
    static final String DELETE_FACET =
            String.format(ADMIN_FACET_GRAPHQL_RESOURCE_TEMPLATE, "delete_facet");


    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    Facet speakerTypeFacet;
    Facet brandFacet;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    @Test
    @Order(1)
    public void createFacet() throws IOException {
        CreateFacetInput createFacetInput = new CreateFacetInput();
        createFacetInput.setPrivateOnly(false);
        createFacetInput.setCode("speaker-type");
        createFacetInput.setName("Speaker Type");
        CreateFacetValueWithFacetInput createFacetValueWithFacetInput =
                new CreateFacetValueWithFacetInput();
        createFacetValueWithFacetInput.setCode("portable");
        createFacetValueWithFacetInput.setName("Portable");
        createFacetInput.getValues().add(createFacetValueWithFacetInput);

        JsonNode inputNode = objectMapper.valueToTree(createFacetInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_FACET, variables,
                Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        speakerTypeFacet = graphQLResponse.get("$.data.createFacet", Facet.class);
        verifyFacet(speakerTypeFacet);
    }

    private void verifyFacet(Facet facet) {
        assertThat(facet.getCode()).isEqualTo("speaker-type");
        assertThat(facet.getId()).isEqualTo(2L);
        assertThat(facet.getPrivateOnly()).isFalse();
        assertThat(facet.getName()).isEqualTo("Speaker Type");

        assertThat(facet.getValues()).hasSize(1);
        FacetValue facetValue = facet.getValues().get(0);
        assertThat(facetValue.getCode()).isEqualTo("portable");
        assertThat(facetValue.getFacet().getId()).isEqualTo(2L);
        assertThat(facetValue.getFacet().getName()).isEqualTo("Speaker Type");
        assertThat(facetValue.getId()).isEqualTo(7L);
        assertThat(facetValue.getName()).isEqualTo("Portable");
    }

    @Test
    @Order(2)
    public void updateFacet() throws IOException {
        UpdateFacetInput input = new UpdateFacetInput();
        input.setId(speakerTypeFacet.getId());
        input.setName("Speaker Category");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_FACET, variables,
                Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet updatedFacet = graphQLResponse.get("$.data.updateFacet", Facet.class);
        assertThat(updatedFacet.getName()).isEqualTo("Speaker Category");
    }

    @Test
    @Order(3)
    public void createFacetValues() throws IOException {
        List<CreateFacetValueInput> inputs = new ArrayList<>();
        CreateFacetValueInput input = new CreateFacetValueInput();
        input.setFacetId(speakerTypeFacet.getId());
        input.setCode("pc");
        input.setName("PC Speakers");
        inputs.add(input);
        input = new CreateFacetValueInput();
        input.setFacetId(speakerTypeFacet.getId());
        input.setCode("hi-fi");
        input.setName("Hi Fi Speakers");
        inputs.add(input);

        JsonNode inputNode = objectMapper.valueToTree(inputs);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_FACET_VALUES, variables,
                Arrays.asList(FACET_VALUE_FRAGMENT));
        List<FacetValue> createdFacetValues = graphQLResponse.getList("$.data.createFacetValues", FacetValue.class);
        assertThat(createdFacetValues).hasSize(2);

        FacetValue facetValue1 = createdFacetValues.get(0);
        assertThat(facetValue1.getCode()).isEqualTo("pc");
        assertThat(facetValue1.getName()).isEqualTo("PC Speakers");
        assertThat(facetValue1.getFacet().getId()).isEqualTo(2L);
        assertThat(facetValue1.getFacet().getName()).isEqualTo("Speaker Category");

        FacetValue facetValue2 = createdFacetValues.get(1);
        assertThat(facetValue2.getCode()).isEqualTo("hi-fi");
        assertThat(facetValue2.getName()).isEqualTo("Hi Fi Speakers");
        assertThat(facetValue2.getFacet().getId()).isEqualTo(2L);
        assertThat(facetValue2.getFacet().getName()).isEqualTo("Speaker Category");
    }

    @Test
    @Order(4)
    public void updateFacetValues() throws IOException {
        List<UpdateFacetValueInput> inputs = new ArrayList<>();
        UpdateFacetValueInput input = new UpdateFacetValueInput();
        input.setId(speakerTypeFacet.getValues().get(0).getId());
        input.setCode("compact");
        inputs.add(input);

        JsonNode inputNode = objectMapper.valueToTree(inputs);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_FACET_VALUES, variables,
                Arrays.asList(FACET_VALUE_FRAGMENT));
        List<FacetValue> updatedFacetValues = graphQLResponse.getList("$.data.updateFacetValues", FacetValue.class);
        assertThat(updatedFacetValues.get(0).getCode()).isEqualTo("compact");
    }

    @Test
    @Order(5)
    public void facets() throws IOException {
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_FACET_LIST, null,
                        Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        FacetList facetList = graphQLResponse.get("$.data.facets", FacetList.class);
        List<Facet> items = facetList.getItems();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getName()).isEqualTo("category");
        assertThat(items.get(1).getName()).isEqualTo("Speaker Category");

        brandFacet = items.get(0);
        speakerTypeFacet = items.get(1);
    }

    @Test
    @Order(6)
    public void facet() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", speakerTypeFacet.getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_FACET_WITH_VALUES, variables,
                        Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet facet = graphQLResponse.get("$.data.facet", Facet.class);
        assertThat(facet.getName()).isEqualTo("Speaker Category");
    }

    /**
     * deletion
     */
    List<Product> products;

    private void before_test_7() throws IOException {
        // add the FacetValues to products and variants
        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCTS_LIST_WITH_VARIANTS, null);
        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);
        products = productList.getItems();

        UpdateProductInput updateProductInput = new UpdateProductInput();
        updateProductInput.setId(products.get(0).getId());
        updateProductInput.setFacetValueIds(new ArrayList<>());
        updateProductInput.getFacetValueIds().add(speakerTypeFacet.getValues().get(0).getId());

        JsonNode inputNode = objectMapper.valueToTree(updateProductInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(products.get(0).getVariants().get(0).getId());
        updateProductVariantInput.getFacetValueIds().add(speakerTypeFacet.getValues().get(0).getId());

        inputNode = objectMapper.valueToTree(updateProductVariantInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));

        updateProductInput = new UpdateProductInput();
        updateProductInput.setId(products.get(1).getId());
        updateProductInput.setFacetValueIds(new ArrayList<>());
        updateProductInput.getFacetValueIds().add(speakerTypeFacet.getValues().get(1).getId());

        inputNode = objectMapper.valueToTree(updateProductInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_PRODUCT, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
    }


    @Test
    @Order(6)
    public void deleteFacetValues_deletes_unused_facetValue() throws IOException {
        before_test_7();

        FacetValue facetValueToDelete = speakerTypeFacet.getValues().get(2);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.putArray("ids").add(facetValueToDelete.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(DELETE_FACET_VALUES, variables);
        List<DeletionResponse> deletionResponses = graphQLResponse.getList("$.data.deleteFacetValues", DeletionResponse.class);
        assertThat(deletionResponses.get(0).getResult()).isEqualTo(DeletionResult.DELETED);

        variables = objectMapper.createObjectNode();
        variables.put("id", speakerTypeFacet.getId());

        graphQLResponse =
                this.adminClient.perform(GET_FACET_WITH_VALUES, variables,
                        Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet facet = graphQLResponse.get("$.data.facet", Facet.class);
        assertThat(facet.getValues().get(0)).isNotEqualTo(facetValueToDelete);
    }

    @Test
    @Order(7)
    public void deleteFacetValues_for_FacetValue_in_use_returns_NOT_DELETED() throws IOException {
        FacetValue facetValueToDelete = speakerTypeFacet.getValues().get(0);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.putArray("ids").add(facetValueToDelete.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(DELETE_FACET_VALUES, variables);
        List<DeletionResponse> deletionResponses = graphQLResponse.getList("$.data.deleteFacetValues", DeletionResponse.class);
        assertThat(deletionResponses.get(0).getResult()).isEqualTo(DeletionResult.NOT_DELETED);
        assertThat(deletionResponses.get(0).getMessage()).isEqualTo(
                "The selected FacetValue is assigned to 1 Product(s) and 1 Variant(s)"
        );

        variables = objectMapper.createObjectNode();
        variables.put("id", speakerTypeFacet.getId());

        graphQLResponse =
                this.adminClient.perform(GET_FACET_WITH_VALUES, variables,
                        Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet facet = graphQLResponse.get("$.data.facet", Facet.class);

        FacetValue facetValue = facet.getValues().stream()
                .filter(v -> Objects.equals(facetValueToDelete.getId(), v.getId())).findFirst().get();
        assertThat(facetValue.getCode()).isEqualTo("compact");
        assertThat(facetValue.getFacet().getId()).isEqualTo(2L);
        assertThat(facetValue.getFacet().getName()).isEqualTo("Speaker Category");
        assertThat(facetValue.getId()).isEqualTo(7L);
        assertThat(facetValue.getName()).isEqualTo("Portable");
    }

    @Test
    @Order(8)
    public void deleteFacet_that_is_in_use_returns_NOT_DELETED() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", speakerTypeFacet.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(DELETE_FACET, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteFacet", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.NOT_DELETED);
        assertThat(deletionResponse.getMessage()).isEqualTo(
                "The selected Facet includes FacetValues which are assigned to 2 Product(s) and 1 Variant(s)"
        );

        variables = objectMapper.createObjectNode();
        variables.put("id", speakerTypeFacet.getId());

        graphQLResponse =
                this.adminClient.perform(GET_FACET_WITH_VALUES, variables,
                        Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet facet = graphQLResponse.get("$.data.facet", Facet.class);
        assertThat(facet).isNotNull();
    }

    @Test
    @Order(9)
    public void deleteFacet_with_no_FacetValues_works() throws IOException {
        CreateFacetInput createFacetInput = new CreateFacetInput();
        createFacetInput.setPrivateOnly(false);
        createFacetInput.setCode("test");
        createFacetInput.setName("Test");

        JsonNode inputNode = objectMapper.valueToTree(createFacetInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_FACET, variables,
                Arrays.asList(FACET_WITH_VALUES_FRAGMENT, FACET_VALUE_FRAGMENT));
        Facet createdFacet = graphQLResponse.get("$.data.createFacet", Facet.class);

        variables = objectMapper.createObjectNode();
        variables.put("id", createdFacet.getId());

        graphQLResponse = adminClient.perform(DELETE_FACET, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteFacet", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);
    }

}
