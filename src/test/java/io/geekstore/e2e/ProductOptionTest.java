/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ProductOptionTest {

    static final String ADMIN_PRODUCT_OPTION_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/product_option/%s.graphqls";
    static final String CREATE_PRODUCT_OPTION_GROUP =
            String.format(ADMIN_PRODUCT_OPTION_GRAPHQL_RESOURCE_TEMPLATE, "create_product_option_group");
    static final String PRODUCT_OPTION_GROUP_FRAGMENT =
            String.format(ADMIN_PRODUCT_OPTION_GRAPHQL_RESOURCE_TEMPLATE, "product_option_group_fragment");
    static final String UPDATE_PRODUCT_OPTION_GROUP =
            String.format(ADMIN_PRODUCT_OPTION_GRAPHQL_RESOURCE_TEMPLATE, "update_product_option_group");
    static final String CREATE_PRODUCT_OPTION =
            String.format(ADMIN_PRODUCT_OPTION_GRAPHQL_RESOURCE_TEMPLATE, "create_product_option");

    static final String UPDATE_PRODUCT_OPTION =
            String.format(ADMIN_PRODUCT_OPTION_GRAPHQL_RESOURCE_TEMPLATE, "update_product_option");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    ProductOptionGroup sizeGroup;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }


    @Test
    @Order(1)
    public void create_product_option_group() throws IOException {
        CreateProductOptionGroupInput createProductOptionGroupInput =
                new CreateProductOptionGroupInput();
        createProductOptionGroupInput.setCode("size");
        createProductOptionGroupInput.setName("Size");
        CreateGroupOptionInput createGroupOptionInput = new CreateGroupOptionInput();
        createGroupOptionInput.setCode("small");
        createGroupOptionInput.setName("Small");
        createProductOptionGroupInput.getOptions().add(createGroupOptionInput);
        createGroupOptionInput = new CreateGroupOptionInput();
        createGroupOptionInput.setCode("large");
        createGroupOptionInput.setName("Large");
        createProductOptionGroupInput.getOptions().add(createGroupOptionInput);

        JsonNode inputNode = objectMapper.valueToTree(createProductOptionGroupInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(
                        CREATE_PRODUCT_OPTION_GROUP, variables, Arrays.asList(PRODUCT_OPTION_GROUP_FRAGMENT));
        sizeGroup = graphQLResponse.get("$.data.createProductOptionGroup", ProductOptionGroup.class);
        assertThat(sizeGroup.getId()).isEqualTo(3L);
        assertThat(sizeGroup.getName()).isEqualTo("Size");
        assertThat(sizeGroup.getCode()).isEqualTo("size");
    }

    @Test
    @Order(2)
    public void update_product_option_group() throws IOException {
        UpdateProductOptionGroupInput updateProductOptionGroupInput = new UpdateProductOptionGroupInput();
        updateProductOptionGroupInput.setId(sizeGroup.getId());
        updateProductOptionGroupInput.setName("Bigness");

        JsonNode inputNode = objectMapper.valueToTree(updateProductOptionGroupInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(
                        UPDATE_PRODUCT_OPTION_GROUP, variables, Arrays.asList(PRODUCT_OPTION_GROUP_FRAGMENT));
        ProductOptionGroup productOptionGroup =
                graphQLResponse.get("$.data.updateProductOptionGroup", ProductOptionGroup.class);
        assertThat(productOptionGroup.getName()).isEqualTo("Bigness");
    }

    @Test
    @Order(3)
    public void createProductOption_throws_with_invalid_productOptionGroupId() throws IOException {
        CreateProductOptionInput createProductOptionInput = new CreateProductOptionInput();
        createProductOptionInput.setProductOptionGroupId(999L);
        createProductOptionInput.setCode("medium");
        createProductOptionInput.setName("Medium");

        JsonNode inputNode = objectMapper.valueToTree(createProductOptionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CREATE_PRODUCT_OPTION, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage())
                    .isEqualTo("No ProductOptionGroupEntity with the id '999' could be found");
        }
    }

    @Test
    @Order(4)
    public void createProductOption() throws IOException {
        CreateProductOptionInput createProductOptionInput = new CreateProductOptionInput();
        createProductOptionInput.setProductOptionGroupId(sizeGroup.getId());
        createProductOptionInput.setCode("medium");
        createProductOptionInput.setName("Medium");

        JsonNode inputNode = objectMapper.valueToTree(createProductOptionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(CREATE_PRODUCT_OPTION, variables);
        ProductOption productOption = graphQLResponse.get("$.data.createProductOption", ProductOption.class);
        assertThat(productOption.getId()).isEqualTo(7L);
        assertThat(productOption.getGroupId()).isEqualTo(sizeGroup.getId());
        assertThat(productOption.getCode()).isEqualTo("medium");
        assertThat(productOption.getName()).isEqualTo("Medium");
    }

    @Test
    @Order(5)
    public void updateProductOption() throws IOException {
        UpdateProductOptionInput updateProductOptionInput = new UpdateProductOptionInput();
        updateProductOptionInput.setId(7L);
        updateProductOptionInput.setName("Middling");

        JsonNode inputNode = objectMapper.valueToTree(updateProductOptionInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_PRODUCT_OPTION, variables);
        ProductOption productOption = graphQLResponse.get("$.data.updateProductOption", ProductOption.class);
        assertThat(productOption.getName()).isEqualTo("Middling");
    }

}
