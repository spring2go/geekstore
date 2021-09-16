/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.TestConfig;
import io.geekstore.config.promotion.PromotionAction;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.config.promotion.PromotionOptions;
import io.geekstore.config.promotion.PromotionOrderAction;
import io.geekstore.entity.OrderEntity;
import io.geekstore.types.common.*;
import io.geekstore.types.promotion.CreatePromotionInput;
import io.geekstore.types.promotion.Promotion;
import io.geekstore.types.promotion.PromotionList;
import io.geekstore.types.promotion.UpdatePromotionInput;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created on Dec, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class PromotionTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String CREATE_PROMOTION =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_promotion");
    static final String PROMOTION_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "promotion_fragment");
    static final String CONFIGURABLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "configurable_fragment");

    static final String PROMOTION_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/promotion/%s.graphqls";
    static final String UPDATE_PROMOTION =
            String.format(PROMOTION_GRAPHQL_RESOURCE_TEMPLATE, "update_promotion");
    static final String GET_PROMOTION =
            String.format(PROMOTION_GRAPHQL_RESOURCE_TEMPLATE, "get_promotion");
    static final String GET_PROMOTION_LIST =
            String.format(PROMOTION_GRAPHQL_RESOURCE_TEMPLATE, "get_promotion_list");
    static final String GET_ADJUSTMENT_OPERATIONS =
            String.format(PROMOTION_GRAPHQL_RESOURCE_TEMPLATE, "get_adjustment_operations");
    static final String CONFIGURABLE_DEF_FRAGMENT =
            String.format(PROMOTION_GRAPHQL_RESOURCE_TEMPLATE, "configurable_def_fragment");
    static final String DELETE_PROMOTION =
            String.format(PROMOTION_GRAPHQL_RESOURCE_TEMPLATE, "delete_promotion");


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
    PromotionOptions promotionOptions;

    PromotionCondition promoCondition;
    PromotionCondition promoCondition2;
    PromotionAction promoAction;

    Promotion promotion;

    @TestConfiguration
    static class ContextConfiguration {

        PromotionCondition promoCondition = generateTestCondition("promo_condition");
        PromotionCondition promoCondition2 = generateTestCondition("promo_condition2");
        PromotionAction promoAction = generateTestAction("promo_action");

        @Bean
        @Primary
        public PromotionOptions testPromotionOptions() {
            return new PromotionOptions(
                    Arrays.asList(promoCondition, promoCondition2),
                    Arrays.asList(promoAction)
            );
        }

        PromotionCondition generateTestCondition(String code) {
            return new PromotionCondition(
                    code,
                    "description for " + code) {
                @Override
                public boolean check(OrderEntity order, ConfigArgValues argValues) {
                    return true;
                }

                @Override
                public Map<String, ConfigArgDefinition> getArgSpec() {
                    ConfigArgDefinition argDef = new ConfigArgDefinition();
                    argDef.setType("int");
                    return ImmutableMap.of("arg", argDef);
                }
            };
        }

        PromotionOrderAction generateTestAction(String code) {
            return new PromotionOrderAction(
                    code,
                    "description for " + code) {
                @Override
                public float execute(OrderEntity order, ConfigArgValues argValues) {
                    return 42;
                }

                @Override
                public Map<String, ConfigArgDefinition> getArgSpec() {
                    ConfigArgDefinition argDef = new ConfigArgDefinition();
                    argDef.setType("ID");
                    argDef.setList(true);
                    return ImmutableMap.of("facetValueIds", argDef);
                }
            };
        }
    }

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        promoCondition = promotionOptions.getPromotionConditions().get(0);
        promoCondition2 = promotionOptions.getPromotionConditions().get(1);
        promoAction = promotionOptions.getPromotionActions().get(0);
    }

    @Test
    @Order(1)
    public void createPromotion() throws IOException, ParseException {
        CreatePromotionInput input = new CreatePromotionInput();
        input.setName("test promotion");
        input.setEnabled(true);
        input.setCouponCode("TEST123");
        input.setStartsAt(convertToDate("2019-10-30T00:00:00 UTC"));
        input.setEndsAt(convertToDate("2019-12-01T00:00:00 UTC"));

        ConfigurableOperationInput condition = new ConfigurableOperationInput();
        condition.setCode(promoCondition.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("arg");
        configArgInput.setValue("500");
        condition.getArguments().add(configArgInput);
        input.getConditions().add(condition);

        ConfigurableOperationInput action = new ConfigurableOperationInput();
        action.setCode(promoAction.getCode());
        configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"1\"]");
        action.getArguments().add(configArgInput);
        input.getActions().add(action);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                CREATE_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
        promotion = graphQLResponse.get("$.data.createPromotion", Promotion.class);
        assertThat(promotion.getCouponCode()).isEqualTo("TEST123");
        assertThat(promotion.getEnabled()).isTrue();
        assertThat(promotion.getName()).isEqualTo("test promotion");
        assertThat(format(promotion.getStartsAt())).isEqualTo("2019-10-30T00:00:00 UTC");
        assertThat(format(promotion.getEndsAt())).isEqualTo("2019-12-01T00:00:00 UTC");

        assertThat(promotion.getConditions()).hasSize(1);
        ConfigurableOperation conditionOp = promotion.getConditions().get(0);
        assertThat(conditionOp.getCode()).isEqualTo("promo_condition");
        assertThat(conditionOp.getArgs().get(0).getName()).isEqualTo("arg");
        assertThat(conditionOp.getArgs().get(0).getValue()).isEqualTo("500");

        assertThat(promotion.getActions()).hasSize(1);
        ConfigurableOperation actionOp = promotion.getActions().get(0);
        assertThat(actionOp.getCode()).isEqualTo("promo_action");
        assertThat(actionOp.getArgs().get(0).getName()).isEqualTo("facetValueIds");
        assertThat(actionOp.getArgs().get(0).getValue()).isEqualTo("[\"1\"]");
    }

    private Date convertToDate(String dateString) throws ParseException {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss zzz");
        return format.parse(dateString);
    }

    private String format(Date date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss zzz");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

    @Test
    @Order(2)
    public void createPromotion_throws_with_empty_conditions_and_no_couponCode() throws IOException {
        CreatePromotionInput input = new CreatePromotionInput();
        input.setName("bad promotion");
        input.setEnabled(true);

        ConfigurableOperationInput action = new ConfigurableOperationInput();
        action.setCode(promoAction.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("facetValueIds");
        configArgInput.setValue("[\"1\"]");
        action.getArguments().add(configArgInput);
        input.getActions().add(action);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(
                    CREATE_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiException) {
            assertThat(apiException.getErrorMessage()).isEqualTo(
                    "A Promotion must have either at least one condition or a coupon code set"
            );
        }
    }

    @Test
    @Order(3)
    public void updatePromotion() throws IOException, ParseException {
        UpdatePromotionInput input = new UpdatePromotionInput();
        input.setId(promotion.getId());
        input.setCouponCode("TEST1235");
        input.setStartsAt(convertToDate("2019-05-30T22:00:00 UTC"));
        input.setEndsAt(convertToDate("2019-06-01T22:00:00 UTC"));

        ConfigurableOperationInput condition = new ConfigurableOperationInput();
        condition.setCode(promoCondition.getCode());
        ConfigArgInput configArgInput = new ConfigArgInput();
        configArgInput.setName("arg");
        configArgInput.setValue("90");
        condition.getArguments().add(configArgInput);
        input.setConditions(new ArrayList<>());
        input.getConditions().add(condition);

        condition = new ConfigurableOperationInput();
        condition.setCode(promoCondition2.getCode());
        configArgInput = new ConfigArgInput();
        configArgInput.setName("arg");
        configArgInput.setValue("10");
        condition.getArguments().add(configArgInput);
        input.getConditions().add(condition);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = adminClient.perform(
                UPDATE_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Promotion updatedPromotion = graphQLResponse.get("$.data.updatePromotion", Promotion.class);
        assertThat(updatedPromotion.getCouponCode()).isEqualTo("TEST1235");
        assertThat(updatedPromotion.getEnabled()).isTrue();
        assertThat(updatedPromotion.getName()).isEqualTo("test promotion");
        assertThat(format(updatedPromotion.getStartsAt())).isEqualTo("2019-05-30T22:00:00 UTC");
        assertThat(format(updatedPromotion.getEndsAt())).isEqualTo("2019-06-01T22:00:00 UTC");

        assertThat(updatedPromotion.getConditions()).hasSize(2);
        ConfigurableOperation conditionOp1 = updatedPromotion.getConditions().get(0);
        assertThat(conditionOp1.getCode()).isEqualTo("promo_condition");
        assertThat(conditionOp1.getArgs().get(0).getName()).isEqualTo("arg");
        assertThat(conditionOp1.getArgs().get(0).getValue()).isEqualTo("90");

        ConfigurableOperation conditionOp2 = updatedPromotion.getConditions().get(1);
        assertThat(conditionOp2.getCode()).isEqualTo("promo_condition2");
        assertThat(conditionOp2.getArgs().get(0).getName()).isEqualTo("arg");
        assertThat(conditionOp2.getArgs().get(0).getValue()).isEqualTo("10");

        assertThat(updatedPromotion.getActions()).hasSize(1);
        ConfigurableOperation actionOp = updatedPromotion.getActions().get(0);
        assertThat(actionOp.getCode()).isEqualTo("promo_action");
        assertThat(actionOp.getArgs().get(0).getName()).isEqualTo("facetValueIds");
        assertThat(actionOp.getArgs().get(0).getValue()).isEqualTo("[\"1\"]");
    }


    @Test
    @Order(4)
    public void updatePromotion_throws_with_empty_conditions_and_no_couponCode() throws IOException {
        UpdatePromotionInput input = new UpdatePromotionInput();
        input.setId(promotion.getId());
        input.setCouponCode("");
        input.setConditions(new ArrayList<>());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(
                    UPDATE_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                  "A Promotion must have either at least one condition or a coupon code set"
            );
        }
    }

    @Test
    @Order(5)
    public void promotion() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", promotion.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Promotion promotion = graphQLResponse.get("$.data.promotion", Promotion.class);
        assertThat(promotion.getName()).isEqualTo(promotion.getName());
    }

    @Test
    @Order(6)
    public void promotions() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_PROMOTION_LIST, null, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT)
        );
        PromotionList promotionList = graphQLResponse.get("$.data.promotions", PromotionList.class);
        assertThat(promotionList.getTotalItems()).isEqualTo(1);
        assertThat(promotionList.getItems().get(0).getName()).isEqualTo("test promotion");
    }

    @Test
    @Order(7)
    public void adjustmentOperations() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_ADJUSTMENT_OPERATIONS, null, Arrays.asList(CONFIGURABLE_DEF_FRAGMENT)
        );
        List<ConfigurableOperationDefinition> promotionActions =
                graphQLResponse.getList("$.data.promotionActions", ConfigurableOperationDefinition.class);
        assertThat(promotionActions).hasSize(1);
        ConfigurableOperationDefinition actionDef = promotionActions.get(0);
        assertThat(actionDef.getCode()).isEqualTo("promo_action");
        assertThat(actionDef.getDescription()).isEqualTo("description for promo_action");
        assertThat(actionDef.getArgs().get(0).getName()).isEqualTo("facetValueIds");
        assertThat(actionDef.getArgs().get(0).getType()).isEqualTo("ID");
        assertThat(actionDef.getArgs().get(0).getUi()).isEmpty();

        List<ConfigurableOperationDefinition> promotionConditions =
                graphQLResponse.getList("$.data.promotionConditions", ConfigurableOperationDefinition.class);
        assertThat(promotionConditions).hasSize(2);
        ConfigurableOperationDefinition conditionDef1 = promotionConditions.get(0);
        assertThat(conditionDef1.getCode()).isEqualTo("promo_condition");
        assertThat(conditionDef1.getDescription()).isEqualTo("description for promo_condition");
        assertThat(conditionDef1.getArgs().get(0).getName()).isEqualTo("arg");
        assertThat(conditionDef1.getArgs().get(0).getType()).isEqualTo("int");
        assertThat(conditionDef1.getArgs().get(0).getUi()).isEmpty();

        ConfigurableOperationDefinition conditionDef2 = promotionConditions.get(1);
        assertThat(conditionDef2.getCode()).isEqualTo("promo_condition2");
        assertThat(conditionDef2.getDescription()).isEqualTo("description for promo_condition2");
        assertThat(conditionDef2.getArgs().get(0).getName()).isEqualTo("arg");
        assertThat(conditionDef2.getArgs().get(0).getType()).isEqualTo("int");
        assertThat(conditionDef2.getArgs().get(0).getUi()).isEmpty();
    }

    // deletion
    List<Promotion> allPromotions;
    Promotion promotionToDelete;

    private void before_test_8() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_PROMOTION_LIST, null, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT)
        );
        PromotionList promotionList = graphQLResponse.get("$.data.promotions", PromotionList.class);
        allPromotions = promotionList.getItems();
    }

    @Test
    @Order(8)
    public void deletes_a_promotion() throws IOException {
        before_test_8();

        promotionToDelete = allPromotions.get(0);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", promotionToDelete.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(
                DELETE_PROMOTION, variables
        );
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deletePromotion", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);
    }

    @Test
    @Order(9)
    public void cannot_get_a_deleted_promotion() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", promotionToDelete.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
        Promotion promotion = graphQLResponse.get("$.data.promotion", Promotion.class);
        assertThat(promotion).isNull();
    }

    @Test
    @Order(10)
    public void deleted_promotion_omitted_from_list() throws IOException {
        GraphQLResponse graphQLResponse = adminClient.perform(
                GET_PROMOTION_LIST, null, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT)
        );
        PromotionList promotionList = graphQLResponse.get("$.data.promotions", PromotionList.class);
        assertThat(promotionList.getItems().size()).isEqualTo(allPromotions.size() - 1);
        assertThat(promotionList.getItems().stream().map(c -> c.getId()).collect(Collectors.toList()))
                .doesNotContain(promotionToDelete.getId());
    }

    @Test
    @Order(11)
    public void updatePromotion_throws_for_deleted_promotion() throws IOException {
        UpdatePromotionInput input = new UpdatePromotionInput();
        input.setId(promotionToDelete.getId());
        input.setEnabled(false);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(
                    UPDATE_PROMOTION, variables, Arrays.asList(PROMOTION_FRAGMENT, CONFIGURABLE_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo(
                    "No PromotionEntity with the id '1' could be found"
            );
        }
    }
}
