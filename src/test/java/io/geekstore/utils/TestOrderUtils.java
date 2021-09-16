/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.utils;

import io.geekstore.ApiClient;
import io.geekstore.config.payment_method.PaymentMethodHandler;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.types.common.CreateAddressInput;
import io.geekstore.types.order.Order;
import io.geekstore.types.payment.PaymentInput;
import io.geekstore.types.shipping.ShippingMethodQuote;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
public class TestOrderUtils {

    @Autowired
    ObjectMapper objectMapper;

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String SET_SHIPPING_ADDRESS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_address");
    static final String GET_ELIGIBLE_SHIPPING_METHODS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_eligible_shipping_methods");
    static final String SET_SHIPPING_METHOD  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_method");
    static final String TRANSITION_TO_STATE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "transition_to_state");

    static final String ADD_PAYMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_payment");
    static final String TEST_ORDER_FRAGMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "test_order_fragment");

    public Long proceedToArrangingPayment(ApiClient shopClient) throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setFullName("name");
        input.setStreetLine1("12 the street");
        input.setCity("foo");
        input.setPostalCode("123456");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_SHIPPING_ADDRESS, variables);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_ELIGIBLE_SHIPPING_METHODS, null);
        List<ShippingMethodQuote> eligibleShippingMethods =
                graphQLResponse.getList("$.data.eligibleShippingMethods", ShippingMethodQuote.class);

        variables = objectMapper.createObjectNode();
        variables.put("id", eligibleShippingMethods.get(1).getId());
        shopClient.perform(SET_SHIPPING_METHOD, variables);

        variables = objectMapper.createObjectNode();
        variables.put("state", OrderState.ArrangingPayment.toString());
        graphQLResponse = shopClient.perform(TRANSITION_TO_STATE, variables);
        Order order = graphQLResponse.get("$.data.transitionOrderToState", Order.class);
        return order.getId();
    }

    public Order addPaymentToOrder(ApiClient shopClient, PaymentMethodHandler handler) throws IOException {
        PaymentInput input = new PaymentInput();
        input.setMethod(handler.getCode());
        input.getMetadata().put("baz", "quux");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.addPaymentToOrder", Order.class);
        return order;
    }
}
