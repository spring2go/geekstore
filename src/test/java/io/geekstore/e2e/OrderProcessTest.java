/*
 * Copyright (c) 2021 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.config.payment.TestSuccessfulPaymentMethod;
import io.geekstore.config.payment_method.PaymentOptions;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.types.common.CreateAddressInput;
import io.geekstore.types.common.CreateCustomerInput;
import io.geekstore.types.order.Order;
import io.geekstore.types.payment.PaymentInput;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created on Jan, 2021 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class OrderProcessTest {

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String ADD_ITEM_TO_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_item_to_order");
    static final String SET_CUSTOMER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_customer");
    static final String SET_SHIPPING_ADDRESS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_address");
    static final String SET_SHIPPING_METHOD =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_method");
    static final String TRANSITION_TO_STATE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "transition_to_state");
    static final String ADD_PAYMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_payment");
    static final String TEST_ORDER_FRAGMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "test_order_fragment");

    static final String ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/order/%s.graphqls";
    static final String ADMIN_TRANSITION_TO_STATE  =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "admin_transition_to_state");

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_ORDER =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_order");
    static final String ORDER_WITH_LINES_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "order_with_lines_fragment");
    static final String ADJUSTMENT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "adjustment_fragment");
    static final String SHIPPING_ADDRESS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "shipping_address_fragment");
    static final String ORDER_ITEM_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "order_item_fragment");

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
    PaymentOptions paymentOptions;

    TestSuccessfulPaymentMethod testSuccessfulPaymentMethod;

    Order order;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        @Primary
        public PaymentOptions testPaymentOptions() {
            return new PaymentOptions(
                    Arrays.asList(
                            new TestSuccessfulPaymentMethod()
                    )
            );
        }
    }

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        testSuccessfulPaymentMethod = (TestSuccessfulPaymentMethod) paymentOptions.getPaymentMethodHandlers().get(0);

        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("sutest@company.com");
        input.setFirstName("Su");
        input.setLastName("Test");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_CUSTOMER, variables);

        CreateAddressInput createAddressInput = new CreateAddressInput();
        createAddressInput.setFullName("name");
        createAddressInput.setStreetLine1("12 the street");
        createAddressInput.setCity("foo");
        createAddressInput.setPostalCode("123456");
        createAddressInput.setPhoneNumber("4444444");

        inputNode = objectMapper.valueToTree(createAddressInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_SHIPPING_ADDRESS, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        shopClient.perform(SET_SHIPPING_METHOD, variables);

        variables = objectMapper.createObjectNode();
        variables.put("state", OrderState.ArrangingPayment.name());
        GraphQLResponse graphQLResponse = shopClient.perform(TRANSITION_TO_STATE, variables);
        order = graphQLResponse.get("$.data.transitionOrderToState", Order.class);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    public void cannot_manually_transition_to_PaymentAuthorized() throws IOException {
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        variables.put("state", OrderState.PaymentAuthorized.name());

        try {
            adminClient.perform(ADMIN_TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot transition Order to the \"PaymentAuthorized\" state " +
                            "when the total is not covered by authorized Payments"
            );
        }

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void cannot_manually_transition_to_PaymentSettled() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        variables.put("state", OrderState.PaymentSettled.name());

        try {
            adminClient.perform(ADMIN_TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot transition Order to the \"PaymentSettled\" state " +
                            "when the total is not covered by settled Payments"
            );
        }

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void cannot_manually_transition_to_Cancelled() throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(testSuccessfulPaymentMethod.getCode());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.addPaymentToOrder", Order.class);

        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        variables.put("state", OrderState.Cancelled.name());

        try {
            adminClient.perform(ADMIN_TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot transition Order to the \"Cancelled\" state unless all OrderItems are cancelled"
            );
        }

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void cannot_manually_transition_to_PartiallyFulfilled() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        variables.put("state", OrderState.PartiallyFulfilled.name());

        try {
            adminClient.perform(ADMIN_TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot transition Order to the \"PartiallyFulfilled\" state unless some OrderItems are fulfilled"
            );
        }

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void cannot_manually_transition_to_Fulfilled() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        variables.put("state", OrderState.Fulfilled.name());

        try {
            adminClient.perform(ADMIN_TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot transition Order to the \"Fulfilled\" state unless all OrderItems are fulfilled"
            );
        }

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
    }
}
