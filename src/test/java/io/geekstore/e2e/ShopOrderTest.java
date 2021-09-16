/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.config.payment.TestErrorPaymentMethod;
import io.geekstore.config.payment.TestFailingPaymentMethod;
import io.geekstore.config.payment.TestSuccessfulPaymentMethod;
import io.geekstore.config.payment_method.PaymentOptions;
import io.geekstore.options.ConfigOptions;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.types.address.Address;
import io.geekstore.types.common.CreateAddressInput;
import io.geekstore.types.common.CreateCustomerInput;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.customer.CustomerListOptions;
import io.geekstore.types.order.Order;
import io.geekstore.types.order.OrderAddress;
import io.geekstore.types.payment.Payment;
import io.geekstore.types.payment.PaymentInput;
import io.geekstore.types.shipping.ShippingMethodQuote;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ShopOrderTest {

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String ADD_ITEM_TO_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_item_to_order");
    static final String TEST_ORDER_FRAGMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "test_order_fragment");
    static final String GET_ACTIVE_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_order");
    static final String ADJUST_ITEM_QUANTITY  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "adjust_item_quantity");
    static final String REMOVE_ITEM_FROM_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "remove_item_from_order");
    static final String GET_NEXT_STATES =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_next_states");
    static final String TRANSITION_TO_STATE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "transition_to_state");
    static final String SET_CUSTOMER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_customer");
    static final String SET_SHIPPING_ADDRESS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_address");
    static final String SET_BILLING_ADDRESS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_billing_address");
    static final String ADD_PAYMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_payment");
    static final String GET_ELIGIBLE_SHIPPING_METHODS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_eligible_shipping_methods");
    static final String SET_SHIPPING_METHOD =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_method");
    static final String GET_ACTIVE_ORDER_PAYMENTS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_order_payments");
    static final String GET_ORDER_BY_CODE =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_order_by_code");
    static final String GET_ACTIVE_ORDER_ADDRESSES =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_order_address");
    static final String GET_ACTIVE_ORDER_ORDERS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_order_orders");

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_CUSTOMER =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer");
    static final String CUSTOMER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "customer_fragment");
    static final String ADDRESS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "address_fragment");
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String ATTEMPT_LOGIN =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "attempt_login");
    static final String CURRENT_USER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "current_user_fragment");

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

    @Autowired
    ConfigOptions configOptions;

    TestSuccessfulPaymentMethod testSuccessfulPaymentMethod;
    TestFailingPaymentMethod testFailingPaymentMethod;
    TestErrorPaymentMethod testErrorPaymentMethod;

    @TestConfiguration
    static class ContextConfiguration {
        @Bean
        @Primary
        public PaymentOptions testPaymentOptions() {
            return new PaymentOptions(
                    Arrays.asList(
                            new TestSuccessfulPaymentMethod(),
                            new TestFailingPaymentMethod(),
                            new TestErrorPaymentMethod()
                    )
            );
        }
    }

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(3).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        configOptions.getOrderOptions().setOrderItemsLimit(99);
        testSuccessfulPaymentMethod = (TestSuccessfulPaymentMethod) paymentOptions.getPaymentMethodHandlers().get(0);
        testFailingPaymentMethod = (TestFailingPaymentMethod) paymentOptions.getPaymentMethodHandlers().get(1);
        testErrorPaymentMethod = (TestErrorPaymentMethod) paymentOptions.getPaymentMethodHandlers().get(2);
    }


    /**
     * ordering as anonymous user
     */
    Long firstOrderLineId;
    Long createdCustomerId;
    String orderCode;

    @Test
    @org.junit.jupiter.api.Order(1)
    public void addItemToOrder_starts_with_no_session_token() {
        assertThat(shopClient.getAuthToken()).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void activeOrder_returns_null_before_any_items_have_been_added() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        io.geekstore.types.order.Order activeOrder =
                graphQLResponse.get("$.data.activeOrder", io.geekstore.types.order.Order.class);
        assertThat(activeOrder).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void activeOrder_creates_an_anonymous_session() throws IOException {
        assertThat(shopClient.getAuthToken()).isNotNull();
        assertThat(shopClient.getAuthToken()).isNotBlank();
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void addItemToOrder_creates_a_new_Order_with_an_item() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(1L);
        assertThat(order.getLines().get(0).getProductVariant().getId()).isEqualTo(1L);
        assertThat(order.getLines().get(0).getId()).isEqualTo(1L);
        firstOrderLineId = order.getLines().get(0).getId();
        orderCode = order.getCode();
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void addItemToOrder_errors_with_an_invalid_productVariantId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 999L);
        variables.put("quantity", 1);
        try {
            shopClient.perform(ADD_ITEM_TO_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No ProductVariant with the id '999' could be found"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void addItemToOrder_errors_with_a_negative_quantity() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 999L);
        variables.put("quantity", -3);
        try {
            shopClient.perform(ADD_ITEM_TO_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "{ -3 } is not a valid quantity for an OrderItem"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    public void addItemToOrder_with_an_existing_productVariantId_adds_quantity_to_the_existing_OrderLine()
            throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 2);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    public void addItemToOrder_errors_when_going_beyond_orderItemsLimit() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 100);
        try {
            shopClient.perform(ADD_ITEM_TO_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot add items. An order may consist of a maximum of { 99 } items"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    public void adjustOrderLine_adjusts_the_quantity() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        variables.put("quantity", 50);
        GraphQLResponse graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    public void adjustOrderLine_with_quantity_0_removes_the_line() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 3L);
        variables.put("quantity", 2);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().stream().map(i -> i.getProductVariant().getId()).collect(Collectors.toList()))
                .containsExactly(1L, 3L);

        variables = objectMapper.createObjectNode();
        variables.put("orderLineId", order.getLines().get(1).getId());
        variables.put("quantity", 0);
        graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().stream().map(i -> i.getProductVariant().getId()).collect(Collectors.toList()))
                .containsExactly(1L);
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    public void adjustOrderLine_errors_when_going_beyond_orderItemsLimit() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        variables.put("quantity", 100);
        try {
            shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo(
                    "Cannot add items. An order may consist of a maximum of { 99 } items"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    public void adjustOrderLine_errors_with_a_negative_quantity() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        variables.put("quantity", -3);
        try {
            shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo(
                    "{ -3 } is not a valid quantity for an OrderItem"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    public void adjustOrderLine_errors_with_an_invalid_orderLineId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", 999L);
        variables.put("quantity", 5);
        try {
            shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo(
                    "This order does not contain an OrderLine with the id { 999 }"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(14)
    public void removeItemFromOrder_removes_the_correct_item() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 3L);
        variables.put("quantity", 3);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().stream().map(i -> i.getProductVariant().getId()).collect(Collectors.toList()))
                .containsExactly(1L, 3L);

        variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        graphQLResponse = shopClient.perform(REMOVE_ITEM_FROM_ORDER, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.removeOrderLine", Order.class);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().stream().map(i -> i.getProductVariant().getId()).collect(Collectors.toList()))
                .containsExactly(3L);
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    public void removeItemFromOrder_errors_with_an_invalid_orderItemId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", 999L);
        try {
            shopClient.perform(REMOVE_ITEM_FROM_ORDER, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
              "This order does not contain an OrderLine with the id { 999 }"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(16)
    public void nextOrderStates_returns_next_valid_states() throws IOException {
        GraphQLResponse graphQLResponse = shopClient.perform(GET_NEXT_STATES, null);
        List<String> states = graphQLResponse.getList("$.data.nextOrderStates", String.class);
        assertThat(states).containsExactly("ArrangingPayment", "Cancelled");
    }

    @Test
    @org.junit.jupiter.api.Order(17)
    public void transitionOrderToState_throws_for_an_invalid_state() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("state", "Completed");
        try {
            shopClient.perform(TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No enum constant io.geekstore.service.helpers.order_state_machine.OrderState.Completed"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(18)
    public void attempting_to_transition_to_ArrangingPayment_throws_when_Order_has_no_Customer() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("state", "ArrangingPayment");
        try {
            shopClient.perform(TRANSITION_TO_STATE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot transition Order to the \"ArrangingPayment\" state without Customer details"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(19)
    public void setCustomerForOrder_creates_a_new_Customer_and_associates_it_with_the_Order() throws IOException {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test@test.com");
        input.setFirstName("Test");
        input.setLastName("Person");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SET_CUSTOMER, variables);
        Order order = graphQLResponse.get("$.data.setCustomerForOrder", Order.class);

        Customer customer = order.getCustomer();
        assertThat(customer.getFirstName()).isEqualTo("Test");
        assertThat(customer.getLastName()).isEqualTo("Person");
        assertThat(customer.getEmailAddress()).isEqualTo("test@test.com");
        createdCustomerId = customer.getId();
    }

    @Test
    @org.junit.jupiter.api.Order(20)
    public void setCustomerForOrder_updates_the_existing_customer_if_Customer_already_set() throws IOException {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test@test.com");
        input.setFirstName("Changed");
        input.setLastName("Person");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SET_CUSTOMER, variables);
        Order order = graphQLResponse.get("$.data.setCustomerForOrder", Order.class);
        Customer customer = order.getCustomer();
        assertThat(customer.getFirstName()).isEqualTo("Changed");
        assertThat(customer.getLastName()).isEqualTo("Person");
        assertThat(customer.getEmailAddress()).isEqualTo("test@test.com");
        assertThat(customer.getId()).isEqualTo(createdCustomerId);
    }

    @Test
    @org.junit.jupiter.api.Order(21)
    public void setOrderShippingAddress_sets_shipping_address() throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setFullName("name");
        input.setCompany("company");
        input.setStreetLine1("12 the street");
        input.setStreetLine2(null);
        input.setCity("foo");
        input.setProvince("bar");
        input.setPostalCode("123456");
        input.setPhoneNumber("4444444");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SET_SHIPPING_ADDRESS, variables);
        Order order = graphQLResponse.get("$.data.setOrderShippingAddress", Order.class);

        OrderAddress orderAddress = order.getShippingAddress();
        assertThat(orderAddress.getFullName()).isEqualTo("name");
        assertThat(orderAddress.getCompany()).isEqualTo("company");
        assertThat(orderAddress.getStreetLine1()).isEqualTo("12 the street");
        assertThat(orderAddress.getStreetLine2()).isNull();
        assertThat(orderAddress.getCity()).isEqualTo("foo");
        assertThat(orderAddress.getProvince()).isEqualTo("bar");
        assertThat(orderAddress.getPostalCode()).isEqualTo("123456");
        assertThat(orderAddress.getPhoneNumber()).isEqualTo("4444444");
    }

    @Test
    @org.junit.jupiter.api.Order(22)
    public void setOrderBillingAddress_sets_billing_address() throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setFullName("name");
        input.setCompany("company");
        input.setStreetLine1("12 the street");
        input.setStreetLine2(null);
        input.setCity("foo");
        input.setProvince("bar");
        input.setPostalCode("123456");
        input.setPhoneNumber("4444444");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SET_BILLING_ADDRESS, variables);
        Order order = graphQLResponse.get("$.data.setOrderBillingAddress", Order.class);

        OrderAddress orderAddress = order.getBillingAddress();
        assertThat(orderAddress.getFullName()).isEqualTo("name");
        assertThat(orderAddress.getCompany()).isEqualTo("company");
        assertThat(orderAddress.getStreetLine1()).isEqualTo("12 the street");
        assertThat(orderAddress.getStreetLine2()).isNull();
        assertThat(orderAddress.getCity()).isEqualTo("foo");
        assertThat(orderAddress.getProvince()).isEqualTo("bar");
        assertThat(orderAddress.getPostalCode()).isEqualTo("123456");
        assertThat(orderAddress.getPhoneNumber()).isEqualTo("4444444");
    }

    @Test
    @org.junit.jupiter.api.Order(23)
    public void customer_default_Addresses_are_not_updated_before_payment() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", activeOrder.getCustomer().getId());

        graphQLResponse =
                adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getAddresses()).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(24)
    public void can_transition_to_ArrangingPayment_once_Customer_has_been_set() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("state", "ArrangingPayment");
        GraphQLResponse graphQLResponse = shopClient.perform(TRANSITION_TO_STATE, variables);
        Order order = graphQLResponse.get("$.data.transitionOrderToState", Order.class);
        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());
    }

    @Test
    @org.junit.jupiter.api.Order(25)
    public void adds_a_successful_payment_and_transitions_Order_state() throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(testSuccessfulPaymentMethod.getCode());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.addPaymentToOrder", Order.class);

        Payment payment = order.getPayments().get(0);
        assertThat(order.getState()).isEqualTo("PaymentSettled");
        assertThat(order.getActive()).isFalse();
        assertThat(order.getPayments()).hasSize(1);
        assertThat(payment.getMethod()).isEqualTo(testSuccessfulPaymentMethod.getCode());
        assertThat(payment.getState()).isEqualTo("Settled");
    }

    @Test
    @org.junit.jupiter.api.Order(26)
    public void activeOrder_is_null_after_payment() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);
        assertThat(activeOrder).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(27)
    public void customer_default_Addresses_are_updated_after_payment() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdCustomerId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);

        Address address = customer.getAddresses().get(0);
        assertThat(address.getStreetLine1()).isEqualTo("12 the street");
        assertThat(address.getPostalCode()).isEqualTo("123456");
        assertThat(address.getDefaultBillingAddress()).isTrue();
        assertThat(address.getDefaultShippingAddress()).isTrue();
    }

    Order activeOrder;
    String authenticatedUserEmailAddress;
    List<Customer> customers;
    String password = MockDataService.TEST_PASSWORD;

    private void before_test_28() throws IOException {
        adminClient.asSuperAdmin();

        CustomerListOptions options = new CustomerListOptions();
        options.setCurrentPage(1);
        options.setPageSize(2);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_CUSTOMER_LIST, variables);
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customers = customerList.getItems();
        authenticatedUserEmailAddress = customers.get(0).getEmailAddress();
        shopClient.asUserWithCredentials(authenticatedUserEmailAddress, password);
    }

    @Test
    @org.junit.jupiter.api.Order(28)
    public void activeOrder_returns_null_before_any_item_have_been_added() throws IOException {
        before_test_28();

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(29)
    public void addItemToOrder_creates_a_new_Order_with_an_item_02() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(1L);
        assertThat(order.getLines().get(0).getProductVariant().getId()).isEqualTo(1L);

        activeOrder = order;
        firstOrderLineId = order.getLines().get(0).getId();
    }

    @Test
    @org.junit.jupiter.api.Order(30)
    public void activeOrder_returns_order_after_item_has_been_added() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getId()).isEqualTo(activeOrder.getId());
        assertThat(activeOrder.getState()).isEqualTo(OrderState.AddingItems.name());
    }

    @Test
    @org.junit.jupiter.api.Order(31)
    public void activeOrder_resolve_customer_user() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getCustomer().getUser().getId()).isEqualTo(2L);
        assertThat(activeOrder.getCustomer().getUser().getIdentifier()).isEqualTo(authenticatedUserEmailAddress);
    }

    @Test
    @org.junit.jupiter.api.Order(32)
    public void addItemToOrder_with_an_existing_productVariantId_adds_quantity_to_the_existing_OrderLine_02()
            throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 2);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @org.junit.jupiter.api.Order(33)
    public void adjustOrderLine_adjusts_the_quantity_02() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        variables.put("quantity", 50);
        GraphQLResponse graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    @org.junit.jupiter.api.Order(34)
    public void removeItemFromOrder_removes_the_correct_item_02() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 3L);
        variables.put("quantity", 3);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getLines().stream().map(i -> i.getProductVariant().getId()).collect(Collectors.toList()))
                .containsExactly(1L, 3L);

        variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        graphQLResponse = shopClient.perform(REMOVE_ITEM_FROM_ORDER, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.removeOrderLine", Order.class);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().stream().map(i -> i.getProductVariant().getId()).collect(Collectors.toList()))
                .containsExactly(3L);
    }

    @Test
    @org.junit.jupiter.api.Order(35)
    public void nextOrderStates_returns_next_valid_states_02() throws IOException {
        GraphQLResponse graphQLResponse = shopClient.perform(GET_NEXT_STATES, null);
        List<String> states = graphQLResponse.getList("$.data.nextOrderStates", String.class);
        assertThat(states).containsExactly("ArrangingPayment", "Cancelled");
    }

    @Test
    @org.junit.jupiter.api.Order(36)
    public void logging_out_and_back_in_again_resumes_the_last_active_order() throws IOException {
        shopClient.asAnonymousUser();
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);
        assertThat(activeOrder).isNull();

        shopClient.asUserWithCredentials(authenticatedUserEmailAddress, password);
        graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);
        assertThat(activeOrder.getId()).isEqualTo(activeOrder.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(37)
    public void cannot_setCustomerForOrder_when_already_logged_in() throws IOException {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("newperson@email.com");
        input.setFirstName("New");
        input.setLastName("Person");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            shopClient.perform(SET_CUSTOMER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot set a Customer for the Order when already logged in"
            );
        }
    }

    List<ShippingMethodQuote> shippingMethods;

    /**
     * shipping
     */
    @Test
    @org.junit.jupiter.api.Order(38)
    public void setOrderShippingAddress_sets_shipping_address_02() throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setFullName("name");
        input.setCompany("company");
        input.setStreetLine1("12 the street");
        input.setStreetLine2(null);
        input.setCity("foo");
        input.setProvince("bar");
        input.setPostalCode("123456");
        input.setPhoneNumber("4444444");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(SET_SHIPPING_ADDRESS, variables);
        Order order = graphQLResponse.get("$.data.setOrderShippingAddress", Order.class);

        OrderAddress orderAddress = order.getShippingAddress();
        assertThat(orderAddress.getFullName()).isEqualTo("name");
        assertThat(orderAddress.getCompany()).isEqualTo("company");
        assertThat(orderAddress.getStreetLine1()).isEqualTo("12 the street");
        assertThat(orderAddress.getStreetLine2()).isNull();
        assertThat(orderAddress.getCity()).isEqualTo("foo");
        assertThat(orderAddress.getProvince()).isEqualTo("bar");
        assertThat(orderAddress.getPostalCode()).isEqualTo("123456");
        assertThat(orderAddress.getPhoneNumber()).isEqualTo("4444444");
    }

    @Test
    @org.junit.jupiter.api.Order(39)
    public void eligibleShippingMethods_lists_shipping_methods() throws IOException {
        GraphQLResponse graphQLResponse = shopClient.perform(GET_ELIGIBLE_SHIPPING_METHODS, null);
        shippingMethods  =
                graphQLResponse.getList("$.data.eligibleShippingMethods", ShippingMethodQuote.class);
        ShippingMethodQuote shippingMethod = shippingMethods.get(0);
        assertThat(shippingMethod.getId()).isEqualTo(1);
        assertThat(shippingMethod.getPrice()).isEqualTo(500);
        assertThat(shippingMethod.getDescription()).isEqualTo("Standard Shipping");
        shippingMethod = shippingMethods.get(1);
        assertThat(shippingMethod.getId()).isEqualTo(2);
        assertThat(shippingMethod.getPrice()).isEqualTo(1000);
        assertThat(shippingMethod.getDescription()).isEqualTo("Express Shipping");
    }

    @Test
    @org.junit.jupiter.api.Order(40)
    public void shipping_is_initially_unset() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);
        assertThat(activeOrder.getShipping()).isEqualTo(0);
        assertThat(activeOrder.getShippingMethod()).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(41)
    public void setOrderShippingMethod_sets_the_shipping_method() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", shippingMethods.get(1).getId());

        shopClient.perform(SET_SHIPPING_METHOD, variables);

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getShipping()).isEqualTo(shippingMethods.get(1).getPrice());
        assertThat(activeOrder.getShippingMethod().getId()).isEqualTo(shippingMethods.get(1).getId());
        assertThat(activeOrder.getShippingMethod().getDescription()).isEqualTo(
                shippingMethods.get(1).getDescription()
        );
    }

    @Test
    @org.junit.jupiter.api.Order(42)
    public void shipping_method_is_preserved_after_adjustOrderLine() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", activeOrder.getLines().get(0).getId());
        variables.put("quantity", 10);
        graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);

        assertThat(order.getShipping()).isEqualTo(shippingMethods.get(1).getPrice());
        assertThat(order.getShippingMethod().getId()).isEqualTo(shippingMethods.get(1).getId());
        assertThat(order.getShippingMethod().getDescription()).isEqualTo(shippingMethods.get(1).getDescription());
    }

    /**
     * payment
     */
    @Test
    @org.junit.jupiter.api.Order(43)
    public void attempting_add_a_Payment_throws_error_when_in_AddingItems_state() throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(testSuccessfulPaymentMethod.getCode());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "A Payment may only be added when Order is in \"ArrangingPayment\" state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(44)
    public void transitions_to_the_ArrangingPayment_state() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("state", "ArrangingPayment");
        variables.put("id", activeOrder.getId());
        GraphQLResponse graphQLResponse = shopClient.perform(TRANSITION_TO_STATE, variables);
        Order order = graphQLResponse.get("$.data.transitionOrderToState", Order.class);
        assertThat(order.getId()).isEqualTo(activeOrder.getId());
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());
    }

    @Test
    @org.junit.jupiter.api.Order(45)
    public void attempting_to_add_item_throws_error_when_in_ArraningPayment_state() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 4L);
        variables.put("quantity", 1);
        try {
            shopClient.perform(ADD_ITEM_TO_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Order contents may only be modified when in the \"AddingItems\" state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(46)
    public void attempting_to_modify_item_quantity_throws_error_when_in_ArrangingPayment_state() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", activeOrder.getLines().get(0).getId());
        variables.put("quantity", 12);
        try {
            shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Order contents may only be modified when in the \"AddingItems\" state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(47)
    public void attempting_to_remove_an_item_throws_error_when_in_ArrangingPayment_state() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("orderLineId", firstOrderLineId);
        try {
            shopClient.perform(REMOVE_ITEM_FROM_ORDER, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Order contents may only be modified when in the \"AddingItems\" state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(48)
    public void attempting_to_setOrderShippingMethod_throws_error_when_in_ArraningPayment_state() throws IOException {
        GraphQLResponse graphQLResponse = shopClient.perform(GET_ELIGIBLE_SHIPPING_METHODS, null);
        List<ShippingMethodQuote> shippingMethods  =
                graphQLResponse.getList("$.data.eligibleShippingMethods", ShippingMethodQuote.class);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", shippingMethods.get(0).getId());

        try {
            shopClient.perform(SET_SHIPPING_METHOD, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat("Order contents may only be modified when in the \"AddingItems\" state");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(49)
    public void adds_a_declined_payment() throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(testFailingPaymentMethod.getCode());
        input.getMetadata().put("foo", "bar");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.addPaymentToOrder", Order.class);

        Payment payment = order.getPayments().get(0);
        assertThat(order.getPayments()).hasSize(1);
        assertThat(payment.getMethod()).isEqualTo(testFailingPaymentMethod.getCode());
        assertThat(payment.getTransactionId()).isNull();
        assertThat(payment.getMetadata()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("foo", "bar")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(50)
    public void adds_an_error_payment_and_returns_error_response() throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(testErrorPaymentMethod.getCode());
        input.getMetadata().put("foo", "bar");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Something went horribly wrong");
        }
        GraphQLResponse graphQLResponse = shopClient.perform(GET_ACTIVE_ORDER_PAYMENTS, null);
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        Payment payment = activeOrder.getPayments().get(1);
        assertThat(activeOrder.getPayments()).hasSize(2);
        assertThat(payment.getMethod()).isEqualTo(testErrorPaymentMethod.getCode());
        assertThat(payment.getState()).isEqualTo("Error");
        assertThat(payment.getErrorMessage()).isEqualTo("Something went horribly wrong");
    }

    @Test
    @org.junit.jupiter.api.Order(51)
    public void adds_a_successful_payment_and_transitions_Order_state_02() throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(testSuccessfulPaymentMethod.getCode());
        input.getMetadata().put("baz", "quux");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.addPaymentToOrder", Order.class);

        Payment payment = order.getPayments().get(2);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
        assertThat(order.getActive()).isFalse();
        assertThat(order.getPayments()).hasSize(3);
        assertThat(payment.getMethod()).isEqualTo(testSuccessfulPaymentMethod.getCode());
        assertThat(payment.getState()).isEqualTo(PaymentState.Settled.name());
        assertThat(payment.getTransactionId()).isEqualTo("12345");
        assertThat(payment.getMetadata()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("baz", "quux")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(52)
    public void does_not_create_address_when_Customer_already_has_address() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(0).getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);

        assertThat(customer.getAddresses()).hasSize(1);
    }

    /**
     * orderByCode immediately after Order is placed.
     */
    @Test
    @org.junit.jupiter.api.Order(53)
    public void works_when_authenticated() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("code", activeOrder.getCode());

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ORDER_BY_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByCode", Order.class);

        assertThat(order.getId()).isEqualTo(activeOrder.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(54)
    public void works_when_anonymous() throws IOException {
        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("code", activeOrder.getCode());

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ORDER_BY_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByCode", Order.class);

        assertThat(order.getId()).isEqualTo(activeOrder.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(55)
    public void throws_error_for_another_user_s_Order() throws IOException {
        authenticatedUserEmailAddress = customers.get(1).getEmailAddress();
        shopClient.asUserWithCredentials(authenticatedUserEmailAddress, password);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("code", activeOrder.getCode());

        try {
            shopClient.perform(GET_ORDER_BY_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat("You are not currently authorized to perform this action");
        }
    }

    /**
     * order merging
     */
    private void before_test_56() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_LIST, null);
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customers = customerList.getItems();
    }

    @Test
    @org.junit.jupiter.api.Order(56)
    public void merges_guest_order_with_no_existing_order() throws IOException {
        before_test_56();

        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);

        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getProductVariant().getId()).isEqualTo(1);

        variables = objectMapper.createObjectNode();
        variables.put("username", customers.get(1).getEmailAddress());
        variables.put("password", password);

        shopClient.perform(ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));

        graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getLines()).hasSize(1);
        assertThat(activeOrder.getLines().get(0).getProductVariant().getId()).isEqualTo(1);
    }

    @Test
    @org.junit.jupiter.api.Order(57)
    public void merges_guest_order_with_existing_order() throws IOException {
        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 2L);
        variables.put("quantity", 1);

        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().get(0).getProductVariant().getId()).isEqualTo(2);

        variables = objectMapper.createObjectNode();
        variables.put("username", customers.get(1).getEmailAddress());
        variables.put("password", password);

        shopClient.perform(ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));

        graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getLines()).hasSize(2);
        assertThat(activeOrder.getLines().get(0).getProductVariant().getId()).isEqualTo(1);
        assertThat(activeOrder.getLines().get(1).getProductVariant().getId()).isEqualTo(2);
    }

    @Test
    @org.junit.jupiter.api.Order(58)
    public void does_not_merge_when_logging_in_to_a_different_account() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("username", customers.get(2).getEmailAddress());
        variables.put("password", password);

        shopClient.perform(ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder).isNull();
    }

    @Test
    @org.junit.jupiter.api.Order(59)
    public void does_not_merge_when_logging_back_to_other_account() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 3L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("username", customers.get(1).getEmailAddress());
        variables.put("password", password);

        shopClient.perform(ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getLines()).hasSize(2);
        assertThat(activeOrder.getLines().get(0).getProductVariant().getId()).isEqualTo(1L);
        assertThat(activeOrder.getLines().get(1).getProductVariant().getId()).isEqualTo(2L);
    }

    /**
     * security of customer data
     */
    /**
     * order merging
     */
    private void before_test_60() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_LIST, null);
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customers = customerList.getItems();
    }

    @Test
    @org.junit.jupiter.api.Order(60)
    public void cannot_setCustomerOrder_to_existing_non_guest_Customer() throws IOException {
        before_test_60();

        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress(customers.get(0).getEmailAddress());
        input.setFirstName("Evil");
        input.setLastName("Hacker");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            shopClient.perform(SET_CUSTOMER, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot use a registered email address for a guest order. Please log in first"
            );
        }

        variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(0).getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getFirstName()).isNotEqualTo("Evil");
        assertThat(customer.getLastName()).isNotEqualTo("Hacker");
    }

    @Test
    @org.junit.jupiter.api.Order(61)
    public void guest_cannot_access_Addresses_of_guest_customer() throws IOException {
        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test@test.com");
        input.setFirstName("Evil");
        input.setLastName("Hacker");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_CUSTOMER, variables);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_ACTIVE_ORDER_ADDRESSES, null);
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getCustomer().getAddresses()).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(62)
    public void guest_cannot_access_Orders_of_guest_customer() throws IOException {
        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test@test.com");
        input.setFirstName("Evil");
        input.setLastName("Hacker");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_CUSTOMER, variables);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_ACTIVE_ORDER_ORDERS, null);
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);
        assertThat(activeOrder.getCustomer().getOrders().getItems()).isEmpty();
    }

}
