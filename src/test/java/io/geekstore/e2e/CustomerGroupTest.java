/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.common.StringOperators;
import io.geekstore.types.customer.*;
import io.geekstore.types.history.HistoryEntry;
import io.geekstore.types.history.HistoryEntryFilterParameter;
import io.geekstore.types.history.HistoryEntryListOptions;
import io.geekstore.types.history.HistoryEntryType;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class CustomerGroupTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String GET_CUSTOMER_HISTORY =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_history");

    static final String CREATE_CUSTOMER_GROUP =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_customer_group");
    static final String CUSTOMER_GROUP_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "customer_group_fragment");

    static final String CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/customer_group/%s.graphqls";
    static final String GET_CUSTOMER_GROUPS =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_groups");
    static final String GET_CUSTOMER_GROUP =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_group");
    static final String UPDATE_CUSTOMER_GROUP =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "update_customer_group");
    static final String ADD_CUSTOMER_TO_GROUP =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "add_customer_to_group");
    static final String GET_CUSTOMER_WITH_GROUPS =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_with_groups");
    static final String REMOVE_CUSTOMERS_FROM_GROUP =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "remove_customers_from_group");
    static final String DELETE_CUSTOMER_GROUP =
            String.format(CUSTOMER_GROUP_GRAPHQL_RESOURCE_TEMPLATE, "delete_customer_group");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    List<Customer> customers;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(5).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();

        customers = getCustomerList().getItems();
    }

    @Test
    @Order(1)
    public void create() throws IOException {
        CreateCustomerGroupInput input = new CreateCustomerGroupInput();
        input.setName("group 1");
        input.getCustomerIds().add(customers.get(0).getId());
        input.getCustomerIds().add(customers.get(1).getId());

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(CREATE_CUSTOMER_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        CustomerGroup customerGroup = graphQLResponse.get("$.data.createCustomerGroup", CustomerGroup.class);
        assertThat(customerGroup.getName()).isEqualTo(input.getName());
        assertThat(customerGroup.getCustomers().getItems().stream().map(Customer::getId).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(customers.get(0).getId(), customers.get(1).getId());
    }

    @Test
    @Order(2)
    public void history_entry_for_CUSTOMER_ADDED_TO_GROUP_after_group_created() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(3);
        options.setCurrentPage(2);
        JsonNode optionsNote = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(0).getId());
        variables.set("options", optionsNote);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_ADDED_TO_GROUP);
        assertThat(historyEntry.getData()).containsEntry("groupName", "group 1");
    }

    @Test
    @Order(3)
    public void customerGroups() throws IOException {
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_GROUPS, null);
        CustomerGroupList customerGroupList =
                graphQLResponse.get("$.data.customerGroups", CustomerGroupList.class);

        assertThat(customerGroupList.getTotalItems()).isEqualTo(1);
        assertThat(customerGroupList.getItems().get(0).getName()).isEqualTo("group 1");
    }

    private CustomerList getCustomerList() throws IOException {
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, null);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        return customerList;
    }

    @Test
    @Order(4)
    public void customerGroup_with_customer_list_options() throws IOException {
        CustomerGroupListOptions options = new CustomerGroupListOptions();
        options.setPageSize(1);
        options.setCurrentPage(1);

        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_GROUP, variables);
        CustomerGroup customerGroup = graphQLResponse.get("$.data.customerGroup", CustomerGroup.class);
        assertThat(customerGroup.getId()).isEqualTo(1L);
        assertThat(customerGroup.getName()).isEqualTo("group 1");
        assertThat(customerGroup.getCustomers().getItems()).hasSize(1);
        assertThat(customerGroup.getCustomers().getTotalItems()).isEqualTo(2);
    }

    @Test
    @Order(5)
    public void update() throws IOException {
        UpdateCustomerGroupInput input = new UpdateCustomerGroupInput();
        input.setId(1L);
        input.setName("group 1 updated");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(UPDATE_CUSTOMER_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        CustomerGroup customerGroup = graphQLResponse.get("$.data.updateCustomerGroup", CustomerGroup.class);
        assertThat(customerGroup.getName()).isEqualTo(input.getName());
    }

    @Test
    @Order(6)
    public void addCustomersToGroup_with_existing_customer() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("groupId", 1L);
        variables.putArray("customerIds").add(customers.get(0).getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADD_CUSTOMER_TO_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        CustomerGroup customerGroup = graphQLResponse.get("$.data.addCustomersToGroup", CustomerGroup.class);
        assertThat(customerGroup.getCustomers().getItems().stream().map(Customer::getId).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(customers.get(0).getId(), customers.get(1).getId());
    }

    @Test
    @Order(7)
    public void addCustomersToGroup_with_new_customers() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("groupId", 1L);
        variables.putArray("customerIds").add(customers.get(2).getId())
                      .add(customers.get(3).getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADD_CUSTOMER_TO_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        CustomerGroup customerGroup = graphQLResponse.get("$.data.addCustomersToGroup", CustomerGroup.class);
        assertThat(customerGroup.getCustomers().getItems().stream().map(Customer::getId).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        customers.get(0).getId(),
                        customers.get(1).getId(),
                        customers.get(2).getId(),
                        customers.get(3).getId());
    }

    @Test
    @Order(8)
    public void history_entry_for_CUSTOMER_ADDED_TO_GROUP_not_duplicated() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setEq(HistoryEntryType.CUSTOMER_ADDED_TO_GROUP.name());
        HistoryEntryFilterParameter filter = new HistoryEntryFilterParameter();
        filter.setType(stringOperators);
        options.setFilter(filter);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(0).getId());
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        assertThat(customer.getHistory().getTotalItems()).isEqualTo(1);

        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_ADDED_TO_GROUP);
        assertThat(historyEntry.getData().get("groupName")).isEqualTo("group 1");
    }

    @Test
    @Order(9)
    public void history_entry_for_CUSTOMER_ADDED_TO_GROUP_after_customer_added() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(3); // skip 3
        options.setCurrentPage(2);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(2).getId());
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        assertThat(customer.getHistory().getTotalItems()).isEqualTo(4);

        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_ADDED_TO_GROUP);
        assertThat(historyEntry.getData().get("groupName")).isEqualTo("group 1 updated");
    }

    @Test
    @Order(10)
    public void customer_dot_groups_field_resolver() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(0).getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_WITH_GROUPS, variables);
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getGroups()).hasSize(1);
        assertThat(customer.getGroups().get(0).getId()).isEqualTo(1L);
        assertThat(customer.getGroups().get(0).getName()).isEqualTo("group 1 updated");
    }

    @Test
    @Order(11)
    public void removeCustomersFromGroup_with_invalid_customerId() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("groupId", 1L);
        variables.putArray("customerIds").add(customers.get(4).getId());

        try {
            this.adminClient.perform(REMOVE_CUSTOMERS_FROM_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Customer '5' does not belong to this CustomerGroup '1'");
        }
    }

    @Test
    @Order(12)
    public void removeCustomersFromGroup_with_valid_customerIds() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("groupId", 1L);
        variables.putArray("customerIds").add(customers.get(1).getId())
                .add(customers.get(3).getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(REMOVE_CUSTOMERS_FROM_GROUP, variables, Arrays.asList(CUSTOMER_GROUP_FRAGMENT));
        CustomerGroup customerGroup = graphQLResponse.get("$.data.removeCustomersFromGroup", CustomerGroup.class);
        assertThat(customerGroup.getCustomers().getItems().stream().map(Customer::getId).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(customers.get(0).getId(), customers.get(2).getId());
    }

    @Test
    @Order(13)
    public void history_entry_for_CUSTOMER_REMOVED_FROM_GROUP() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setPageSize(4); // skip 4
        options.setCurrentPage(2);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customers.get(1).getId());
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        assertThat(customer.getHistory().getTotalItems()).isEqualTo(5);

        HistoryEntry historyEntry = customer.getHistory().getItems().get(0);
        assertThat(historyEntry.getType()).isEqualTo(HistoryEntryType.CUSTOMER_REMOVED_FROM_GROUP);
        assertThat(historyEntry.getData().get("groupName")).isEqualTo("group 1 updated");
    }

    @Test
    @Order(14)
    public void deleteCustomerGroup() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", 1L);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(DELETE_CUSTOMER_GROUP, variables);
        DeletionResponse deletionResponse =
                graphQLResponse.get("$.data.deleteCustomerGroup", DeletionResponse.class);
        assertThat(deletionResponse.getMessage()).isNull();
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        graphQLResponse = this.adminClient.perform(GET_CUSTOMER_GROUPS, null);
        CustomerGroupList customerGroupList =
                graphQLResponse.get("$.data.customerGroups", CustomerGroupList.class);

        assertThat(customerGroupList.getTotalItems()).isEqualTo(0);
    }

}
