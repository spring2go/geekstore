/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.config.payment.TestSuccessfulPaymentMethod;
import io.geekstore.config.payment_method.PaymentMethodHandler;
import io.geekstore.config.payment_method.PaymentOptions;
import io.geekstore.config.promotion.actions.FacetValuesDiscountAction;
import io.geekstore.config.promotion.actions.OrderPercentageDiscount;
import io.geekstore.config.promotion.actions.ProductDiscountAction;
import io.geekstore.config.promotion.conditions.ContainsProductsCondition;
import io.geekstore.config.promotion.conditions.CustomerGroupCondition;
import io.geekstore.config.promotion.conditions.HasFacetValuesCondition;
import io.geekstore.config.promotion.conditions.MinimumOrderAmountCondition;
import io.geekstore.service.helpers.order_state_machine.OrderState;
import io.geekstore.types.common.AdjustmentType;
import io.geekstore.types.common.ConfigArgInput;
import io.geekstore.types.common.ConfigurableOperationInput;
import io.geekstore.types.common.CreateCustomerInput;
import io.geekstore.types.customer.CreateCustomerGroupInput;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerGroup;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.facet.FacetList;
import io.geekstore.types.facet.FacetValue;
import io.geekstore.types.history.HistoryEntry;
import io.geekstore.types.history.HistoryEntryType;
import io.geekstore.types.order.Order;
import io.geekstore.types.order.OrderLine;
import io.geekstore.types.product.Product;
import io.geekstore.types.product.ProductList;
import io.geekstore.types.product.ProductListOptions;
import io.geekstore.types.product.ProductVariant;
import io.geekstore.types.promotion.CreatePromotionInput;
import io.geekstore.types.promotion.Promotion;
import io.geekstore.utils.TestHelper;
import io.geekstore.utils.TestOrderUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class OrderPromotionTest {

    static final String ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/order/%s.graphqls";
    static final String GET_PROMO_PRODUCTS  =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "get_promo_products");
    static final String DELETE_PROMOTION  =
            String.format(ADMIN_ORDER_GRAPHQL_RESOURCE_TEMPLATE, "delete_promotion");

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String FACET_WITH_VALUES_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_with_values_fragment");
    static final String FACET_VALUE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "facet_value_fragment");
    static final String GET_FACET_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_facet_list");
    static final String CREATE_PROMOTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_promotion");
    static final String PROMOTION_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "promotion_fragment");
    static final String CONFIGURABLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "configurable_fragment");
    static final String CREATE_CUSTOMER_GROUP =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_customer_group");
    static final String CUSTOMER_GROUP_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "customer_group_fragment");
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String REMOVE_CUSTOMERS_FROM_GROUP =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "remove_customers_from_group");

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String ADD_ITEM_TO_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_item_to_order");
    static final String APPLY_COUPON_CODE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "apply_coupon_code");
    static final String TEST_ORDER_FRAGMENT  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "test_order_fragment");
    static final String GET_ACTIVE_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_order");
    static final String REMOVE_COUPON_CODE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "remove_coupon_code");
    static final String ADJUST_ITEM_QUANTITY  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "adjust_item_quantity");
    static final String SET_CUSTOMER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "set_customer");
    static final String GET_ORDER_PROMOTIONS_BY_CODE  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_order_promotions_by_code");

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

    @Autowired
    MinimumOrderAmountCondition minimumOrderAmount;
    @Autowired
    OrderPercentageDiscount orderPercentageDiscount;
    @Autowired
    HasFacetValuesCondition hasFacetValuesCondition;
    @Autowired
    ContainsProductsCondition containsProductsCondition;
    @Autowired
    CustomerGroupCondition customerGroupCondition;
    @Autowired
    FacetValuesDiscountAction facetValuesDiscountAction;
    @Autowired
    ProductDiscountAction productDiscountAction;

    PaymentMethodHandler testSuccessfulPaymentMethod;

    List<Product> products;
    List<Customer> customers;
    String password = MockDataService.TEST_PASSWORD;


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
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-promotions.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_CUSTOMER_LIST, null);
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customers = customerList.getItems();

        testSuccessfulPaymentMethod = paymentOptions.getPaymentMethodHandlers().get(0);

        getProducts();
        createGlobalPromotions();
    }

    /**
     * coupon codes test casese
     */
    final String TEST_COUPON_CODE = "TESTCOUPON";
    final String EXPIRED_COUPON_CODE = "EXPIRED";

    Promotion promoFreeWithCoupon;
    Promotion promoFreeWithExpiredCoupon;

    private void before_test_01() throws IOException {
        CreatePromotionInput input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Free with test coupon");
        input.setCouponCode(TEST_COUPON_CODE);
        input.getActions().add(getFreeOrderAction());

        promoFreeWithCoupon = createPromotion(input);

        input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Expired coupon");
        Calendar calendar = Calendar.getInstance();
        calendar.set(2010, 0, 1);
        input.setStartsAt(calendar.getTime());
        input.setEndsAt(calendar.getTime());
        input.setCouponCode(EXPIRED_COUPON_CODE);
        input.getActions().add(getFreeOrderAction());

        promoFreeWithExpiredCoupon = createPromotion(input);

        shopClient.asAnonymousUser();
        ProductVariant item60 = getVariantBySlug("item-60");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item60.getId());
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    public void applyCouponCode_throws_with_nonexistant_code() throws IOException {
        before_test_01();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", "bad code");

        try {
            shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Coupon code \"{ bad code }\" is not valid"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void applyCouponCode_throws_with_expired_code() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", EXPIRED_COUPON_CODE);

        try {
            shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Coupon code \"{ " + EXPIRED_COUPON_CODE + " }\" has expired"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void apply_a_valid_coupon_code() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getCouponCodes()).containsExactly(TEST_COUPON_CODE);
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo("Free with test coupon");
        assertThat(order.getTotal()).isEqualTo(0);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void order_history_records_application() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getHistory().getItems()).hasSize(1);

        HistoryEntry historyEntry = activeOrder.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.ORDER_COUPON_APPLIED);
        assertThat(historyEntry.getData()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("couponCode", TEST_COUPON_CODE, "promotionId", "3")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void de_duplicates_existing_codes() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getCouponCodes()).containsExactly(TEST_COUPON_CODE);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void removes_a_coupon_code() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(REMOVE_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.removeCouponCode", Order.class);

        assertThat(order.getAdjustments()).hasSize(0);
        assertThat(order.getTotal()).isEqualTo(5000);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    public void order_history_records_removal() throws IOException {
        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getHistory().getItems()).hasSize(2);

        HistoryEntry historyEntry1 = activeOrder.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_COUPON_APPLIED);
        assertThat(historyEntry1.getData()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("couponCode", TEST_COUPON_CODE, "promotionId", "3")
        );

        HistoryEntry historyEntry2 = activeOrder.getHistory().getItems().get(1);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.ORDER_COUPON_REMOVED);
        assertThat(historyEntry2.getData()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("couponCode", TEST_COUPON_CODE)
        );
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    public void does_not_record_removal_of_coupon_code_that_was_not_added() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", "NOT_THERE");

        GraphQLResponse graphQLResponse =
                shopClient.perform(REMOVE_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.removeCouponCode", Order.class);

        assertThat(order.getHistory().getItems()).hasSize(2);

        HistoryEntry historyEntry1 = order.getHistory().getItems().get(0);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.ORDER_COUPON_APPLIED);
        assertThat(historyEntry1.getData()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("couponCode", TEST_COUPON_CODE, "promotionId", "3")
        );

        HistoryEntry historyEntry2 = order.getHistory().getItems().get(1);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.ORDER_COUPON_REMOVED);
        assertThat(historyEntry2.getData()).containsExactlyInAnyOrderEntriesOf(
                ImmutableMap.of("couponCode", TEST_COUPON_CODE)
        );

        // end coupon codes test cases
        deletePromotion(promoFreeWithCoupon.getId());
        deletePromotion(promoFreeWithExpiredCoupon.getId());
    }

    /**
     * default PromotionConditions test cases
     */

    @Test
    @org.junit.jupiter.api.Order(9)
    public void minimumOrderAmount() throws IOException {
        shopClient.asAnonymousUser();

        CreatePromotionInput input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Free if order total greater than 100");
        input.getConditions().add(getMinOrderAmountCondition(10000));
        input.getActions().add(getFreeOrderAction());

        Promotion promotion = createPromotion(input);

        ProductVariant item60 = getVariantBySlug("item-60");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item60.getId());
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getTotal()).isEqualTo(5000);
        assertThat(order.getAdjustments()).isEmpty();

        variables = objectMapper.createObjectNode();
        variables.put("orderLineId", order.getLines().get(0).getId());
        variables.put("quantity", 2);
        graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);
        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo(
                "Free if order total greater than 100"
        );
        assertThat(order.getAdjustments().get(0).getAmount()).isEqualTo(-10000);

        deletePromotion(promotion.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    public void atLeastNWithFacets() throws IOException {
        shopClient.asAnonymousUser();

        GraphQLResponse graphQLResponse =
                adminClient.perform(
                        GET_FACET_LIST, null, Arrays.asList(FACET_VALUE_FRAGMENT, FACET_WITH_VALUES_FRAGMENT));
        FacetList facetList = graphQLResponse.get("$.data.facets", FacetList.class);

        FacetValue saleFacetValue = facetList.getItems().get(0).getValues().get(0);

        CreatePromotionInput input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Free if order contains 2 items with Sale facet value");
        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(hasFacetValuesCondition.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("minimum");
        configArgInput.setValue("2");
        configurableOperationInput.getArguments().add(configArgInput);
        configArgInput = new ConfigArgInput();
        configArgInput.setName("facets");
        configArgInput.setValue("[\"" + saleFacetValue.getId() + "\"]");
        configurableOperationInput.getArguments().add(configArgInput);
        input.getConditions().add(configurableOperationInput);
        input.getActions().add(getFreeOrderAction());

        Promotion promotion = createPromotion(input);

        ProductVariant itemSale1 = getVariantBySlug("item-sale-1");
        ProductVariant itemSale12 = getVariantBySlug("item-sale-12");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", itemSale1.getId());
        variables.put("quantity", 1);
        graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getTotal()).isEqualTo(100);
        assertThat(order.getAdjustments()).isEmpty();

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", itemSale12.getId());
        variables.put("quantity", 1);
        graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo(
                "Free if order contains 2 items with Sale facet value"
        );
        assertThat(order.getAdjustments().get(0).getAmount()).isEqualTo(-1100);

        deletePromotion(promotion.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    public void containsProducts() throws IOException {
        shopClient.asAnonymousUser();

        ProductVariant item60 = getVariantBySlug("item-60");
        ProductVariant item12 = getVariantBySlug("item-12");

        CreatePromotionInput input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Free if buying 3 or more offer products");
        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(containsProductsCondition.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("minimum");
        configArgInput.setValue("3");
        configurableOperationInput.getArguments().add(configArgInput);
        configArgInput = new ConfigArgInput();
        configArgInput.setName("productVariantIds");
        configArgInput.setValue(objectMapper.writeValueAsString(Arrays.asList(item60.getId(), item12.getId())));
        configurableOperationInput.getArguments().add(configArgInput);
        input.getConditions().add(configurableOperationInput);
        input.getActions().add(getFreeOrderAction());

        Promotion promotion = createPromotion(input);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item60.getId());
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item12.getId());
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getTotal()).isEqualTo(6000);
        assertThat(order.getAdjustments()).isEmpty();

        variables = objectMapper.createObjectNode();
        variables.put("orderLineId", order.getLines().get(0).getId());
        variables.put("quantity", 2);
        graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);
        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo(
                "Free if buying 3 or more offer products"
        );
        assertThat(order.getAdjustments().get(0).getAmount()).isEqualTo(-11000);

        deletePromotion(promotion.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    public void customerGroup() throws IOException {
        shopClient.asAnonymousUser();

        CreateCustomerGroupInput input = new CreateCustomerGroupInput();
        input.setName("Test Group");
        input.getCustomerIds().add(customers.get(0).getId());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                CREATE_CUSTOMER_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        CustomerGroup createdCustomerGroup =
                graphQLResponse.get("$.data.createCustomerGroup", CustomerGroup.class);

        shopClient.asUserWithCredentials(customers.get(0).getEmailAddress(), password);

        CreatePromotionInput createPromotionInput = new CreatePromotionInput();
        createPromotionInput.setEnabled(true);
        createPromotionInput.setName("Free for group members");
        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(customerGroupCondition.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("customerGroupId");
        configArgInput.setValue(createdCustomerGroup.getId().toString());
        configurableOperationInput.getArguments().add(configArgInput);
        createPromotionInput.getConditions().add(configurableOperationInput);
        createPromotionInput.getActions().add(getFreeOrderAction());

        Promotion promotion = createPromotion(createPromotionInput);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", getVariantBySlug("item-60").getId());
        variables.put("quantity", 1);
        graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo("Free for group members");
        assertThat(order.getAdjustments().get(0).getAmount()).isEqualTo(-5000);

        variables = objectMapper.createObjectNode();
        variables.put("groupId", createdCustomerGroup.getId());
        variables.putArray("customerIds").add(customers.get(0).getId());

        adminClient.perform(REMOVE_CUSTOMERS_FROM_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        customerGroupCondition.clearCache(); // 清除customerGroupCondition对customerId的缓存

        variables = objectMapper.createObjectNode();
        variables.put("orderLineId", order.getLines().get(0).getId());
        variables.put("quantity", 2);
        graphQLResponse = shopClient.perform(ADJUST_ITEM_QUANTITY, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.adjustOrderLine", Order.class);

        assertThat(order.getTotal()).isEqualTo(10000);
        assertThat(order.getAdjustments()).isEmpty();

        deletePromotion(promotion.getId());
    }

    /**
     * default PromotionActions test cases
     */
    @Test
    @org.junit.jupiter.api.Order(13)
    public void orderPercentageDiscount() throws IOException {
        shopClient.asAnonymousUser();

        String couponCode = "50%_off_order";

        CreatePromotionInput createPromotionInput = new CreatePromotionInput();
        createPromotionInput.setEnabled(true);
        createPromotionInput.setName("50% discount on order");
        createPromotionInput.setCouponCode(couponCode);

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(orderPercentageDiscount.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("discount");
        configArgInput.setValue("50");
        configurableOperationInput.getArguments().add(configArgInput);
        createPromotionInput.getActions().add(configurableOperationInput);

        Promotion promotion = createPromotion(createPromotionInput);

        ProductVariant item60 = getVariantBySlug("item-60");
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item60.getId());
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getTotal()).isEqualTo(5000);
        assertThat(order.getAdjustments()).isEmpty();

        variables = objectMapper.createObjectNode();
        variables.put("couponCode", couponCode);

        graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo("50% discount on order");
        assertThat(order.getTotal()).isEqualTo(2500);

        deletePromotion(promotion.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(14)
    public void discountOnItemWithFacets() throws IOException {
        shopClient.asAnonymousUser();

        GraphQLResponse graphQLResponse =
                adminClient.perform(
                        GET_FACET_LIST, null, Arrays.asList(FACET_VALUE_FRAGMENT, FACET_WITH_VALUES_FRAGMENT));
        FacetList facetList = graphQLResponse.get("$.data.facets", FacetList.class);

        FacetValue saleFacetValue = facetList.getItems().get(0).getValues().get(0);

        String couponCode = "50%_off_sale_items";

        CreatePromotionInput createPromotionInput = new CreatePromotionInput();
        createPromotionInput.setEnabled(true);
        createPromotionInput.setName("50% off sale items");
        createPromotionInput.setCouponCode(couponCode);

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValuesDiscountAction.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("discount");
        configArgInput.setValue("50");
        configurableOperationInput.getArguments().add(configArgInput);
        configArgInput = new ConfigArgInput();
        configArgInput.setName("facets");
        configArgInput.setValue("[\"" + saleFacetValue.getId() + "\"]");
        configurableOperationInput.getArguments().add(configArgInput);
        createPromotionInput.getActions().add(configurableOperationInput);

        Promotion promotion = createPromotion(createPromotionInput);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", getVariantBySlug("item-12").getId());
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", getVariantBySlug("item-sale-12").getId());
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        variables = objectMapper.createObjectNode();
        variables.put("productVariantId", getVariantBySlug("item-sale-1").getId());
        variables.put("quantity", 2);
        graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);
        assertThat(order.getAdjustments()).isEmpty();
        assertThat(getItemSale1Line(order.getLines()).getAdjustments()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(2200);

        variables = objectMapper.createObjectNode();
        variables.put("couponCode", couponCode);

        graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getTotal()).isEqualTo(1600);
        assertThat(getItemSale1Line(order.getLines()).getAdjustments()).hasSize(2);

        variables = objectMapper.createObjectNode();
        variables.put("couponCode", couponCode);

        graphQLResponse =
                shopClient.perform(REMOVE_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.removeCouponCode", Order.class);

        assertThat(getItemSale1Line(order.getLines()).getAdjustments()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(2200);

        graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(getItemSale1Line(activeOrder.getLines()).getAdjustments()).isEmpty();
        assertThat(activeOrder.getTotal()).isEqualTo(2200);
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    public void productsPercentageDiscount() throws IOException {
        shopClient.asAnonymousUser();

        ProductVariant item60 = getVariantBySlug("item-60");
        String couponCode = "50%_off_product";

        CreatePromotionInput createPromotionInput = new CreatePromotionInput();
        createPromotionInput.setEnabled(true);
        createPromotionInput.setName("50% off product");
        createPromotionInput.setCouponCode(couponCode);

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(productDiscountAction.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("discount");
        configArgInput.setValue("50");
        configurableOperationInput.getArguments().add(configArgInput);
        configArgInput = new ConfigArgInput();
        configArgInput.setName("productVariantIds");
        configArgInput.setValue("[\"" + item60.getId() + "\"]");
        configurableOperationInput.getArguments().add(configArgInput);
        createPromotionInput.getActions().add(configurableOperationInput);

        Promotion promotion = createPromotion(createPromotionInput);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item60.getId());
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        assertThat(order.getAdjustments()).isEmpty();
        assertThat(order.getLines().get(0).getAdjustments()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(5000);

        variables = objectMapper.createObjectNode();
        variables.put("couponCode", couponCode);

        graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getTotal()).isEqualTo(2500);
        assertThat(order.getLines().get(0).getAdjustments()).hasSize(1);

        variables = objectMapper.createObjectNode();
        variables.put("couponCode", couponCode);

        graphQLResponse =
                shopClient.perform(REMOVE_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.removeCouponCode", Order.class);

        assertThat(order.getLines().get(0).getAdjustments()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(5000);

        deletePromotion(promotion.getId());
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    public void multiple_promotions_simultaneously() throws IOException {
        shopClient.asAnonymousUser();

        GraphQLResponse graphQLResponse =
                adminClient.perform(
                        GET_FACET_LIST, null, Arrays.asList(FACET_VALUE_FRAGMENT, FACET_WITH_VALUES_FRAGMENT));
        FacetList facetList = graphQLResponse.get("$.data.facets", FacetList.class);

        FacetValue saleFacetValue = facetList.getItems().get(0).getValues().get(0);

        CreatePromotionInput createPromotionInput = new CreatePromotionInput();
        createPromotionInput.setEnabled(true);
        createPromotionInput.setName("item promo");
        createPromotionInput.setCouponCode("CODE1");

        ConfigurableOperationInput configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(facetValuesDiscountAction.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("discount");
        configArgInput.setValue("50");
        configurableOperationInput.getArguments().add(configArgInput);
        configArgInput = new ConfigArgInput();
        configArgInput.setName("facets");
        configArgInput.setValue("[\"" + saleFacetValue.getId() + "\"]");
        configurableOperationInput.getArguments().add(configArgInput);
        createPromotionInput.getActions().add(configurableOperationInput);

        Promotion promotion1 = createPromotion(createPromotionInput);

        createPromotionInput = new CreatePromotionInput();
        createPromotionInput.setEnabled(true);
        createPromotionInput.setName("order promo");
        createPromotionInput.setCouponCode("CODE2");

        configurableOperationInput = new ConfigurableOperationInput();
        configurableOperationInput.setCode(orderPercentageDiscount.getCode());
        configArgInput = new ConfigArgInput();
        configArgInput.setName("discount");
        configArgInput.setValue("50");
        configurableOperationInput.getArguments().add(configArgInput);
        createPromotionInput.getActions().add(configurableOperationInput);

        Promotion promotion2 = createPromotion(createPromotionInput);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", getVariantBySlug("item-sale-12").getId());
        variables.put("quantity", 1);
        shopClient.perform(ADD_ITEM_TO_ORDER, variables);

        // Apply the OrderItem-level promo
        variables = objectMapper.createObjectNode();
        variables.put("couponCode", "CODE1");

        graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getLines().get(0).getAdjustments()).hasSize(1);
        assertThat(order.getLines().get(0).getAdjustments().stream()
                .filter(a ->  Objects.equals(a.getType(), AdjustmentType.PROMOTION))
                .findFirst()
                .get()
                .getDescription()
        ).isEqualTo("item promo");
        assertThat(order.getAdjustments()).isEmpty();

        // Apply the Order-level promo
        variables = objectMapper.createObjectNode();
        variables.put("couponCode", "CODE2");

        graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getLines().get(0).getAdjustments()).hasSize(1);
        assertThat(order.getLines().get(0).getAdjustments().stream()
                .filter(a ->  Objects.equals(a.getType(), AdjustmentType.PROMOTION))
                .findFirst()
                .get()
                .getDescription()
        ).isEqualTo("item promo");
        assertThat(order.getAdjustments()).hasSize(1);
        assertThat(order.getAdjustments().get(0).getDescription()).isEqualTo("order promo");
    }

    private OrderLine getItemSale1Line(List<OrderLine> lines) {
        return lines.stream()
                .filter(l -> Objects.equals(l.getProductVariant().getId(), getVariantBySlug("item-sale-1").getId()))
                .findFirst().get();
    }

    /**
     * per-customer usage limit
     */
    Promotion promoWithUsageLimit;

    private void before_test_16() throws IOException {
        CreatePromotionInput input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Free with test coupon");
        input.setCouponCode(TEST_COUPON_CODE);
        input.setPerCustomerUsageLimit(1);
        input.getActions().add(getFreeOrderAction());

        Promotion promoWithUsageLimit = createPromotion(input);
    }

    private Order createNewActiveOrder() throws IOException {
        ProductVariant item60 = getVariantBySlug("item-60");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", item60.getId());
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        Order order = graphQLResponse.get("$.data.addItemToOrder", Order.class);

        return order;
    }

    /**
     * guest customer
     */
    String GUEST_EMAIL_ADDRESS = "guest@test.com";
    String orderCode;

    private void addGuestCustomerToOrder() throws IOException {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress(GUEST_EMAIL_ADDRESS);
        input.setFirstName("Guest");
        input.setLastName("Customer");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        shopClient.perform(SET_CUSTOMER, variables);
    }


    @Test
    @org.junit.jupiter.api.Order(16)
    public void allows_initial_usage() throws IOException {
        before_test_16();
        shopClient.asAnonymousUser();
        createNewActiveOrder();
        addGuestCustomerToOrder();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getCouponCodes()).containsExactly(TEST_COUPON_CODE);

        testOrderUtils.proceedToArrangingPayment(shopClient);
        order = testOrderUtils.addPaymentToOrder(shopClient, testSuccessfulPaymentMethod);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
        assertThat(order.getActive()).isFalse();
        orderCode = order.getCode();
    }

    @Test
    @org.junit.jupiter.api.Order(17)
    public void adds_Promotions_to_Order_once_payment_arranged() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("code", orderCode);

        GraphQLResponse graphQLResponse =
                shopClient.perform(GET_ORDER_PROMOTIONS_BY_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.orderByCode", Order.class);

        assertThat(order.getPromotions()).hasSize(1);
        assertThat(order.getPromotions().get(0).getName()).isEqualTo("Free with test coupon");
    }

    @Test
    @org.junit.jupiter.api.Order(18)
    public void throws_when_usage_exceeds_limit() throws IOException {
        shopClient.asAnonymousUser();
        createNewActiveOrder();
        addGuestCustomerToOrder();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        try {
            shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Coupon code cannot be used more than 1 time(s) per customer"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(19)
    public void removes_couponCode_from_order_when_adding_customer_after_code_applied() throws IOException {
        shopClient.asAnonymousUser();
        createNewActiveOrder();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getCouponCodes()).containsExactly(TEST_COUPON_CODE);

        addGuestCustomerToOrder();

        graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);
        assertThat(activeOrder.getCouponCodes()).isEmpty();
        assertThat(activeOrder.getTotal()).isEqualTo(5000);
    }

    /**
     * signed-in customer
     */

    private void loginAsRegisteredCustomer() throws IOException {
        shopClient.asUserWithCredentials(customers.get(0).getEmailAddress(), password);
    }

    @Test
    @org.junit.jupiter.api.Order(20)
    public void allows_initial_usage_02() throws IOException {
        loginAsRegisteredCustomer();
        createNewActiveOrder();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getCouponCodes()).containsExactly(TEST_COUPON_CODE);

        testOrderUtils.proceedToArrangingPayment(shopClient);
        order = testOrderUtils.addPaymentToOrder(shopClient, testSuccessfulPaymentMethod);
        assertThat(order.getState()).isEqualTo(OrderState.PaymentSettled.name());
        assertThat(order.getActive()).isFalse();
    }

    @Test
    @org.junit.jupiter.api.Order(21)
    public void throws_when_usage_exceeds_limit_02() throws IOException {
        loginAsRegisteredCustomer();
        createNewActiveOrder();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        try {
            shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "Coupon code cannot be used more than 1 time(s) per customer"
            );
        }
    }

    @Test
    @org.junit.jupiter.api.Order(22)
    public void removes_couponCode_from_order_when_logging_in_after_code_applied() throws IOException {
        shopClient.asAnonymousUser();
        createNewActiveOrder();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("couponCode", TEST_COUPON_CODE);

        GraphQLResponse graphQLResponse =
                shopClient.perform(APPLY_COUPON_CODE, variables, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order order = graphQLResponse.get("$.data.applyCouponCode", Order.class);

        assertThat(order.getCouponCodes()).containsExactly(TEST_COUPON_CODE);
        assertThat(order.getTotal()).isEqualTo(0);

        loginAsRegisteredCustomer();

        graphQLResponse =
                shopClient.perform(GET_ACTIVE_ORDER, null, Arrays.asList(TEST_ORDER_FRAGMENT));
        Order activeOrder = graphQLResponse.get("$.data.activeOrder", Order.class);

        assertThat(activeOrder.getTotal()).isEqualTo(5000);
        assertThat(activeOrder.getCouponCodes()).isEmpty();
    }

    private void getProducts() throws IOException {
        ProductListOptions options = new ProductListOptions();
        options.setPageSize(10);
        options.setCurrentPage(1);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_PROMO_PRODUCTS, variables);
        ProductList productList = graphQLResponse.get("$.data.adminProducts", ProductList.class);

        products = productList.getItems();
    }

    private void createGlobalPromotions() throws IOException {
        CreatePromotionInput input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Promo not yet started");
        Calendar calendar = Calendar.getInstance();
        calendar.set(2199, 0, 1);
        input.setStartsAt(calendar.getTime());
        input.getConditions().add(getMinOrderAmountCondition(100));
        input.getActions().add(getFreeOrderAction());

        this.createPromotion(input);

        input = new CreatePromotionInput();
        input.setEnabled(true);
        input.setName("Deleted promotion");
        input.getConditions().add(getMinOrderAmountCondition(100));
        input.getActions().add(getFreeOrderAction());

        Promotion promotion = createPromotion(input);
        deletePromotion(promotion.getId());
    }

    private Promotion createPromotion(CreatePromotionInput input) throws IOException {
        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                CREATE_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Promotion promotion = graphQLResponse.get("$.data.createPromotion", Promotion.class);
        return promotion;
    }

    private ProductVariant getVariantBySlug(String slug) {
        return products.stream().filter(p -> Objects.equals(p.getSlug(), slug)).findFirst().get().getVariants().get(0);
    }

    private void deletePromotion(Long id) throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", id);

        adminClient.perform(DELETE_PROMOTION, variables);
    }

    private ConfigurableOperationInput getMinOrderAmountCondition(Integer amount) {
        ConfigurableOperationInput input = new ConfigurableOperationInput();
        input.setCode(minimumOrderAmount.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("amount");
        configArgInput.setValue(amount.toString());
        input.getArguments().add(configArgInput);
        return input;
    }

    private ConfigurableOperationInput getFreeOrderAction() {
        ConfigurableOperationInput input = new ConfigurableOperationInput();
        input.setCode(orderPercentageDiscount.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("discount");
        configArgInput.setValue("100");
        input.getArguments().add(configArgInput);
        return input;
    }
}
