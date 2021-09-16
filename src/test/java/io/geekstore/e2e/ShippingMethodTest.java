/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.TestConfig;
import io.geekstore.config.shipping_method.*;
import io.geekstore.entity.OrderEntity;
import io.geekstore.types.common.*;
import io.geekstore.types.shipping.*;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ShippingMethodTest {

    static final String SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/shipping_method/%s.graphqls";
    static final String GET_ELIGIBILITY_CHECKERS =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "get_eligibility_checkers");
    static final String GET_CALCULATORS =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "get_calculators");
    static final String SHIPPING_METHOD_FRAGMENT =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "shipping_method_fragment");
    static final String GET_SHIPPING_METHOD_LIST =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "get_shipping_method_list");
    static final String GET_SHIPPING_METHOD =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "get_shipping_method");
    static final String CREATE_SHIPPING_METHOD =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "create_shipping_method");
    static final String TEST_SHIPPING_METHOD =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "test_shipping_method");
    static final String TEST_ELIGIBLE_SHIPPING_METHODS =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "test_eligible_shipping_methods");
    static final String UPDATE_SHIPPING_METHOD =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "update_shipping_method");
    static final String DELETE_SHIPPING_METHOD =
            String.format(SHIPPING_METHOD_GRAPHQL_RESOURCE_TEMPLATE, "delete_shipping_method");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    @Autowired
    ShippingOptions shippingOptions;

    ShippingEligibilityChecker defaultShippingEligibilityChecker;
    ShippingCalculator calculatorWithMetadata;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        @Primary
        public ShippingOptions testShippingOptions() {
            return new ShippingOptions(
                    Arrays.asList(new DefaultShippingEligibilityChecker()),
                    Arrays.asList(
                            new DefaultShippingCalculator(),
                            calculatorWithMetadata()
                    )
            );
        }

        Map<String, String> TEST_METADATA = ImmutableMap.of("foo", "bar", "baz", "1,2,3");

        ShippingCalculator calculatorWithMetadata() {
            return new ShippingCalculator(
                    "calculator-with-metadata",
                    "Has metadata") {
                @Override
                public ShippingCalculationResult calculate(OrderEntity orderEntity, ConfigArgValues argValues) {
                    ShippingCalculationResult result = new ShippingCalculationResult();
                    result.setPrice(100);
                    result.setMetadata(TEST_METADATA);
                    return result;
                }

                @Override
                public Map<String, ConfigArgDefinition> getArgSpec() {
                    return ImmutableMap.of();
                }
            };
        }
    }

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        defaultShippingEligibilityChecker = shippingOptions.getShippingEligibilityCheckers().get(0);
        calculatorWithMetadata = shippingOptions.getShippingCalculators().get(1);
    }


    @Test
    @Order(1)
    public void shippingEligibilityCheckers() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_ELIGIBILITY_CHECKERS, null);
        List<ConfigurableOperationDefinition> shippingEligibilityCheckers = graphQLResponse
                .getList("$.data.shippingEligibilityCheckers", ConfigurableOperationDefinition.class);
        assertThat(shippingEligibilityCheckers).hasSize(1);
        ConfigurableOperationDefinition cod = shippingEligibilityCheckers.get(0);
        assertThat(cod.getCode()).isEqualTo("default-shipping-eligibility-checker");
        assertThat(cod.getDescription()).isEqualTo("Default Shipping Eligibility Checker");
        assertThat(cod.getArgs()).hasSize(1);
        ConfigArgDefinition cad = cod.getArgs().get(0);
        assertThat(cad.getDescription())
                .isEqualTo("Order is eligible only if its total is greater or equal to this value");
        assertThat(cad.getLabel()).isEqualTo("Minimum order value");
        assertThat(cad.getName()).isEqualTo("orderMinimum");
        assertThat(cad.getType()).isEqualTo("int");
        assertThat(cad.getUi().get("component")).isEqualTo("currency-form-input");
    }

    @Test
    @Order(2)
    public void shippingCalculators() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_CALCULATORS, null);
        List<ConfigurableOperationDefinition> shippingCalculators = graphQLResponse
                .getList("$.data.shippingCalculators", ConfigurableOperationDefinition.class);
        assertThat(shippingCalculators).hasSize(2);
        ConfigurableOperationDefinition cod1 = shippingCalculators.get(0);
        assertThat(cod1.getCode()).isEqualTo("default-shipping-calculator");
        assertThat(cod1.getDescription()).isEqualTo("Default Flat-Rate Shipping Calculator");
        assertThat(cod1.getArgs()).hasSize(1);
        ConfigArgDefinition cad = cod1.getArgs().get(0);
        assertThat(cad.getType()).isEqualTo("int");
        assertThat(cad.getName()).isEqualTo("rate");
        assertThat(cad.getLabel()).isEqualTo("Shipping price");
        assertThat(cad.getDescription()).isNull();
        assertThat(cad.getUi().get("component")).isEqualTo("currency-form-input");

        ConfigurableOperationDefinition cod2 = shippingCalculators.get(1);
        assertThat(cod2.getCode()).isEqualTo("calculator-with-metadata");
        assertThat(cod2.getDescription()).isEqualTo("Has metadata");
        assertThat(cod2.getArgs()).isEmpty();
    }

    @Test
    @Order(3)
    public void shippingMethods() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_SHIPPING_METHOD_LIST, null, Arrays.asList(SHIPPING_METHOD_FRAGMENT));
        ShippingMethodList shippingMethodList =
                graphQLResponse.get("$.data.shippingMethods", ShippingMethodList.class);
        assertThat(shippingMethodList.getTotalItems()).isEqualTo(2);
        assertThat(shippingMethodList.getItems().get(0).getCode()).isEqualTo("standard-shipping");
        assertThat(shippingMethodList.getItems().get(1).getCode()).isEqualTo("express-shipping");
    }

    @Test
    @Order(4)
    public void shippingMethod() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_SHIPPING_METHOD, variables, Arrays.asList(SHIPPING_METHOD_FRAGMENT));
        ShippingMethod shippingMethod = graphQLResponse.get("$.data.shippingMethod", ShippingMethod.class);
        assertThat(shippingMethod.getCode()).isEqualTo("standard-shipping");
    }

    @Test
    @Order(5)
    public void createShippingMethod() throws IOException {
        CreateShippingMethodInput input = new CreateShippingMethodInput();
        ConfigurableOperationInput checker = new ConfigurableOperationInput();
        checker.setCode(defaultShippingEligibilityChecker.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("orderMinimum");
        configArgInput.setValue("0");
        checker.getArguments().add(configArgInput);
        input.setChecker(checker);

        ConfigurableOperationInput calculator = new ConfigurableOperationInput();
        calculator.setCode(calculatorWithMetadata.getCode());
        input.setCalculator(calculator);

        input.setDescription("new method");
        input.setCode("new-method");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(CREATE_SHIPPING_METHOD, variables, Arrays.asList(SHIPPING_METHOD_FRAGMENT));
        ShippingMethod createdShippingMethod =
                graphQLResponse.get("$.data.createShippingMethod", ShippingMethod.class);
        assertThat(createdShippingMethod.getId()).isEqualTo(3L);
        assertThat(createdShippingMethod.getCode()).isEqualTo("new-method");
        assertThat(createdShippingMethod.getDescription()).isEqualTo("new method");
        assertThat(createdShippingMethod.getCalculator().getCode()).isEqualTo("calculator-with-metadata");
        assertThat(createdShippingMethod.getChecker().getCode()).isEqualTo("default-shipping-eligibility-checker");
    }

    @Test
    @Order(6)
    public void testShippingMethod() throws IOException {
        TestShippingMethodInput input = new TestShippingMethodInput();
        ConfigurableOperationInput checker = new ConfigurableOperationInput();
        checker.setCode(defaultShippingEligibilityChecker.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("orderMinimum");
        configArgInput.setValue("0");
        checker.getArguments().add(configArgInput);
        input.setChecker(checker);

        ConfigurableOperationInput calculator = new ConfigurableOperationInput();
        calculator.setCode(calculatorWithMetadata.getCode());
        input.setCalculator(calculator);

        TestShippingMethodOrderLineInput testShippingMethodOrderLineInput = new TestShippingMethodOrderLineInput();
        testShippingMethodOrderLineInput.setProductVariantId(1L);
        testShippingMethodOrderLineInput.setQuantity(1);
        input.getLines().add(testShippingMethodOrderLineInput);

        CreateAddressInput createAddressInput = new CreateAddressInput();
        createAddressInput.setStreetLine1("");
        input.setShippingAddress(createAddressInput);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(TEST_SHIPPING_METHOD, variables);
        TestShippingMethodResult testShippingMethodResult =
                graphQLResponse.get("$.data.testShippingMethod", TestShippingMethodResult.class);
        assertThat(testShippingMethodResult.getEligible()).isTrue();
        assertThat(testShippingMethodResult.getQuote().getPrice()).isEqualTo(100);
        assertThat(testShippingMethodResult.getQuote().getMetadata()).hasSize(2);
        assertThat(testShippingMethodResult.getQuote().getMetadata()).containsEntry("foo", "bar");
        assertThat(testShippingMethodResult.getQuote().getMetadata()).containsEntry("baz", "1,2,3");
    }


    @Test
    @Order(7)
    public void testEligibleShippingMethods() throws IOException {
        TestEligibleShippingMethodsInput input = new TestEligibleShippingMethodsInput();
        CreateAddressInput createAddressInput = new CreateAddressInput();
        createAddressInput.setStreetLine1("");
        input.setShippingAddress(createAddressInput);
        TestShippingMethodOrderLineInput testShippingMethodOrderLineInput =
                new TestShippingMethodOrderLineInput();
        testShippingMethodOrderLineInput.setProductVariantId(1L);
        testShippingMethodOrderLineInput.setQuantity(1);
        input.getLines().add(testShippingMethodOrderLineInput);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(TEST_ELIGIBLE_SHIPPING_METHODS, variables);
        List<ShippingMethodQuote> shippingMethodQuotes =
                graphQLResponse.getList("$.data.testEligibleShippingMethods", ShippingMethodQuote.class);
        assertThat(shippingMethodQuotes).hasSize(3);

        ShippingMethodQuote quote1 = shippingMethodQuotes.get(0);
        assertThat(quote1.getId()).isEqualTo(3L);
        assertThat(quote1.getDescription()).isEqualTo("new method");
        assertThat(quote1.getPrice()).isEqualTo(100);
        assertThat(quote1.getMetadata()).containsExactly(entry("foo", "bar"), entry("baz", "1,2,3"));

        ShippingMethodQuote quote2 = shippingMethodQuotes.get(1);
        assertThat(quote2.getId()).isEqualTo(1L);
        assertThat(quote2.getDescription()).isEqualTo("Standard Shipping");
        assertThat(quote2.getPrice()).isEqualTo(500);
        assertThat(quote2.getMetadata()).isEmpty();

        ShippingMethodQuote quote3 = shippingMethodQuotes.get(2);
        assertThat(quote3.getId()).isEqualTo(2L);
        assertThat(quote3.getDescription()).isEqualTo("Express Shipping");
        assertThat(quote3.getPrice()).isEqualTo(1000);
        assertThat(quote3.getMetadata()).isEmpty();
    }

    @Test
    @Order(8)
    public void updateShippingMethod() throws IOException {
        UpdateShippingMethodInput input = new UpdateShippingMethodInput();
        input.setId(3L);
        input.setDescription("changed method");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_SHIPPING_METHOD, variables, Arrays.asList(SHIPPING_METHOD_FRAGMENT));
        ShippingMethod shippingMethod = graphQLResponse.get("$.data.updateShippingMethod", ShippingMethod.class);
        assertThat(shippingMethod.getDescription()).isEqualTo("changed method");
    }

    @Test
    @Order(9)
    public void deleteShippingMethod() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_SHIPPING_METHOD_LIST, null, Arrays.asList(SHIPPING_METHOD_FRAGMENT));
        ShippingMethodList shippingMethodList =
                graphQLResponse.get("$.data.shippingMethods", ShippingMethodList.class);
        assertThat(shippingMethodList.getItems().stream().map(ShippingMethod::getId).collect(Collectors.toList()))
                .containsExactly(1L, 2L, 3L);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 3L);

        graphQLResponse = adminClient.perform(DELETE_SHIPPING_METHOD, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteShippingMethod", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        graphQLResponse =
                adminClient.perform(GET_SHIPPING_METHOD_LIST, null, Arrays.asList(SHIPPING_METHOD_FRAGMENT));
        shippingMethodList =
                graphQLResponse.get("$.data.shippingMethods", ShippingMethodList.class);
        assertThat(shippingMethodList.getItems().stream().map(ShippingMethod::getId).collect(Collectors.toList()))
                .containsExactly(1L, 2L);
    }
}
