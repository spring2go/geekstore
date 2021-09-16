/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.config.payment.FailsToSettlePaymentMethod;
import io.geekstore.config.payment.SingleStageRefundablePaymentMethod;
import io.geekstore.config.payment.TwoStagePaymentMethod;
import io.geekstore.config.payment_method.PaymentMethodHandler;
import io.geekstore.config.payment_method.PaymentOptions;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.service.helpers.refund_state_machine.RefundState;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.customer.CustomerListOptions;
import io.geekstore.types.history.HistoryEntry;
import io.geekstore.types.history.HistoryEntryListOptions;
import io.geekstore.types.history.HistoryEntryType;
import io.geekstore.types.order.*;
import io.geekstore.types.payment.Payment;
import io.geekstore.types.payment.Refund;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductVariant;
import io.geekstore.types.product.UpdateProductVariantInput;
import io.geekstore.types.stock.StockMovement;
import io.geekstore.types.stock.StockMovementType;
import io.geekstore.utils.TestHelper;
import io.geekstore.utils.TestOrderUtils;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class OrderTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String ORDER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "order_fragment");
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
    static final String GET_STOCK_MOVEMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_stock_movement");
    static final String VARIANT_WITH_STOCK_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "variant_with_stock_fragment");

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String ADD_ITEM_TO_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_item_to_order");
    static final String GET_ACTIVE_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_order");
    static final String TEST_ORDER_FRAGMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "test_order_fragment");

    static final String ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/order/%s.graphqls";
    static final String GET_ORDER_LIST  =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "get_order_list");
    static final String GET_ORDER_HISTORY  =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "get_order_history");
    static final String SETTLE_PAYMENT =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "settle_payment");
    static final String CREATE_FULFILLMENT =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "create_fulfillment");
    static final String GET_ORDER_FULFILLMENTS =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "get_order_fulfillments");
    static final String GET_ORDER_LIST_FULFILLMENTS =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "get_order_list_fulfillments");
    static final String GET_ORDER_FULFILLMENT_ITEMS =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "get_order_fulfillment_items");
    static final String CANCEL_ORDER =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "cancel_order");
    static final String REFUND_ORDER =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "refund_order");
    static final String SETTLE_REFUND =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "settle_refund");
    static final String ADD_NOTE_TO_ORDER =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "add_note_to_order");
    static final String UPDATE_NOTE =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "update_note");
    static final String DELETE_ORDER_NOTE =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "delete_order_note");


    @Autowired
    TestHelper testHelper;

    @Autowired
    TestOrderUtils testOrderUtils;

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

    PaymentMethodHandler twoStatePaymentMethod;
    PaymentMethodHandler failsToSettlePaymentMethod;
    PaymentMethodHandler singleStageRefundablePaymentMethod;

    @TestConfiguration
    static class ContextConfiguration {

        @Bean
        @Primary
        public PaymentOptions testPaymentOptions() {
            return new PaymentOptions(
                    Arrays.asList(
                            spy(TwoStagePaymentMethod.class),
                            new FailsToSettlePaymentMethod(),
                            new SingleStageRefundablePaymentMethod()
                    )
            );
        }
    }

    List<Customer> customers;
    String password = MockDataService.TEST_PASSWORD;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(3).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        twoStatePaymentMethod = paymentOptions.getPaymentMethodHandlers().get(0);
        failsToSettlePaymentMethod = paymentOptions.getPaymentMethodHandlers().get(1);
        singleStageRefundablePaymentMethod = paymentOptions.getPaymentMethodHandlers().get(2);

        CustomerListOptions options = new CustomerListOptions();
        options.setCurrentPage(1);
        options.setPageSize(3);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_CUSTOMER_LIST, variables);
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customers = customerList.getItems();

        shopClient.asUserWithCredentials(customers.get(0).getEmailAddress(), password);
        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 2L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        shopClient.asUserWithCredentials(customers.get(1).getEmailAddress(), password);
        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 2L);
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 3L);
        variables.put("quantity", 3);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    public void orders() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_LIST, null, Arrays.asList(ORDER_FRAGMENT));
        OrderList orderList = graphQLResponse.get("$.data.orders", OrderList.class);
        assertThat(orderList.getItems().stream().map(o -> o.getId())).containsExactly(1L, 2L);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void order() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getId()).isEqualTo(2);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void order_history_initially_empty() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        assertThat(order.getHistory().getTotalItems()).isEqualTo(0);
        assertThat(order.getHistory().getItems()).isEmpty();
    }

    /**
     * payments
     */
    @Test
    @org.junit.jupiter.api.Order(4)
    public void settlePayment_fails() throws IOException {
        shopClient.asUserWithCredentials(customers.get(0).getEmailAddress(), password);
        testOrderUtils.proceedToArrangingPayment(shopClient);
        Order order = testOrderUtils.addPaymentToOrder(shopClient, failsToSettlePaymentMethod);

        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());

        Payment payment = order.getPayments().get(0);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", payment.getId());
        GraphQLResponse graphQLResponse = adminClient.perform(SETTLE_PAYMENT, variables);
        Payment settlePayment = graphQLResponse.get("$.data.settlePayment", Payment.class);
        assertThat(settlePayment.getId()).isEqualTo(payment.getId());
        assertThat(settlePayment.getState()).isEqualTo(PaymentState.Authorized.name());

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void settlePayment_succeeds_onStateTransitionStart_called() throws IOException {
        Mockito.clearInvocations(twoStatePaymentMethod);

        shopClient.asUserWithCredentials(customers.get(1).getEmailAddress(), password);
        testOrderUtils.proceedToArrangingPayment(shopClient);
        Order order = testOrderUtils.addPaymentToOrder(shopClient, twoStatePaymentMethod);

        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());

        ArgumentCaptor<PaymentState> fromStateCapture = ArgumentCaptor.forClass(PaymentState.class);
        ArgumentCaptor<PaymentState> toStateCapture = ArgumentCaptor.forClass(PaymentState.class);
        Mockito.verify(twoStatePaymentMethod, Mockito.times(1))
                .onStateTransitionStart(fromStateCapture.capture(), toStateCapture.capture(), any());
        assertThat(fromStateCapture.getValue()).isEqualTo(PaymentState.Created);
        assertThat(toStateCapture.getValue()).isEqualTo(PaymentState.Authorized);

        Payment payment = order.getPayments().get(0);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", payment.getId());
        GraphQLResponse graphQLResponse = adminClient.perform(SETTLE_PAYMENT, variables);
        Payment settlePayment = graphQLResponse.get("$.data.settlePayment", Payment.class);
        assertThat(settlePayment.getId()).isEqualTo(payment.getId());
        assertThat(settlePayment.getState()).isEqualTo(PaymentState.Settled.name());
        assertThat(settlePayment.getMetadata()).containsExactly(entry("baz", "quux"), entry("moreData", "42"));

        Mockito.verify(twoStatePaymentMethod, Mockito.times(2))
                .onStateTransitionStart(fromStateCapture.capture(), toStateCapture.capture(), any());
        assertThat(fromStateCapture.getValue()).isEqualTo(PaymentState.Authorized);
        assertThat(toStateCapture.getValue()).isEqualTo(PaymentState.Settled);

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getId());
        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
        assertThat(order.getPayments().get(0).getState()).isEqualTo(PaymentState.Settled.name());
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void order_history_contains_expected_entries_01() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).hasSize(5);

        HistoryEntry historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry1.getData())
                .containsExactly(entry("from", "AddingItems"), entry("to", "ArrangingPayment"));

        HistoryEntry historyEntry2 = order.getHistory().getItems().get(1);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.ORDER_PAYMENT_TRANSITION);
        assertThat(historyEntry2.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "paymentId", "2", "from", "Created", "to", "Authorized"));

        HistoryEntry historyEntry3 = order.getHistory().getItems().get(2);
        assertThat(historyEntry3.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry3.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "from", "ArrangingPayment", "to", "PaymentAuthorized"));

        HistoryEntry historyEntry4 = order.getHistory().getItems().get(3);
        assertThat(historyEntry4.getType()).isEqualTo(HistoryEntryType.ORDER_PAYMENT_TRANSITION);
        assertThat(historyEntry4.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "paymentId", "2", "from", "Authorized", "to", "Settled"));

        HistoryEntry historyEntry5 = order.getHistory().getItems().get(4);
        assertThat(historyEntry5.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry5.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "from", "PaymentAuthorized", "to", "PaymentSettled"));
    }

    /**
     * fulfillment
     */

    @Test
    @org.junit.jupiter.api.Order(7)
    public void throws_if_Order_is_not_in_PaymentSettled_state() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());

        FulfillOrderInput input = new FulfillOrderInput();
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(l.getQuantity());
            return orderLineInput;
        }).collect(Collectors.toList()));
        input.setMethod("Test");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CREATE_FULFILLMENT, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "One or more OrderItems belong to an Order which is in an invalid state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    public void throws_if_lines_is_empty() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());

        FulfillOrderInput input = new FulfillOrderInput();
        input.setMethod("Test");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CREATE_FULFILLMENT, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Nothing to fulfill"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    public void throws_if_all_quantities_are_zero() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());

        FulfillOrderInput input = new FulfillOrderInput();
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(0);
            return orderLineInput;
        }).collect(Collectors.toList()));
        input.setMethod("Test");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CREATE_FULFILLMENT, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Nothing to fulfill"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    public void creates_a_partial_fulfillment() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
        List<OrderLine> lines = order.getLines();

        FulfillOrderInput input = new FulfillOrderInput();
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(1);
            return orderLineInput;
        }).collect(Collectors.toList()));
        input.setMethod("Test1");
        input.setTrackingCode("111");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(CREATE_FULFILLMENT, variables);
        Fulfillment fulfillment = graphQLResponse.get("$.data.fulfillOrder", Fulfillment.class);

        assertThat(fulfillment.getMethod()).isEqualTo("Test1");
        assertThat(fulfillment.getTrackingCode()).isEqualTo("111");
        OrderItem orderItem1 = fulfillment.getOrderItems().get(0);
        assertThat(orderItem1.getId()).isEqualTo(lines.get(0).getItems().get(0).getId());
        OrderItem orderItem2 = fulfillment.getOrderItems().get(1);
        assertThat(orderItem2.getId()).isEqualTo(lines.get(1).getItems().get(0).getId());

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PartiallyFulfilled.name());

        assertThat(order.getLines().get(0).getItems().get(0).getFulfillment().getId()).isEqualTo(fulfillment.getId());
        assertThat(order.getLines().get(1).getItems().stream().filter(
                i -> i.getFulfillment() != null && Objects.equals(i.getFulfillment().getId(), fulfillment.getId())
        ).collect(Collectors.toList())).hasSize(1);
        assertThat(order.getLines().get(1).getItems().stream().filter(
                i -> i.getFulfillment() == null
        ).collect(Collectors.toList())).hasSize(2);
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    public void creates_a_second_partial_fulfillment() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PartiallyFulfilled.name());
        List<OrderLine> lines = order.getLines();

        FulfillOrderInput input = new FulfillOrderInput();
        OrderLineInput orderLineInput = new OrderLineInput();
        orderLineInput.setOrderLineId(lines.get(1).getId());
        orderLineInput.setQuantity(1);
        input.getLines().add(orderLineInput);
        input.setMethod("Test2");
        input.setTrackingCode("222");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(CREATE_FULFILLMENT, variables);
        graphQLResponse.get("$.data.fulfillOrder", Fulfillment.class);

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PartiallyFulfilled.name());
        assertThat(order.getLines().get(1).getItems().stream().filter(
                i -> i.getFulfillment() != null
        ).collect(Collectors.toList())).hasSize(2);
        assertThat(order.getLines().get(1).getItems().stream().filter(
                i -> i.getFulfillment() == null
        ).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    public void throws_if_an_OrderItem_already_part_of_a_Fulfillment() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PartiallyFulfilled.name());

        FulfillOrderInput input = new FulfillOrderInput();
        OrderLineInput orderLineInput = new OrderLineInput();
        orderLineInput.setOrderLineId(order.getLines().get(0).getId());
        orderLineInput.setQuantity(1);
        input.getLines().add(orderLineInput);
        input.setMethod("Test");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CREATE_FULFILLMENT, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "One or more OrderItems have already been fulfilled"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    public void completes_fulfillment() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.PartiallyFulfilled.name());

        OrderItem unfulfilledItem = order.getLines().get(1).getItems().stream()
                .filter(i -> i.getFulfillment() == null).findFirst().get();

        FulfillOrderInput input = new FulfillOrderInput();
        OrderLineInput orderLineInput = new OrderLineInput();
        orderLineInput.setOrderLineId(order.getLines().get(1).getId());
        orderLineInput.setQuantity(1);
        input.getLines().add(orderLineInput);
        input.setMethod("Test3");
        input.setTrackingCode("333");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(CREATE_FULFILLMENT, variables);
        Fulfillment fulfillment = graphQLResponse.get("$.data.fulfillOrder", Fulfillment.class);
        assertThat(fulfillment.getMethod()).isEqualTo("Test3");
        assertThat(fulfillment.getTrackingCode()).isEqualTo("333");
        assertThat(fulfillment.getOrderItems()).hasSize(1);
        assertThat(fulfillment.getOrderItems().get(0).getId()).isEqualTo(unfulfilledItem.getId());

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.Fulfilled.name());
    }

    @Test
    @org.junit.jupiter.api.Order(14)
    public void order_history_contains_expected_entries_02() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(5);
        options.setCurrentPage(2);

        JsonNode optionsNode = objectMapper.valueToTree(options);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).hasSize(5);

        HistoryEntry historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_FULFILLMENT);
        assertThat(historyEntry1.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of("fulfillmentId", "1"));

        HistoryEntry historyEntry2 = order.getHistory().getItems().get(1);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry2.getData())
                .containsExactlyInAnyOrderEntriesOf(
                        ImmutableMap.of("from", "PaymentSettled", "to", "PartiallyFulfilled"));

        HistoryEntry historyEntry3 = order.getHistory().getItems().get(2);
        assertThat(historyEntry3.getType()).isEqualTo(HistoryEntryType.ORDER_FULFILLMENT);
        assertThat(historyEntry3.getData())
                .containsExactlyInAnyOrderEntriesOf(
                        ImmutableMap.of("fulfillmentId", "2"));

        HistoryEntry historyEntry4 = order.getHistory().getItems().get(3);
        assertThat(historyEntry4.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry4.getData())
                .containsExactlyInAnyOrderEntriesOf(
                        ImmutableMap.of("from", "PartiallyFulfilled", "to", "PartiallyFulfilled"));

        HistoryEntry historyEntry5 = order.getHistory().getItems().get(4);
        assertThat(historyEntry5.getType()).isEqualTo(HistoryEntryType.ORDER_FULFILLMENT);
        assertThat(historyEntry5.getData())
                .containsExactlyInAnyOrderEntriesOf(
                        ImmutableMap.of("fulfillmentId", "3"));

        // 还有1个在第3页
        options = new HistoryEntryListOptions();
        options.setPageSize(5);
        options.setCurrentPage(3);

        optionsNode = objectMapper.valueToTree(options);

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);
        variables.set("options", optionsNode);

        graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).hasSize(1);

        historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry1.getData())
                .containsExactlyInAnyOrderEntriesOf(
                        ImmutableMap.of("from", "PartiallyFulfilled", "to", "Fulfilled"));
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    public void order_fulfillments_resolver_for_single_order() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_FULFILLMENTS, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getFulfillments()).hasSize(3);

        Fulfillment fulfillment1 = order.getFulfillments().get(0);
        assertThat(fulfillment1.getId()).isEqualTo(1L);
        assertThat(fulfillment1.getMethod()).isEqualTo("Test1");

        Fulfillment fulfillment2 = order.getFulfillments().get(1);
        assertThat(fulfillment2.getId()).isEqualTo(2L);
        assertThat(fulfillment2.getMethod()).isEqualTo("Test2");

        Fulfillment fulfillment3 = order.getFulfillments().get(2);
        assertThat(fulfillment3.getId()).isEqualTo(3L);
        assertThat(fulfillment3.getMethod()).isEqualTo("Test3");
    }

    @Test
    @org.junit.jupiter.api.Order(16)
    public void order_fulfillments_resolver_for_order_list() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_LIST_FULFILLMENTS, null);
        OrderList orderList = graphQLResponse.get("$.data.orders", OrderList.class);

        assertThat(orderList.getItems().get(0).getFulfillments()).isEmpty();
        assertThat(orderList.getItems().get(1).getFulfillments()).hasSize(3);
        Fulfillment fulfillment1 = orderList.getItems().get(1).getFulfillments().get(0);
        assertThat(fulfillment1.getId()).isEqualTo(1L);
        assertThat(fulfillment1.getMethod()).isEqualTo("Test1");

        Fulfillment fulfillment2 = orderList.getItems().get(1).getFulfillments().get(1);
        assertThat(fulfillment2.getId()).isEqualTo(2L);
        assertThat(fulfillment2.getMethod()).isEqualTo("Test2");

        Fulfillment fulfillment3 = orderList.getItems().get(1).getFulfillments().get(2);
        assertThat(fulfillment3.getId()).isEqualTo(3L);
        assertThat(fulfillment3.getMethod()).isEqualTo("Test3");
    }

    @Test
    @org.junit.jupiter.api.Order(17)
    public void order_fulfillments_orderItems_resolver() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_FULFILLMENT_ITEMS, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getFulfillments().get(0).getOrderItems()).hasSize(2);
        assertThat(order.getFulfillments().get(0).getOrderItems().stream()
                .map(OrderItem::getId).collect(Collectors.toList())).containsExactly(3L, 4L);
        assertThat(order.getFulfillments().get(1).getOrderItems()).hasSize(1);
        assertThat(order.getFulfillments().get(1).getOrderItems().stream()
                .map(OrderItem::getId).collect(Collectors.toList())).containsExactly(5L);
    }

    /**
     * cancellation by orderId
     */

    @Test
    @org.junit.jupiter.api.Order(18)
    public void cancel_from_AddingItems_state() throws IOException {
        List<Object> testOrderInfoList =
                createTestOrder(adminClient, shopClient, customers.get(0).getEmailAddress(), password);
        Product testProduct = (Product) testOrderInfoList.get(0);
        Long testProductVariantId = (Long) testOrderInfoList.get(1);
        Long testOrderId = (Long) testOrderInfoList.get(2);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", testOrderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        assertThat(order.getState()).isEqualTo(OrderState.AddingItems.name());

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(testOrderId);

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(CANCEL_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", testOrderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        assertThat(order.getState()).isEqualTo(OrderState.Cancelled.name());
        assertThat(order.getActive()).isFalse();
        assertNoStockMovementsCreated(testProduct.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(19)
    public void cancel_from_ArrangingPayment_state() throws IOException {
        List<Object> testOrderInfoList =
                createTestOrder(adminClient, shopClient, customers.get(0).getEmailAddress(), password);
        Product testProduct = (Product) testOrderInfoList.get(0);
        Long testProductVariantId = (Long) testOrderInfoList.get(1);
        Long testOrderId = (Long) testOrderInfoList.get(2);

        testOrderUtils.proceedToArrangingPayment(shopClient);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", testOrderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(testOrderId);

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(CANCEL_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", testOrderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.Cancelled.name());
        assertThat(order.getActive()).isFalse();

        this.assertNoStockMovementsCreated(testProduct.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(20)
    public void cancel_from_PaymentAuthorized_state() throws IOException {
        List<Object> testOrderInfoList =
                createTestOrder(adminClient, shopClient, customers.get(0).getEmailAddress(), password);
        Product testProduct = (Product) testOrderInfoList.get(0);
        Long testProductVariantId = (Long) testOrderInfoList.get(1);
        Long testOrderId = (Long) testOrderInfoList.get(2);

        testOrderUtils.proceedToArrangingPayment(shopClient);

        Order order = testOrderUtils.addPaymentToOrder(shopClient, failsToSettlePaymentMethod);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 3L);

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        ProductVariant variant1 = product.getVariants().get(0);
        assertThat(variant1.getStockOnHand()).isEqualTo(98);
        StockMovement stockMovement1 = variant1.getStockMovements().getItems().get(0);
        assertThat(stockMovement1.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(stockMovement1.getQuantity()).isEqualTo(100);
        StockMovement stockMovement2 = variant1.getStockMovements().getItems().get(1);
        assertThat(stockMovement2.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement2.getQuantity()).isEqualTo(-2);

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(testOrderId);

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(CANCEL_ORDER, variables);
        Order cancelOrder = graphQLResponse.get("$.data.cancelOrder", Order.class);
        assertThat(cancelOrder.getLines()).hasSize(1);
        assertThat(cancelOrder.getLines().get(0).getItems()).hasSize(2);

        OrderItem orderItem1 = cancelOrder.getLines().get(0).getItems().get(0);
        assertThat(orderItem1.getId()).isEqualTo(11L);
        assertThat(orderItem1.getCancelled()).isTrue();
        OrderItem orderItem2 = cancelOrder.getLines().get(0).getItems().get(1);
        assertThat(orderItem2.getId()).isEqualTo(12L);
        assertThat(orderItem2.getCancelled()).isTrue();

        variables = objectMapper.createObjectNode();
        variables.put("id", testOrderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.Cancelled.name());
        assertThat(order.getActive()).isFalse();

        variables = objectMapper.createObjectNode();
        variables.put("id", 3L);

        graphQLResponse = adminClient.perform(
                GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        product = graphQLResponse.get("$.data.adminProduct", Product.class);
        variant1 = product.getVariants().get(0);
        assertThat(variant1.getStockOnHand()).isEqualTo(100);
        stockMovement1 = variant1.getStockMovements().getItems().get(0);
        assertThat(stockMovement1.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(stockMovement1.getQuantity()).isEqualTo(100);
        stockMovement2 = variant1.getStockMovements().getItems().get(1);
        assertThat(stockMovement2.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement2.getQuantity()).isEqualTo(-2);

        StockMovement stockMovement3 = variant1.getStockMovements().getItems().get(2);
        assertThat(stockMovement3.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement3.getQuantity()).isEqualTo(1);

        StockMovement stockMovement4 = variant1.getStockMovements().getItems().get(3);
        assertThat(stockMovement4.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement4.getQuantity()).isEqualTo(1);
    }

    private void assertNoStockMovementsCreated(Long productId) throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", productId);

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        ProductVariant productVariant = product.getVariants().get(0);
        assertThat(productVariant.getStockOnHand()).isEqualTo(100);
        assertThat(productVariant.getStockMovements().getItems()).hasSize(1);
        StockMovement stockMovement = productVariant.getStockMovements().getItems().get(0);
        assertThat(stockMovement.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(stockMovement.getQuantity()).isEqualTo(100);
    }

    private List<Object> createTestOrder(
            ApiClient adminClient, ApiClient shopClient, String emailAddress, String password) throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 3L);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_PRODUCT_WITH_VARIANTS, variables,
                Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        Long productVariantId = product.getVariants().get(0).getId();

        // Set the ProductVariant to trackInventory
        UpdateProductVariantInput updateProductVariantInput = new UpdateProductVariantInput();
        updateProductVariantInput.setId(productVariantId);
        updateProductVariantInput.setTrackInventory(true);

        JsonNode inputNode = objectMapper.valueToTree(
                Arrays.asList(updateProductVariantInput));
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = this.adminClient.perform(UPDATE_PRODUCT_VARIANTS, variables,
                Arrays.asList(PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);

        // Add the ProductVariant to the Order
        shopClient.asUserWithCredentials(emailAddress, password);

        shopClient.asUserWithCredentials(emailAddress, password);
        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", productVariantId);
        variables.put("quantity", 2);
        graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        Long orderId = order.getId();

        return Arrays.asList(product, productVariantId, orderId);
    }

    /**
     * cancellation by OrderLine test cases
     */
    Long orderId;
    Product product;
    Long productVariantId;

    private void before_test_20() throws IOException {
        List<Object> testOrderInfoList =
                createTestOrder(adminClient, shopClient, customers.get(0).getEmailAddress(), password);
        product = (Product) testOrderInfoList.get(0);
        productVariantId = (Long) testOrderInfoList.get(1);
        orderId = (Long) testOrderInfoList.get(2);
    }

    @Test
    @org.junit.jupiter.api.Order(21)
    public void cannot_cancel_from_AddingItems_state() throws IOException {
        before_test_20();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.AddingItems.name());

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(orderId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(1);
            return orderLineInput;
        }).collect(Collectors.toList()));

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CANCEL_ORDER, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot cancel OrderLines from an Order in the \"{ AddingItems }\" state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(22)
    public void cannot_cancel_from_ArrangingPayment_state() throws IOException {
        testOrderUtils.proceedToArrangingPayment(shopClient);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.ArrangingPayment.name());

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(orderId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(1);
            return orderLineInput;
        }).collect(Collectors.toList()));

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CANCEL_ORDER, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot cancel OrderLines from an Order in the \"{ ArrangingPayment }\" state"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(23)
    public void throws_if_lines_are_empty() throws IOException {
        Order order = testOrderUtils.addPaymentToOrder(shopClient, twoStatePaymentMethod);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(orderId);
        input.setLines(new ArrayList<>());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CANCEL_ORDER, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Nothing to cancel"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(24)
    public void throws_if_all_quantities_zero() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(orderId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(0);
            return orderLineInput;
        }).collect(Collectors.toList()));

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CANCEL_ORDER, variables);
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Nothing to cancel"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(25)
    public void partial_cancellation() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", product.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);
        ProductVariant variant1 = product.getVariants().get(0);
        assertThat(variant1.getStockOnHand()).isEqualTo(98);
        StockMovement stockMovement1 = variant1.getStockMovements().getItems().get(0);
        assertThat(stockMovement1.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(stockMovement1.getQuantity()).isEqualTo(100);
        StockMovement stockMovement2 = variant1.getStockMovements().getItems().get(1);
        assertThat(stockMovement2.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement2.getQuantity()).isEqualTo(-2);
        StockMovement stockMovement3 = variant1.getStockMovements().getItems().get(2);
        assertThat(stockMovement3.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement3.getQuantity()).isEqualTo(1);
        StockMovement stockMovement4 = variant1.getStockMovements().getItems().get(3);
        assertThat(stockMovement4.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement4.getQuantity()).isEqualTo(1);
        StockMovement stockMovement5 = variant1.getStockMovements().getItems().get(4);
        assertThat(stockMovement5.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement5.getQuantity()).isEqualTo(-2);

        variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(orderId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(1);
            return orderLineInput;
        }).collect(Collectors.toList()));
        input.setReason("cancel reason 1");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(CANCEL_ORDER, variables);
        Order cancelOrder = graphQLResponse.get("$.data.cancelOrder", Order.class);

        assertThat(cancelOrder.getLines().get(0).getQuantity()).isEqualTo(1);
        List<OrderItem> sortedItems = cancelOrder.getLines().get(0).getItems().stream()
                .sorted((a, b) -> a.getId() < b.getId() ? -1 : 1).collect(Collectors.toList());
        assertThat(sortedItems.get(0).getId()).isEqualTo(13L);
        assertThat(sortedItems.get(0).getCancelled()).isTrue();
        assertThat(sortedItems.get(1).getId()).isEqualTo(14L);
        assertThat(sortedItems.get(1).getCancelled()).isFalse();

        variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());
        assertThat(order.getLines().get(0).getQuantity()).isEqualTo(1);

        variables = objectMapper.createObjectNode();
        variables.put("id", product.getId());

        graphQLResponse = adminClient.perform(
                GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        product = graphQLResponse.get("$.data.adminProduct", Product.class);
        ProductVariant variant2 = product.getVariants().get(0);
        assertThat(variant2.getStockOnHand()).isEqualTo(99);
        stockMovement1 = variant2.getStockMovements().getItems().get(0);
        assertThat(stockMovement1.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(stockMovement1.getQuantity()).isEqualTo(100);
        stockMovement2 = variant2.getStockMovements().getItems().get(1);
        assertThat(stockMovement2.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement2.getQuantity()).isEqualTo(-2);
        stockMovement3 = variant2.getStockMovements().getItems().get(2);
        assertThat(stockMovement3.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement3.getQuantity()).isEqualTo(1);
        stockMovement4 = variant2.getStockMovements().getItems().get(3);
        assertThat(stockMovement4.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement4.getQuantity()).isEqualTo(1);
        stockMovement5 = variant2.getStockMovements().getItems().get(4);
        assertThat(stockMovement5.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement5.getQuantity()).isEqualTo(-2);
        StockMovement stockMovement6 = variant2.getStockMovements().getItems().get(5);
        assertThat(stockMovement6.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement6.getQuantity()).isEqualTo(1);
    }

    @Test
    @org.junit.jupiter.api.Order(26)
    public void complete_cancellation() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        CancelOrderInput input = new CancelOrderInput();
        input.setOrderId(orderId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(1);
            return orderLineInput;
        }).collect(Collectors.toList()));
        input.setReason("cancel reason 2");

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(CANCEL_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getState()).isEqualTo(OrderState.Cancelled.name());

        variables = objectMapper.createObjectNode();
        variables.put("id", product.getId());

        graphQLResponse = adminClient.perform(
                GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        product = graphQLResponse.get("$.data.adminProduct", Product.class);
        ProductVariant variant2 = product.getVariants().get(0);
        assertThat(variant2.getStockOnHand()).isEqualTo(100);
        StockMovement stockMovement1 = variant2.getStockMovements().getItems().get(0);
        assertThat(stockMovement1.getType()).isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(stockMovement1.getQuantity()).isEqualTo(100);
        StockMovement stockMovement2 = variant2.getStockMovements().getItems().get(1);
        assertThat(stockMovement2.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement2.getQuantity()).isEqualTo(-2);
        StockMovement stockMovement3 = variant2.getStockMovements().getItems().get(2);
        assertThat(stockMovement3.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement3.getQuantity()).isEqualTo(1);
        StockMovement stockMovement4 = variant2.getStockMovements().getItems().get(3);
        assertThat(stockMovement4.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement4.getQuantity()).isEqualTo(1);
        StockMovement stockMovement5 = variant2.getStockMovements().getItems().get(4);
        assertThat(stockMovement5.getType()).isEqualTo(StockMovementType.SALE);
        assertThat(stockMovement5.getQuantity()).isEqualTo(-2);
        StockMovement stockMovement6 = variant2.getStockMovements().getItems().get(5);
        assertThat(stockMovement6.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement6.getQuantity()).isEqualTo(1);
        StockMovement stockMovement7 = variant2.getStockMovements().getItems().get(6);
        assertThat(stockMovement7.getType()).isEqualTo(StockMovementType.CANCELLATION);
        assertThat(stockMovement7.getQuantity()).isEqualTo(1);
    }

    @Test
    @org.junit.jupiter.api.Order(27)
    public void order_history_contains_expected_entries() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).isNotEmpty();

        HistoryEntry historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry1.getData())
                .containsExactly(entry("from", "AddingItems"), entry("to", "ArrangingPayment"));

        HistoryEntry historyEntry2 = order.getHistory().getItems().get(1);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.ORDER_PAYMENT_TRANSITION);
        assertThat(historyEntry2.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "paymentId", "4", "from", "Created", "to", "Authorized"));

        HistoryEntry historyEntry3 = order.getHistory().getItems().get(2);
        assertThat(historyEntry3.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry3.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "from", "ArrangingPayment", "to", "PaymentAuthorized"));

        HistoryEntry historyEntry4 = order.getHistory().getItems().get(3);
        assertThat(historyEntry4.getType()).isEqualTo(HistoryEntryType.ORDER_CANCELLATION);
        assertThat(historyEntry4.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "orderItemIds", "13", "reason", "cancel reason 1"));

        HistoryEntry historyEntry5 = order.getHistory().getItems().get(4);
        assertThat(historyEntry5.getType()).isEqualTo(HistoryEntryType.ORDER_CANCELLATION);
        assertThat(historyEntry5.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "orderItemIds", "14", "reason", "cancel reason 2"));

        HistoryEntry historyEntry6 = order.getHistory().getItems().get(5);
        assertThat(historyEntry6.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry6.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "from", "PaymentAuthorized", "to", "Cancelled"));
    }

    /**
     * refunds
     */

    Long paymentId;
    Long refundId;

    private void before_test_28() throws IOException {
        List<Object> testOrderInfoList =
                createTestOrder(adminClient, shopClient, customers.get(0).getEmailAddress(), password);
        product = (Product) testOrderInfoList.get(0);
        productVariantId = (Long) testOrderInfoList.get(1);
        orderId = (Long) testOrderInfoList.get(2);
    }

    @Test
    @org.junit.jupiter.api.Order(28)
    public void cannot_refund_from_PaymentAuthorized_state() throws IOException {
        before_test_28();

        testOrderUtils.proceedToArrangingPayment(shopClient);
        Order order = testOrderUtils.addPaymentToOrder(shopClient, twoStatePaymentMethod);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentAuthorized.name());
        paymentId = order.getPayments().get(0).getId();

        RefundOrderInput input = new RefundOrderInput();
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(1);
            return orderLineInput; }).collect(Collectors.toList()));
        input.setShipping(0);
        input.setAdjustment(0);
        input.setPaymentId(paymentId);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(REFUND_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat("Cannot refund an Order in the \"PaymentAuthorized\" state");
        }
    }

    @Test
    @org.junit.jupiter.api.Order(29)
    public void throws_if_no_lines_and_no_shipping() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        variables = objectMapper.createObjectNode();
        variables.put("id", order.getPayments().get(0).getId());
        graphQLResponse = adminClient.perform(SETTLE_PAYMENT, variables);
        Payment settlePayment = graphQLResponse.get("$.data.settlePayment", Payment.class);

        assertThat(settlePayment.getState()).isEqualTo(PaymentState.Settled.name());

        RefundOrderInput input = new RefundOrderInput();
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(0);
            return orderLineInput; }).collect(Collectors.toList()));
        input.setShipping(0);
        input.setAdjustment(0);
        input.setPaymentId(paymentId);

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(REFUND_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Nothing to refund"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(30)
    public void throws_if_paymentId_not_valid() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        RefundOrderInput input = new RefundOrderInput();
        input.setShipping(100);
        input.setAdjustment(0);
        input.setPaymentId(999L);

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(REFUND_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No PaymentEntity with the id '999' could be found"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(31)
    public void throws_if_payment_and_order_lines_do_not_belong_to_the_same_Order() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        RefundOrderInput input = new RefundOrderInput();
        input.setShipping(100);
        input.setAdjustment(0);
        input.setPaymentId(1L);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(l.getQuantity());
            return orderLineInput; }).collect(Collectors.toList()));

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(REFUND_ORDER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "The Payment and OrderLines do not belong to the same Order"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(32)
    public void creates_a_Refund_to_be_manually_settled() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        RefundOrderInput input = new RefundOrderInput();
        input.setShipping(order.getShipping());
        input.setAdjustment(0);
        input.setReason("foo");
        input.setPaymentId(paymentId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(l.getQuantity());
            return orderLineInput; }).collect(Collectors.toList()));

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = adminClient.perform(REFUND_ORDER, variables);
        Refund refund = graphQLResponse.get("$.data.refundOrder", Refund.class);

        assertThat(refund.getShipping()).isEqualTo(order.getShipping());
        assertThat(refund.getItems()).isEqualTo(order.getSubTotal());
        assertThat(refund.getTotal()).isEqualTo(order.getTotal());
        assertThat(refund.getTransactionId()).isNull();
        assertThat(refund.getState()).isEqualTo(RefundState.Pending.name());
        refundId = refund.getId();
    }

    @Test
    @org.junit.jupiter.api.Order(33)
    public void throws_if_attempting_to_refund_the_same_item_more_than_once() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER, variables, Arrays.asList(
                        ORDER_WITH_LINES_FRAGMENT, ADJUSTMENT_FRAGMENT, SHIPPING_ADDRESS_FRAGMENT, ORDER_ITEM_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);

        RefundOrderInput input = new RefundOrderInput();
        input.setShipping(order.getShipping());
        input.setAdjustment(0);
        input.setPaymentId(paymentId);
        input.setLines(order.getLines().stream().map(l -> {
            OrderLineInput orderLineInput = new OrderLineInput();
            orderLineInput.setOrderLineId(l.getId());
            orderLineInput.setQuantity(l.getQuantity());
            return orderLineInput; }).collect(Collectors.toList()));

        JsonNode inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(REFUND_ORDER, variables);
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Cannot refund an OrderItem which has already been refunded"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(34)
    public void manually_settle_a_Refund() throws IOException {
        SettleRefundInput input = new SettleRefundInput();
        input.setId(refundId);
        input.setTransactionId("aaabbb");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(SETTLE_REFUND, variables);
        Refund settleRefund = graphQLResponse.get("$.data.settleRefund", Refund.class);

        assertThat(settleRefund.getState()).isEqualTo(RefundState.Settled.name());
        assertThat(settleRefund.getTransactionId()).isEqualTo("aaabbb");
    }

    @Test
    @org.junit.jupiter.api.Order(35)
    public void order_history_contains_expected_entries_after_refund() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).isNotEmpty();

        HistoryEntry historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry1.getData())
                .containsExactly(entry("from", "AddingItems"), entry("to", "ArrangingPayment"));

        HistoryEntry historyEntry2 = order.getHistory().getItems().get(1);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.ORDER_PAYMENT_TRANSITION);
        assertThat(historyEntry2.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "paymentId", "5", "from", "Created", "to", "Authorized"));

        HistoryEntry historyEntry3 = order.getHistory().getItems().get(2);
        assertThat(historyEntry3.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry3.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "from", "ArrangingPayment", "to", "PaymentAuthorized"));

        HistoryEntry historyEntry4 = order.getHistory().getItems().get(3);
        assertThat(historyEntry4.getType()).isEqualTo(HistoryEntryType.ORDER_PAYMENT_TRANSITION);
        assertThat(historyEntry4.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "paymentId", "5", "from", "Authorized", "to", "Settled"));

        HistoryEntry historyEntry5 = order.getHistory().getItems().get(4);
        assertThat(historyEntry5.getType()).isEqualTo(HistoryEntryType.ORDER_STATE_TRANSITION);
        assertThat(historyEntry5.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "from", "PaymentAuthorized", "to", "PaymentSettled"));

        HistoryEntry historyEntry6 = order.getHistory().getItems().get(5);
        assertThat(historyEntry6.getType()).isEqualTo(HistoryEntryType.ORDER_REFUND_TRANSITION);
        assertThat(historyEntry6.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "refundId", "1", "reason", "foo", "from", "Pending", "to", "Settled"));
    }

    /**
     * order notes test cases
     */

    Long firstNoteId;

    private void before_test_36() throws IOException {
        List<Object> testOrderInfoList =
                createTestOrder(adminClient, shopClient, customers.get(2).getEmailAddress(), password);
        orderId = (Long) testOrderInfoList.get(2);
    }

    @Test
    @org.junit.jupiter.api.Order(36)
    public void private_note() throws IOException {
        before_test_36();

        AddNoteToOrderInput input = new AddNoteToOrderInput();
        input.setId(orderId);
        input.setNote("A private note");
        input.setPrivateOnly(true);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(ADD_NOTE_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addNoteToOrder", Order.class);
        assertThat(order.getId()).isEqualTo(orderId);

        variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).hasSize(1);

        HistoryEntry historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_NOTE);
        assertThat(historyEntry1.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "note", "A private note"));

        firstNoteId = order.getHistory().getItems().get(0).getId();

        graphQLResponse = shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getHistory().getItems()).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(37)
    public void public_note() throws IOException {
        AddNoteToOrderInput input = new AddNoteToOrderInput();
        input.setId(orderId);
        input.setNote("A public note");
        input.setPrivateOnly(false);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(ADD_NOTE_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addNoteToOrder", Order.class);
        assertThat(order.getId()).isEqualTo(orderId);

        variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        order = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(order.getHistory().getItems()).hasSize(2);

        HistoryEntry historyEntry = order.getHistory().getItems().get(1);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.ORDER_NOTE);
        assertThat(historyEntry.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "note", "A public note"));

        graphQLResponse = shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getHistory().getItems()).hasSize(1);

        historyEntry = activeOrder.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.ORDER_NOTE);
        assertThat(historyEntry.getData())
                .containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                        "note", "A public note"));
    }

    @Test
    @org.junit.jupiter.api.Order(38)
    public void update_note() throws IOException {
        UpdateOrderNoteInput input = new UpdateOrderNoteInput();
        input.setNoteId(firstNoteId);
        input.setNote("An updated note");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(UPDATE_NOTE, variables);
        HistoryEntry historyEntry = graphQLResponse.get("$.data.updateOrderNote", HistoryEntry.class);
        assertThat(historyEntry.getData()).containsExactlyInAnyOrderEntriesOf(ImmutableMap.of(
                "note",  "An updated note"));
    }

    @Test
    @org.junit.jupiter.api.Order(39)
    public void delete_note() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order beforeOrder = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(beforeOrder.getHistory().getItems()).hasSize(2);

        variables = objectMapper.createObjectNode();
        variables.put("id", firstNoteId);

        graphQLResponse = adminClient.perform(DELETE_ORDER_NOTE, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteOrderNote", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        variables = objectMapper.createObjectNode();
        variables.put("id", orderId);

        graphQLResponse =
                adminClient.perform(GET_ORDER_HISTORY, variables);
        Order afterOrder = graphQLResponse.get("$.data.orderByAdmin", Order.class);
        assertThat(afterOrder.getHistory().getItems()).hasSize(1);
    }
}
