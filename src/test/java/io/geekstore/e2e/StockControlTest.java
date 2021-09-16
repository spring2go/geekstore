/*
 * Copyright (c) 2021 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.config.payment.TestSuccessfulPaymentMethod;
import io.geekstore.config.payment_method.PaymentOptions;
import io.geekstore.types.common.CreateAddressInput;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.payment.PaymentInput;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductVariant;
import io.geekstore.types.product.UpdateProductVariantInput;
import io.geekstore.types.stock.StockMovementType;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created on Jan, 2021 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class StockControlTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_STOCK_MOVEMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_stock_movement");
    static final String VARIANT_WITH_STOCK_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "variant_with_stock_fragment");
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String ADD_ITEM_TO_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_item_to_order");
    static final String SET_SHIPPING_ADDRESS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_shipping_address");
    static final String TRANSITION_TO_STATE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "transition_to_state");
    static final String ADD_PAYMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_payment");
    static final String TEST_ORDER_FRAGMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "test_order_fragment");

    static final String ADMIN_STOCK_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/stock/%s.graphqls";
    static final String UPDATE_STOCK_ON_HAND  =
            String.format(ADMIN_STOCK_GRAPHQL_RESOURCE_TEMPLATE, "update_stock_on_hand");

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

    List<ProductVariant> variants;

    TestSuccessfulPaymentMethod testSuccessfulPaymentMethod;

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
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(2).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-stock-control.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        testSuccessfulPaymentMethod = (TestSuccessfulPaymentMethod) paymentOptions.getPaymentMethodHandlers().get(0);
    }

    /**
     * stock adjustments
     */

    @Test
    @org.junit.jupiter.api.Order(1)
    public void stockMovements_are_initially_empty() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);

        variants = product.getVariants();
        variants.forEach(variant -> {
            assertThat(variant.getStockMovements().getItems()).isEmpty();
            assertThat(variant.getStockMovements().getTotalItems()).isEqualTo(0);
        });
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void updating_ProductVariant_with_same_stockOnHand_does_not_create_a_StockMovement() throws IOException {
        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(variants.get(0).getId());
        input.setStockOnHand(variants.get(0).getStockOnHand());

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(input));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_STOCK_ON_HAND, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);

        assertThat(updatedProductVariants.get(0).getStockMovements().getItems()).isEmpty();
        assertThat(updatedProductVariants.get(0).getStockMovements().getTotalItems()).isEqualTo(0);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void increasing_stockOnHand_creates_a_StockMovement_with_correct_quantity() throws IOException {
        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(variants.get(0).getId());
        input.setStockOnHand(variants.get(0).getStockOnHand() + 5);

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(input));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_STOCK_ON_HAND, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);

        assertThat(updatedProductVariants.get(0).getStockOnHand()).isEqualTo(5);
        assertThat(updatedProductVariants.get(0).getStockMovements().getTotalItems()).isEqualTo(1);
        assertThat(updatedProductVariants.get(0).getStockMovements().getItems().get(0).getType())
                .isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(updatedProductVariants.get(0).getStockMovements().getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void decreasing_stockOnHand_creates_a_StockMovement_with_correct_quantity() throws IOException {
        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(variants.get(0).getId());
        input.setStockOnHand(variants.get(0).getStockOnHand() + 5 - 2);

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(input));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_STOCK_ON_HAND, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        List<ProductVariant> updatedProductVariants =
                graphQLResponse.getList("$.data.updateProductVariants", ProductVariant.class);

        assertThat(updatedProductVariants.get(0).getStockOnHand()).isEqualTo(3);
        assertThat(updatedProductVariants.get(0).getStockMovements().getTotalItems()).isEqualTo(2);
        assertThat(updatedProductVariants.get(0).getStockMovements().getItems().get(1).getType())
                .isEqualTo(StockMovementType.ADJUSTMENT);
        assertThat(updatedProductVariants.get(0).getStockMovements().getItems().get(1).getQuantity()).isEqualTo(-2);
    }


    @Test
    @org.junit.jupiter.api.Order(5)
    public void attempting_to_set_a_negative_stockOnHand_throws() throws IOException {
        UpdateProductVariantInput input = new UpdateProductVariantInput();
        input.setId(variants.get(0).getId());
        input.setStockOnHand(-1);

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(input));
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(UPDATE_STOCK_ON_HAND, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "stockOnHand cannot be a negative value"
            );
        }
    }

    /**
     * sales
     */

    List<Customer> customers;

    private void before_test_06() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_CUSTOMER_LIST, null);
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customers = customerList.getItems();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        graphQLResponse =
                adminClient.perform(GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);

        ProductVariant variant1 = product.getVariants().get(0);
        ProductVariant variant2 = product.getVariants().get(1);

        UpdateProductVariantInput input1 = new UpdateProductVariantInput();
        input1.setId(variant1.getId());
        input1.setStockOnHand(5);
        input1.setTrackInventory(false);

        UpdateProductVariantInput input2 = new UpdateProductVariantInput();
        input2.setId(variant2.getId());
        input2.setStockOnHand(5);
        input2.setTrackInventory(true);

        JsonNode inputNode = objectMapper.valueToTree(Arrays.asList(input1, input2));
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        adminClient.perform(UPDATE_STOCK_ON_HAND, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));

        // add items to order and check out
        shopClient.asUserWithCredentials(customers.get(0).getEmailAddress(), MockDataService.TEST_PASSWORD);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", variant1.getId());
        variables.put("quantity", 2);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", variant2.getId());
        variables.put("quantity", 3);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        CreateAddressInput input = new CreateAddressInput();
        input.setStreetLine1("1 Test Street");

        inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_SHIPPING_ADDRESS, variables);

        variables = objectMapper.createObjectNode();
        variables.put("state", "ArrangingPayment");
        shopClient.perform(TRANSITION_TO_STATE, variables);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void creates_a_Sale_when_order_completed() throws IOException {
        before_test_06();

        PaymentInput input = new PaymentInput();
        input.setMethod(testSuccessfulPaymentMethod.getCode());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(ADD_PAYMENT, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        io.geekstore.types.order.Order order =
                graphQLResponse.get("$.data.addPaymentToOrder", io.geekstore.types.order.Order.class);

        assertThat(order).isNotNull();

        variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        graphQLResponse =
                adminClient.perform(GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);

        ProductVariant variant1 = product.getVariants().get(0);
        ProductVariant variant2 = product.getVariants().get(1);

        assertThat(variant1.getStockMovements().getTotalItems()).isEqualTo(2);
        assertThat(variant1.getStockMovements().getItems().get(1).getType()).isEqualTo(StockMovementType.SALE);
        assertThat(variant1.getStockMovements().getItems().get(1).getQuantity()).isEqualTo(-2);

        assertThat(variant2.getStockMovements().getTotalItems()).isEqualTo(2);
        assertThat(variant2.getStockMovements().getItems().get(1).getType()).isEqualTo(StockMovementType.SALE);
        assertThat(variant2.getStockMovements().getItems().get(1).getQuantity()).isEqualTo(-3);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    public void stockOnHand_is_updated_according_to_trackInventory_setting() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_STOCK_MOVEMENT, variables, Arrays.asList(VARIANT_WITH_STOCK_FRAGMENT));
        Product product = graphQLResponse.get("$.data.adminProduct", Product.class);

        ProductVariant variant1 = product.getVariants().get(0);
        ProductVariant variant2 = product.getVariants().get(1);

        assertThat(variant1.getStockOnHand()).isEqualTo(5); // untracked inventory
        assertThat(variant2.getStockOnHand()).isEqualTo(2); // tracked inventory
    }

}
