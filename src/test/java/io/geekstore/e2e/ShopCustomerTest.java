/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.email.EmailSender;
import io.geekstore.options.ConfigOptions;
import io.geekstore.types.address.Address;
import io.geekstore.types.auth.LoginResult;
import io.geekstore.types.common.CreateAddressInput;
import io.geekstore.types.common.UpdateAddressInput;
import io.geekstore.types.customer.Customer;
import io.geekstore.types.customer.CustomerList;
import io.geekstore.types.customer.UpdateCustomerInput;
import io.geekstore.types.history.*;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class ShopCustomerTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String GET_CUSTOMER =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer");
    static final String CUSTOMER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "customer_fragment");
    static final String ADDRESS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "address_fragment");
    static final String CURRENT_USER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "current_user_fragment");
    static final String GET_CUSTOMER_HISTORY =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_history");

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String UPDATE_CUSTOMER =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "update_customer");
    static final String CREATE_ADDRESS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "create_address");
    static final String UPDATE_ADDRESS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "update_address");
    static final String DELETE_ADDRESS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "delete_address");
    static final String ATTEMPT_LOGIN =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "attempt_login");
    static final String UPDATE_PASSWORD =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "update_password");

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

    @MockBean
    EmailSender emailSender;

    @Autowired
    ConfigOptions configOptions;

    Customer customer;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(2).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-full.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        CustomerList customerList = getCustomerList();
        customer = getCustomerById(customerList.getItems().get(0).getId());
    }

    private Customer getCustomerById(Long customerId) throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customerId);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        return graphQLResponse.get("$.data.customer", Customer.class);
    }

    private CustomerList getCustomerList() throws IOException {
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, null);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        return customerList;
    }

    @Test
    @Order(1)
    public void updateCustomer_throws_if_not_logged_in() throws IOException {
        UpdateCustomerInput input = new UpdateCustomerInput();
        input.setId(customer.getId());
        input.setFirstName("xyz");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.shopClient.perform(UPDATE_CUSTOMER, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    @Test
    @Order(2)
    public void createCustomerAddress_throws_if_not_logged_in() throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setStreetLine1("1 Test Street");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.shopClient.perform(CREATE_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    @Test
    @Order(3)
    public void updateCustomerAddress_throws_if_not_logged_in() throws IOException {
        UpdateAddressInput input = new UpdateAddressInput();
        input.setId(1L);
        input.setStreetLine1("zxc");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.shopClient.perform(UPDATE_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    @Test
    @Order(4)
    public void deleteCustomerAddress_throws_if_not_logged_in() throws IOException {
        try {
            ObjectNode variables = objectMapper.createObjectNode();
            variables.put("id", 1L);

            this.shopClient.perform(DELETE_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    /**
     * Logged in Customer
     */

    Long addressId;

    private void beforeTest5() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("username", customer.getEmailAddress());
        variables.put("password", MockDataService.TEST_PASSWORD);
        variables.put("rememberMe", false);

        shopClient.perform(ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));
    }

    @Test
    @Order(5)
    public void updateCustomer_works() throws IOException {
        beforeTest5();

        UpdateCustomerInput input = new UpdateCustomerInput();
        input.setId(customer.getId());
        input.setFirstName("xyz");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.shopClient.perform(UPDATE_CUSTOMER, variables);
        Customer customer = graphQLResponse.get("$.data.updateCustomer", Customer.class);
        assertThat(customer.getFirstName()).isEqualTo("xyz");
    }

    @Test
    @Order(6)
    public void customer_history_for_CUSTOMER_DETAILS_UPDATED() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(3); // skip populated CUSTOMER_ADDRESS_CREATED entry
        options.setCurrentPage(2);
        JsonNode optionsNote = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNote);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_DETAIL_UPDATED);
        assertThat(historyEntry.getData()).containsEntry("firstName", "xyz");
        assertThat(historyEntry.getData()).containsEntry("id", "1");
    }

    @Test
    @Order(7)
    public void createCustomerAddress_works() throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setStreetLine1("1 Test Street");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.shopClient.perform(CREATE_ADDRESS, variables);
        Address createdCustomerAddress = graphQLResponse.get("$.data.createCustomerAddress", Address.class);
        assertThat(createdCustomerAddress.getId()).isEqualTo(3);
        assertThat(createdCustomerAddress.getStreetLine1()).isEqualTo(input.getStreetLine1());

        addressId = createdCustomerAddress.getId();
    }

    @Test
    @Order(8)
    public void customer_history_for_CUSTOMER_ADDRESS_CREATED() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(4); // skip populated CUSTOMER_ADDRESS_CREATED, CUSTOMER_DETAIL_UPDATED entries
        options.setCurrentPage(2);
        JsonNode optionsNote = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNote);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_ADDRESS_CREATED);
        assertThat(historyEntry.getData()).containsEntry("address", "1 Test Street");
    }

    @Test
    @Order(9)
    public void updateCustomerAddress_works() throws IOException {
        UpdateAddressInput input = new UpdateAddressInput();
        input.setId(addressId);
        input.setStreetLine1("5 Test Street");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.shopClient.perform(UPDATE_ADDRESS, variables);
        Address updatedAddress = graphQLResponse.get("$.data.updateCustomerAddress", Address.class);
        assertThat(updatedAddress.getStreetLine1()).isEqualTo(input.getStreetLine1());
    }

    @Test
    @Order(10)
    public void customer_history_for_CUSTOMER_ADDRESS_UPDATED() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(5); // skip populated CUSTOMER_ADDRESS_CREATED, CUSTOMER_DETAIL_UPDATED entries
        options.setCurrentPage(2);
        JsonNode optionsNote = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNote);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_ADDRESS_UPDATED);
        assertThat(historyEntry.getData()).containsEntry("address", "5 Test Street");
        assertThat(historyEntry.getData()).containsEntry("id", String.valueOf(addressId));
        assertThat(historyEntry.getData()).containsEntry("streetLine1", "5 Test Street");
    }

    @Test
    @Order(11)
    public void updateCustomerAddress_fails_for_address_not_owned_by_Customer() throws IOException {
        UpdateAddressInput input = new UpdateAddressInput();
        input.setId(2L);
        input.setStreetLine1("1 Test Street");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.shopClient.perform(UPDATE_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    @Test
    @Order(12)
    public void deleteCustomerAddress_works() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 3L);

        GraphQLResponse graphQLResponse = this.shopClient.perform(DELETE_ADDRESS, variables);
        Boolean result = graphQLResponse.get("$.data.deleteCustomerAddress", Boolean.class);
        assertThat(result).isTrue();
    }

    @Test
    @Order(13)
    public void customer_history_for_CUSTOMER_ADDRESS_DELETED() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(6);
        options.setCurrentPage(2);
        JsonNode optionsNote = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNote);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_ADDRESS_DELETED);
        assertThat(historyEntry.getData()).containsEntry("address", "5 Test Street");
    }

    @Test
    @Order(14)
    public void deleteCustomerAddress_fails_for_address_not_owned_by_Customer() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 2L);

        try {
            this.shopClient.perform(DELETE_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }

    @Test
    @Order(15)
    public void updatePassword_fails_with_incorrect_current_password() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("old", "wrong");
        variables.put("new", "new123456");

        try {
            this.shopClient.perform(UPDATE_PASSWORD, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("The credentials did not match. Please check and try again");
        }
    }

    @Test
    @Order(16)
    public void updatePassword_works() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("old", MockDataService.TEST_PASSWORD);
        variables.put("new", "new123456");

        GraphQLResponse graphQLResponse = this.shopClient.perform(UPDATE_PASSWORD, variables);
        Boolean updatedResult = graphQLResponse.get("$.data.updateCustomerPassword", Boolean.class);
        assertThat(updatedResult).isTrue();

        // Log out and log in with new password
        graphQLResponse = shopClient.asUserWithCredentials(customer.getEmailAddress(), "new123456");
        LoginResult loginResult = graphQLResponse.get("$.data.login", LoginResult.class);
        assertThat(loginResult.getUser().getIdentifier()).isEqualTo(customer.getEmailAddress());
    }

    @Test
    @Order(17)
    public void customer_history_for_CUSTOMER_PASSWORD_UPDATED() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(7);
        options.setCurrentPage(2);
        JsonNode optionsNote = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNote);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_PASSWORD_UPDATED);
        assertThat(historyEntry.getData()).isEmpty();
    }
}
