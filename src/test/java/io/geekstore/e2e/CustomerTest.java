/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.email.EmailDetails;
import io.geekstore.email.EmailSender;
import io.geekstore.eventbus.events.AccountRegistrationEvent;
import io.geekstore.types.address.Address;
import io.geekstore.types.common.*;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static io.geekstore.config.TestConfig.ASYNC_TIMEOUT;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class CustomerTest {

    static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    static final String GET_CUSTOMER_LIST =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_list");
    static final String GET_CUSTOMER =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer");
    static final String CUSTOMER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "customer_fragment");
    static final String ADDRESS_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "address_fragment");
    public static final String GET_CUSTOMER_HISTORY =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_history");

    static final String ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/customer/%s.graphqls";
    static final String GET_CUSTOMER_WITH_USER =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_with_user");
    static final String CREATE_ADDRESS =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "create_address");
    static final String UPDATE_ADDRESS =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "update_address");
    static final String DELETE_CUSTOMER_ADDRESS =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "delete_customer_address");
    static final String CREATE_CUSTOMER =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "create_customer");
    static final String DELETE_CUSTOMER =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "delete_customer");
    static final String UPDATE_CUSTOMER =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "update_customer");
    static final String ADD_NOTE_TO_CUSTOMER =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "add_note_to_customer");
    static final String UPDATE_CUSTOMER_NOTE =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "update_customer_note");
    static final String DELETE_CUSTOMER_NOTE =
            String.format(ADMIN_CUSTOMER_GRAPHQL_RESOURCE_TEMPLATE, "delete_customer_note");

    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String ADD_ITEM_TO_ORDER  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "add_item_to_order");
    static final String GET_CUSTOMER_ORDERS  =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_customer_orders");

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

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(5).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    Customer firstCustomer;
    Customer secondCustomer;
    Customer thirdCustomer;

    /**
     * Customer resolver
     */

    @Test
    @Order(1)
    public void customer_list() throws IOException {
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, null);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        assertThat(customerList.getTotalItems()).isEqualTo(5);
        assertThat(customerList.getItems()).hasSize(5);
        firstCustomer = customerList.getItems().get(0);
        secondCustomer = customerList.getItems().get(1);
        thirdCustomer = customerList.getItems().get(2);
    }

    @Test
    @Order(2)
    public void customer_resolver_resolves_user() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER_WITH_USER, variables);
        assertThat(graphQLResponse.isOk());
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getUser()).isNotNull();
        assertThat(customer.getUser().getId()).isEqualTo(2);
        assertThat(customer.getUser().getIdentifier()).isEqualTo(firstCustomer.getEmailAddress());
        assertThat(customer.getUser().getVerified()).isTrue();
    }

    /**
     * address
     */

    List<Long> firstCustomerAddressIds;
    Long firstCustomerThirdAddressId;

    @Test
    @Order(3)
    public void createCustomerAddress_creates_a_new_address() throws IOException {
        CreateAddressInput createAddressInput = new CreateAddressInput();
        createAddressInput.setFullName("fullName");
        createAddressInput.setCompany("company");
        createAddressInput.setStreetLine1("streetLine1");
        createAddressInput.setStreetLine2("streetLine2");
        createAddressInput.setCity("city");
        createAddressInput.setProvince("province");
        createAddressInput.setPostalCode("postalCode");
        createAddressInput.setPhoneNumber("18008887788");
        createAddressInput.setDefaultShippingAddress(false);
        createAddressInput.setDefaultBillingAddress(false);

        JsonNode inputNode = objectMapper.valueToTree(createAddressInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_ADDRESS, variables);
        assertThat(graphQLResponse.isOk());

        Address address = graphQLResponse.get("$.data.adminCreateCustomerAddress", Address.class);
        assertThat(address.getFullName()).isEqualTo(createAddressInput.getFullName());
        assertThat(address.getCompany()).isEqualTo(createAddressInput.getCompany());
        assertThat(address.getStreetLine1()).isEqualTo(createAddressInput.getStreetLine1());
        assertThat(address.getStreetLine2()).isEqualTo(createAddressInput.getStreetLine2());
        assertThat(address.getCity()).isEqualTo(createAddressInput.getCity());
        assertThat(address.getProvince()).isEqualTo(createAddressInput.getProvince());
        assertThat(address.getPostalCode()).isEqualTo(createAddressInput.getPostalCode());
        assertThat(address.getPhoneNumber()).isEqualTo(createAddressInput.getPhoneNumber());
        assertThat(address.getDefaultBillingAddress()).isEqualTo(createAddressInput.getDefaultBillingAddress());
        assertThat(address.getDefaultShippingAddress()).isEqualTo(createAddressInput.getDefaultShippingAddress());
    }

    @Test
    @Order(4)
    public void customer_query_returns_addresses() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getAddresses()).hasSize(2);
        firstCustomerAddressIds =
                customer.getAddresses().stream().map(address -> address.getId()).collect(toList());
    }

    @Test
    @Order(5)
    public void updateCustomerAddress_updates_the_city() throws IOException {
        UpdateAddressInput input = new UpdateAddressInput();
        input.setId(firstCustomerAddressIds.get(0));
        input.setCity("updated_city");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(UPDATE_ADDRESS, variables);
        Address address = graphQLResponse.get("$.data.adminUpdateCustomerAddress", Address.class);
        assertThat(address.getCity()).isEqualTo(input.getCity());
    }

    @Test
    @Order(6)
    public void updateCustomerAddress_allows_only_a_single_default_address() throws IOException {
        // set the first customer's second address to be default
        UpdateAddressInput input = new UpdateAddressInput();
        input.setId(firstCustomerAddressIds.get(1));
        input.setDefaultShippingAddress(true);
        input.setDefaultBillingAddress(true);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(UPDATE_ADDRESS, variables);
        Address address = graphQLResponse.get("$.data.adminUpdateCustomerAddress", Address.class);
        assertThat(address.getDefaultShippingAddress()).isTrue();
        assertThat(address.getDefaultBillingAddress()).isTrue();

        // asset the first customer's other address is not default
        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        Address otherAddress =
                customer.getAddresses().stream().filter(a -> !a.getId().equals(firstCustomerAddressIds.get(1)))
                .collect(toList()).get(0);
        assertThat(otherAddress.getDefaultBillingAddress()).isFalse();
        assertThat(otherAddress.getDefaultShippingAddress()).isFalse();

        // set the first customer's first address to be default
        input = new UpdateAddressInput();
        input.setId(firstCustomerAddressIds.get(0));
        input.setDefaultShippingAddress(true);
        input.setDefaultBillingAddress(true);

        inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = this.adminClient.perform(UPDATE_ADDRESS, variables);
        address = graphQLResponse.get("$.data.adminUpdateCustomerAddress", Address.class);
        assertThat(address.getDefaultShippingAddress()).isTrue();
        assertThat(address.getDefaultBillingAddress()).isTrue();

        // assert the first customer's second address is not default
        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        customer = graphQLResponse.get("$.data.customer", Customer.class);
        otherAddress =
                customer.getAddresses().stream().filter(a -> !a.getId().equals(firstCustomerAddressIds.get(0)))
                        .collect(toList()).get(0);
        assertThat(otherAddress.getDefaultBillingAddress()).isFalse();
        assertThat(otherAddress.getDefaultShippingAddress()).isFalse();

        // get the second customer's address id
        variables = objectMapper.createObjectNode();
        variables.put("id", secondCustomer.getId());

        graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        customer = graphQLResponse.get("$.data.customer", Customer.class);
        Long secondCustomerAddressId = customer.getAddresses().get(0).getId();

        // set the second customer's address to be default
        input = new UpdateAddressInput();
        input.setId(secondCustomerAddressId);
        input.setDefaultShippingAddress(true);
        input.setDefaultBillingAddress(true);

        inputNode = objectMapper.valueToTree(input);
        variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        graphQLResponse = this.adminClient.perform(UPDATE_ADDRESS, variables);
        address = graphQLResponse.get("$.data.adminUpdateCustomerAddress", Address.class);
        assertThat(address.getDefaultShippingAddress()).isTrue();
        assertThat(address.getDefaultBillingAddress()).isTrue();

        // assets the first customer's address defaults are unchanged
        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        customer = graphQLResponse.get("$.data.customer", Customer.class);

        assertThat(customer.getAddresses().get(0).getDefaultShippingAddress()).isTrue();
        assertThat(customer.getAddresses().get(0).getDefaultBillingAddress()).isTrue();
        assertThat(customer.getAddresses().get(1).getDefaultShippingAddress()).isFalse();
        assertThat(customer.getAddresses().get(1).getDefaultBillingAddress()).isFalse();
    }

    @Test
    @Order(6)
    public void createCustomerAddress_with_true_defaults_unsets_existing_defaults() throws IOException {
        CreateAddressInput input = new CreateAddressInput();
        input.setStreetLine1("new default streetline");
        input.setCity("new_city");
        input.setDefaultShippingAddress(true);
        input.setDefaultBillingAddress(true);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_ADDRESS, variables);
        assertThat(graphQLResponse.isOk());
        Address createCustomerAddress = graphQLResponse.get("$.data.adminCreateCustomerAddress", Address.class);
        assertThat(createCustomerAddress.getFullName()).isEmpty();
        assertThat(createCustomerAddress.getCompany()).isEmpty();
        assertThat(createCustomerAddress.getStreetLine1()).isEqualTo(input.getStreetLine1());
        assertThat(createCustomerAddress.getStreetLine2()).isEmpty();
        assertThat(createCustomerAddress.getCity()).isEqualTo(input.getCity());
        assertThat(createCustomerAddress.getProvince()).isEmpty();
        assertThat(createCustomerAddress.getPostalCode()).isEmpty();
        assertThat(createCustomerAddress.getPhoneNumber()).isEmpty();
        assertThat(createCustomerAddress.getDefaultShippingAddress()).isTrue();
        assertThat(createCustomerAddress.getDefaultBillingAddress()).isTrue();

        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());
        graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        for(Address address : customer.getAddresses()) {
            boolean shouldBeDefault = createCustomerAddress.getId().equals(address.getId());
            assertThat(address.getDefaultShippingAddress()).isEqualTo(shouldBeDefault);
            assertThat(address.getDefaultBillingAddress()).isEqualTo(shouldBeDefault);
        }

        firstCustomerThirdAddressId = createCustomerAddress.getId();
    }

    @Test
    @Order(7)
    public void deleteCustomerAddress_on_default_address_resets_defaults() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomerThirdAddressId);

        GraphQLResponse graphQLResponse = this.adminClient.perform(DELETE_CUSTOMER_ADDRESS, variables);
        assertThat(graphQLResponse.isOk());
        Boolean deleteCustomerAddress = graphQLResponse.get("$.data.adminDeleteCustomerAddress", Boolean.class);
        assertThat(deleteCustomerAddress).isTrue();

        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());
        graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getAddresses()).hasSize(2);

        List<Address> defaultAddresses = customer.getAddresses()
                .stream().filter(a -> a.getDefaultBillingAddress() && a.getDefaultShippingAddress())
                .collect(toList());
        List<Address> otherAddresses = customer.getAddresses()
                .stream().filter(a -> !a.getDefaultBillingAddress() && !a.getDefaultShippingAddress())
                .collect(toList());
        assertThat(defaultAddresses).hasSize(1);
        assertThat(otherAddresses).hasSize(1);
    }

    @Test
    @Order(8)
    public void list_that_user_s_orders() throws IOException {
        // log in as first customer
        shopClient.asUserWithCredentials(firstCustomer.getEmailAddress(), MockDataService.TEST_PASSWORD);
        // add an item to the order to create an order
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("productVariantId", 1L);
        variables.put("quantity", 1);
        GraphQLResponse graphQLResponse = shopClient.perform(ADD_ITEM_TO_ORDER, variables);
        io.geekstore.types.order.Order order =
                graphQLResponse.get("$.data.addItemToOrder", io.geekstore.types.order.Order.class);

        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());
        graphQLResponse = adminClient.perform(GET_CUSTOMER_ORDERS, variables);
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);

        assertThat(customer.getOrders().getTotalItems()).isEqualTo(1);
        assertThat(customer.getOrders().getItems().get(0).getId()).isEqualTo(order.getId());
    }


    /**
     * creation test suite
     */

    @Test
    @Order(9)
    public void triggers_verification_event_if_no_password_supplied() throws Exception {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test1@test.com");
        input.setFirstName("New");
        input.setLastName("Customer");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(
                CREATE_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.createCustomer", Customer.class);
        assertThat(customer.getUser().getVerified()).isFalse();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        assertThat(emailDetails.getEvent()).isInstanceOf(AccountRegistrationEvent.class);
        assertThat(emailDetails.getRecipient()).isEqualTo(input.getEmailAddress());
    }

    @Test
    @Order(10)
    public void creates_a_verified_Customer() throws Exception {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test2@test.com");
        input.setFirstName("New");
        input.setLastName("Customer");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);
        variables.put("password", MockDataService.TEST_PASSWORD);

        GraphQLResponse graphQLResponse = this.adminClient.perform(
                CREATE_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.createCustomer", Customer.class);
        assertThat(customer.getUser().getVerified()).isTrue();
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(0)).send(any());
    }

    @Test
    @Order(11)
    public void throws_when_using_an_existing_non_deleted_emailAddress() throws IOException {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress("test2@test.com");
        input.setFirstName("New");
        input.setLastName("Customer");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);
        variables.put("password", MockDataService.TEST_PASSWORD);

        try {
            this.adminClient.perform(
                    CREATE_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("The email address must be unique");
        }
    }

    /**
     * deletion test suite
     */

    @Test
    @Order(12)
    public void deletes_a_customer() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", thirdCustomer.getId());

        GraphQLResponse graphQLResponse = this.adminClient.perform(
                DELETE_CUSTOMER, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteCustomer", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);
    }

    @Test
    @Order(13)
    public void cannot_get_a_deleted_customer() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", thirdCustomer.getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer).isNull();
    }

    @Test
    @Order(14)
    public void deleted_customer_omitted_from_list() throws IOException {
        GraphQLResponse graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, null);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        customerList.getItems().stream().noneMatch(c -> c.getId().equals(thirdCustomer.getId()));
    }

    @Test
    @Order(15)
    public void updateCustomer_throws_for_deleted_customer() throws IOException {
        UpdateCustomerInput input = new UpdateCustomerInput();
        input.setId(thirdCustomer.getId());
        input.setFirstName("updated");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(UPDATE_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("No CustomerEntity with the id '3' could be found");
        }
    }

    @Test
    @Order(16)
    public void createCustomerAddress_throws_for_deleted_customer() throws IOException {
        CreateAddressInput createAddressInput = new CreateAddressInput();
        createAddressInput.setStreetLine1("test_street_line");
        createAddressInput.setCity("test_city");

        JsonNode inputNode = objectMapper.valueToTree(createAddressInput);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", thirdCustomer.getId());
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(CREATE_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("No CustomerEntity with the id '3' could be found");
        }
    }

    @Test
    @Order(17)
    public void new_Customer_cannot_be_created_with_same_emailAddress_as_a_deleted_Customer() throws IOException {
        CreateCustomerInput input = new CreateCustomerInput();
        input.setEmailAddress(thirdCustomer.getEmailAddress());
        input.setFirstName("Reusing Email");
        input.setLastName("Customer");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(
                    CREATE_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            // Unique index or primary key violation on tb_user table
            assertThat(apiEx.getMessage())
                    .isEqualTo("Unexpected internal server error, please report to our engineering team!");
        }
    }

    /**
     * customer notes
     */

    Long noteId;

    @Test
    @Order(18)
    public void addNoteToCustomer() throws IOException {
        AddNoteToCustomerInput input = new AddNoteToCustomerInput();
        input.setId(firstCustomer.getId());
        input.setPrivateOnly(false);
        input.setNote("Test note");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(
                ADD_NOTE_TO_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        HistoryEntryListOptions options = new HistoryEntryListOptions();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setEq(HistoryEntryType.CUSTOMER_NOTE.name());
        HistoryEntryFilterParameter filter = new HistoryEntryFilterParameter();
        filter.setType(stringOperators);
        options.setFilter(filter);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());
        variables.set("options", optionsNode);

        graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(1);
        assertThat(customer.getHistory().getTotalItems()).isEqualTo(1);

        assertThat(customer.getHistory().getItems().get(0).getType()).isEqualTo(HistoryEntryType.CUSTOMER_NOTE);
        assertThat(customer.getHistory().getItems().get(0).getData().get("note")).isEqualTo(input.getNote());

        noteId = customer.getHistory().getItems().get(0).getId();
    }

    @Test
    @Order(19)
    public void update_note() throws IOException {
        UpdateCustomerNoteInput input = new UpdateCustomerNoteInput();
        input.setNoteId(noteId);
        input.setNote("An updated note");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = this.adminClient.perform(
                UPDATE_CUSTOMER_NOTE, variables);
        assertThat(graphQLResponse.isOk());

        HistoryEntry historyEntry = graphQLResponse.get("$.data.updateCustomerNode", HistoryEntry.class);
        assertThat(historyEntry.getData()).contains(entry("note", input.getNote()));
        assertThat(historyEntry.getPrivateOnly()).isFalse();
    }

    @Test
    @Order(20)
    public void delete_note() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer beforeCustomer = graphQLResponse.get("$.data.customer", Customer.class);
        Integer historyCount = beforeCustomer.getHistory().getTotalItems();

        variables = objectMapper.createObjectNode();
        variables = objectMapper.createObjectNode();
        variables.put("id", noteId);

        graphQLResponse = adminClient.perform(DELETE_CUSTOMER_NOTE, variables);
        assertThat(graphQLResponse.isOk());
        DeletionResponse deletionResponse =
                graphQLResponse.get("$.data.deleteCustomerNote", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        variables = objectMapper.createObjectNode();
        variables = objectMapper.createObjectNode();
        variables.put("id", firstCustomer.getId());

        graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer afterCustomer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(afterCustomer.getHistory().getTotalItems()).isEqualTo(historyCount - 1);
    }
}
