/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.common.Constant;
import io.geekstore.config.TestConfig;
import io.geekstore.types.administrator.CreateAdministratorInput;
import io.geekstore.types.auth.CurrentUser;
import io.geekstore.types.common.CreateCustomerInput;
import io.geekstore.types.common.Permission;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.product.CreateProductInput;
import io.geekstore.types.product.UpdateProductInput;
import io.geekstore.types.role.CreateRoleInput;
import io.geekstore.types.role.Role;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class AuthTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String ADMIN_ME =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "admin_me");
    static final String CURRENT_USER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "current_user_fragment");
    static final String ATTEMPT_LOGIN =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "admin_attempt_login");
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String CREATE_ROLE =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_role");
    static final String ROLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "role_fragment");
    static final String CREATE_ADMINISTRATOR =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_administrator");
    static final String ADMINISTRATOR_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "administrator_fragment");
    static final String GET_PRODUCT_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_product_list");
    static final String UPDATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "update_product");

    static final String ASSET_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "asset_fragment");
    static final String PRODUCT_VARIANT_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_variant_fragment");
    static final String PRODUCT_WITH_VARIANTS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "product_with_variants_fragment");
    static final String CREATE_PRODUCT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_product");

    static final String ADMIN_AUTH_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/auth/%s.graphqls";
    static final String CREATE_CUSTOMER =
            String.format(ADMIN_AUTH_GRAPHQL_RESOURCE_TEMPLATE, "create_customer");
    static final String GET_CUSTOMER_COUNT =
            String.format(ADMIN_AUTH_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_count");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
    }

    /**
     * admin permissions test suite
     */
    @Test
    @Order(1)
    public void me_is_not_permitted_for_anonymous_user() throws IOException {
        // Anonymous user
        this.adminClient.asAnonymousUser();

        try {
            this.adminClient.perform(ADMIN_ME, null, Arrays.asList(CURRENT_USER_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    @Test
    @Order(2)
    public void anonymous_user_can_attempt_login() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("username", Constant.SUPER_ADMIN_USER_IDENTIFIER);
        variables.put("password", Constant.SUPER_ADMIN_USER_PASSWORD);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));
        assertThat(graphQLResponse.isOk());
    }

    /**
     * Customer user test
     */
    @Test
    @Order(3)
    public void customer_user_cannot_login() throws IOException {
        this.adminClient.asSuperAdmin();
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, null);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        assertThat(customerList.getItems()).hasSize(1);
        String customerEmailAddress = customerList.getItems().get(0).getEmailAddress();

        try {
            this.adminClient.asUserWithCredentials(customerEmailAddress, MockDataService.TEST_PASSWORD);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("The credentials did not match. Please check and try again");
        }
    }

    /**
     * ReadCatalog permission test suite
     */

    @Test
    @Order(4)
    public void me_returns_correct_permissions_after_adding_read_catalog_permission() throws IOException {
        this.adminClient.asSuperAdmin();
        Pair<String, String> pair =
                this.createAdministratorWithPermissions("ReadCatalog", Arrays.asList(Permission.ReadCatalog));
        this.adminClient.asUserWithCredentials(pair.getLeft(), pair.getRight());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADMIN_ME, null, Arrays.asList(CURRENT_USER_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        CurrentUser currentUser = graphQLResponse.get("$.data.adminMe", CurrentUser.class);
        assertThat(currentUser.getPermissions()).containsExactlyInAnyOrder(
                Permission.Authenticated, Permission.ReadCatalog
        );
    }

    @Test
    @Order(5)
    public void can_read() throws IOException {
        adminClient.perform(GET_PRODUCT_LIST, null);
    }

    @Test
    @Order(6)
    public void cannot_update() throws IOException {
        UpdateProductInput input = new UpdateProductInput();
        input.setId(1L);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(UPDATE_PRODUCT, variables,
                    Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("FORBIDDEN");
        }
    }

    @Test
    @Order(7)
    public void cannot_create() throws IOException {
        CreateProductInput input = new CreateProductInput();

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(CREATE_PRODUCT, variables,
                    Arrays.asList(PRODUCT_WITH_VARIANTS_FRAGMENT, PRODUCT_VARIANT_FRAGMENT, ASSET_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("FORBIDDEN");
        }
    }

    /**
     * CRUD on Customers permissions test suite
     */
    @Test
    @Order(9)
    public void me_returns_correct_permissions_after_adding_crud_on_customers_permission() throws IOException {
        this.adminClient.asSuperAdmin();
        Pair<String, String> pair = this.createAdministratorWithPermissions("CRUDCustomer",
                        Arrays.asList(
                                Permission.ReadCustomer,
                                Permission.CreateCustomer,
                                Permission.UpdateCustomer,
                                Permission.DeleteCustomer));
        this.adminClient.asUserWithCredentials(pair.getLeft(), pair.getRight());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADMIN_ME, null, Arrays.asList(CURRENT_USER_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        CurrentUser currentUser = graphQLResponse.get("$.data.adminMe", CurrentUser.class);
        assertThat(currentUser.getPermissions()).containsExactlyInAnyOrder(
                Permission.ReadCustomer,
                Permission.CreateCustomer,
                Permission.UpdateCustomer,
                Permission.DeleteCustomer,
                Permission.Authenticated
        );
    }

    @Test
    @Order(10)
    public void can_create_customer() throws IOException {
        CreateCustomerInput createCustomerInput = new CreateCustomerInput();
        createCustomerInput.setEmailAddress("");
        createCustomerInput.setFirstName("");
        createCustomerInput.setLastName("");

        JsonNode inputNode = objectMapper.valueToTree(createCustomerInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_CUSTOMER, variables);
        assertThat(graphQLResponse.isOk());
    }

    @Test
    @Order(11)
    public void can_read_customer() throws IOException {
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_COUNT, null);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        assertThat(customerList.getTotalItems()).isEqualTo(2);
    }

    private Pair<String, String> createAdministratorWithPermissions(String code, List<Permission> permissions)
            throws IOException {
        CreateRoleInput input = new CreateRoleInput();
        input.setCode(code);
        input.setDescription("");
        input.setPermissions(permissions);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Role role = graphQLResponse.get("$.data.createRole", Role.class);

        String identifier = code + "@" + RandomStringUtils.randomAlphabetic(7);
        String password = MockDataService.TEST_PASSWORD;

        CreateAdministratorInput createAdministratorInput = new CreateAdministratorInput();
        createAdministratorInput.setEmailAddress(identifier);
        createAdministratorInput.setFirstName(code);
        createAdministratorInput.setLastName("Admin");
        createAdministratorInput.setPassword(password);
        createAdministratorInput.getRoleIds().add(role.getId());

        inputNode = objectMapper.valueToTree(createAdministratorInput);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);
        graphQLResponse = this.adminClient.perform(CREATE_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        return Pair.of(identifier, password);
    }
}
