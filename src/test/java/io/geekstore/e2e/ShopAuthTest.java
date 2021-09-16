/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.email.EmailDetails;
import io.geekstore.email.EmailSender;
import io.geekstore.eventbus.events.IdentifierChangeEvent;
import io.geekstore.options.ConfigOptions;
import io.geekstore.types.auth.CurrentUser;
import io.geekstore.types.auth.LoginResult;
import io.geekstore.types.common.StringOperators;
import io.geekstore.types.customer.*;
import io.geekstore.types.history.HistoryEntry;
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

import static io.geekstore.config.TestConfig.ASYNC_TIMEOUT;
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
public class ShopAuthTest {
    static final String SHOP_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shop/%s.graphqls";
    static final String REGISTER_ACCOUNT =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "register_account");
    static final String REFRESH_TOKEN =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "refresh_token");
    static final String VERIFY_EMAIL =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "verify_email");
    static final String GET_ACTIVE_CUSTOMER =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_active_customer");
    static final String REQUEST_PASSWORD_RESET =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "request_password_reset");
    static final String RESET_PASSWORD =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "reset_password");
    static final String REQUEST_UPDATE_EMAIL_ADDRESS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "request_update_email_address");
    static final String UPDATE_EMAIL_ADDRESS =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "update_email_address");
    static final String GET_ME =
            String.format(SHOP_GRAPHQL_RESOURCE_TEMPLATE, "get_me");

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

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(2).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    /**
     * customer account creation with deferred password
     */
    String password = "password1234";
    String emailAddress = "test1@test.com";

    String verificationToken;
    Long newCustomerId;

    @Test
    @Order(1)
    public void register_a_new_account_without_password() throws Exception {
        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Sean");
        input.setLastName("Tester");
        input.setPhoneNumber("123456");
        input.setEmailAddress(emailAddress);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(REGISTER_ACCOUNT, variables);
        Boolean result = graphQLResponse.get("$.data.registerCustomerAccount", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        verificationToken = (String) emailDetails.getModel().get("verificationToken");
        assertThat(verificationToken).isNotNull();


        CustomerListOptions options = new CustomerListOptions();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setEq(emailAddress);
        CustomerFilterParameter filter = new CustomerFilterParameter();
        filter.setEmailAddress(stringOperators);
        options.setFilter(filter);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, variables);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        assertThat(customerList.getItems()).hasSize(1);
        assertThat(customerList.getTotalItems()).isEqualTo(1);
        Customer customer = customerList.getItems().get(0);
        assertThat(customer.getFirstName()).isEqualTo(input.getFirstName());
        assertThat(customer.getLastName()).isEqualTo(input.getLastName());
        assertThat(customer.getEmailAddress()).isEqualTo(input.getEmailAddress());
        assertThat(customer.getPhoneNumber()).isEqualTo(input.getPhoneNumber());
    }

    @Test
    @Order(2)
    public void issues_a_new_token_if_attempting_to_register_a_second_time() throws Exception {
        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Sean");
        input.setLastName("Tester");
        input.setEmailAddress(emailAddress);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(REGISTER_ACCOUNT, variables);
        Boolean result = graphQLResponse.get("$.data.registerCustomerAccount", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        String newVerificationToken = (String) emailDetails.getModel().get("verificationToken");
        assertThat(newVerificationToken).isNotNull();
        assertThat(newVerificationToken).isNotEqualTo(verificationToken);

        verificationToken = newVerificationToken;
    }

    @Test
    @Order(3)
    public void refreshCustomerVerification_issues_a_new_token() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("emailAddress", emailAddress);

        GraphQLResponse graphQLResponse = shopClient.perform(REFRESH_TOKEN, variables);
        Boolean result = graphQLResponse.get("$.data.refreshCustomerVerification", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        String newVerificationToken = (String) emailDetails.getModel().get("verificationToken");
        assertThat(newVerificationToken).isNotNull();
        assertThat(newVerificationToken).isNotEqualTo(verificationToken);

        verificationToken = newVerificationToken;
    }

    @Test
    @Order(4)
    public void refreshCustomerVerification_does_nothing_with_an_unrecognized_emailAddress() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("emailAddress", "never-been-register@test.com");

        GraphQLResponse graphQLResponse = shopClient.perform(REFRESH_TOKEN, variables);
        Boolean result = graphQLResponse.get("$.data.refreshCustomerVerification", Boolean.class);
        assertThat(result).isTrue();

        verify(emailSender, timeout(ASYNC_TIMEOUT).times(0)).send(any());
    }

    @Test
    @Order(5)
    public void login_fails_before_verification() throws IOException {
        try {
            shopClient.asUserWithCredentials(emailAddress, password);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Test
    @Order(6)
    public void verification_fails_with_wrong_token() throws IOException {
        try {
            ObjectNode variables = objectMapper.createObjectNode();
            variables.put("password", password);
            variables.put("token", "bad-token");

            shopClient.perform(VERIFY_EMAIL, variables);

            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Verification token not recognized");
        }
    }

    @Test
    @Order(7)
    public void verification_fails_with_no_password() throws IOException {
        try {
            ObjectNode variables = objectMapper.createObjectNode();
            variables.put("token", verificationToken);

            shopClient.perform(VERIFY_EMAIL, variables);

            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage())
                    .isEqualTo("A password must be provided as it was not set during registration");
        }
    }

    @Test
    @Order(8)
    public void verification_succeeds_with_password_and_correct_token() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", verificationToken);
        variables.put("password", password);

        GraphQLResponse graphQLResponse = shopClient.perform(VERIFY_EMAIL, variables);
        LoginResult loginResult = graphQLResponse.get("$.data.verifyCustomerAccount", LoginResult.class);
        assertThat(loginResult.getUser().getIdentifier()).isEqualTo(emailAddress);

        Customer customer = getActiveCustomer();
        newCustomerId = customer.getId();
    }

    private Customer getActiveCustomer() throws IOException {
        GraphQLResponse graphQLResponse = shopClient.perform(GET_ACTIVE_CUSTOMER, null);
        Customer customer = graphQLResponse.get("$.data.activeCustomer", Customer.class);
        assertThat(customer).isNotNull();
        return customer;
    }

    @Test
    @Order(9)
    public void registration_silently_fails_if_attempting_to_register_an_email_already_verified() throws Exception {
        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Dodgy");
        input.setLastName("Hacker");
        input.setEmailAddress(emailAddress);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(REGISTER_ACCOUNT, variables);
        Boolean result = graphQLResponse.get("$.data.registerCustomerAccount", Boolean.class);
        assertThat(result).isFalse();

        verify(emailSender, timeout(ASYNC_TIMEOUT).times(0)).send(any());
    }

    @Test
    @Order(10)
    public void verification_fails_if_attempted_a_second_time() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", verificationToken);
        variables.put("password", password);

        try {
            shopClient.perform(VERIFY_EMAIL, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Verification token not recognized");
        }
    }

    @Test
    @Order(11)
    public void customer_history_contains_entries_for_registration_and_verification() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", newCustomerId);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer customer = graphQLResponse.get("$.data.customer", Customer.class);
        assertThat(customer.getHistory().getItems()).hasSize(3);

        HistoryEntry historyEntry0 = customer.getHistory().getItems().get(0);
        assertThat(historyEntry0.getType()).isEqualTo(HistoryEntryType.CUSTOMER_REGISTERED);
        assertThat(historyEntry0.getData()).containsEntry("strategy", "native");

        HistoryEntry historyEntry1 = customer.getHistory().getItems().get(1);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.CUSTOMER_REGISTERED);
        assertThat(historyEntry1.getData()).containsEntry("strategy", "native");

        HistoryEntry historyEntry2 = customer.getHistory().getItems().get(2);
        assertThat(historyEntry2.getType()).isEqualTo(HistoryEntryType.CUSTOMER_VERIFIED);
        assertThat(historyEntry2.getData()).containsEntry("strategy", "native");
    }

    @Test
    @Order(12)
    public void customer_account_creation_with_up_front_password() throws Exception {
        // reset instance variables
        password = "password2";
        emailAddress = "test2@test.com";
        verificationToken = null;

        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Lu");
        input.setLastName("Tester");
        input.setPhoneNumber("443324");
        input.setEmailAddress(emailAddress);
        input.setPassword(password);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(REGISTER_ACCOUNT, variables);
        Boolean result = graphQLResponse.get("$.data.registerCustomerAccount", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        verificationToken = (String) emailDetails.getModel().get("verificationToken");
        assertThat(verificationToken).isNotNull();

        CustomerListOptions options = new CustomerListOptions();
        StringOperators stringOperators = new StringOperators();
        stringOperators.setEq(emailAddress);
        CustomerFilterParameter filter = new CustomerFilterParameter();
        filter.setEmailAddress(stringOperators);
        options.setFilter(filter);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        variables = objectMapper.createObjectNode();
        variables.set("options", optionsNode);

        graphQLResponse = this.adminClient.perform(GET_CUSTOMER_LIST, variables);
        assertThat(graphQLResponse.isOk());
        CustomerList customerList = graphQLResponse.get("$.data.customers", CustomerList.class);
        assertThat(customerList.getItems()).hasSize(1);
        assertThat(customerList.getTotalItems()).isEqualTo(1);
        Customer customer = customerList.getItems().get(0);
        assertThat(customer.getFirstName()).isEqualTo(input.getFirstName());
        assertThat(customer.getLastName()).isEqualTo(input.getLastName());
        assertThat(customer.getEmailAddress()).isEqualTo(input.getEmailAddress());
        assertThat(customer.getPhoneNumber()).isEqualTo(input.getPhoneNumber());
    }

    @Test
    @Order(13)
    public void verification_fails_with_password() throws IOException {
        try {
            ObjectNode variables = objectMapper.createObjectNode();
            variables.put("password", "new password");
            variables.put("token", verificationToken);

            shopClient.perform(VERIFY_EMAIL, variables);

            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("A password has already been set during registration");
        }
    }

    @Test
    @Order(14)
    public void verification_succeeds_with_no_password_and_correct_token() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", verificationToken);

        GraphQLResponse graphQLResponse = shopClient.perform(VERIFY_EMAIL, variables);
        LoginResult loginResult = graphQLResponse.get("$.data.verifyCustomerAccount", LoginResult.class);
        assertThat(loginResult.getUser().getIdentifier()).isEqualTo(emailAddress);
    }

    String passwordResetToken;
    Customer customer;

    @Test
    @Order(15)
    public void requestPasswordReset_silently_fails_with_invalid_identifier() throws Exception {
        customer = getCustomerById(1L);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("identifier", "invalid-identifier");

        GraphQLResponse graphQLResponse = shopClient.perform(REQUEST_PASSWORD_RESET, variables);
        Boolean result = graphQLResponse.get("$.data.requestPasswordReset", Boolean.class);
        assertThat(result).isTrue();

        verify(emailSender, timeout(ASYNC_TIMEOUT).times(0)).send(any());
        assertThat(passwordResetToken).isNull();
    }

    @Test
    @Order(16)
    public void requestPasswordReset_sends_reset_token() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("identifier", customer.getEmailAddress());

        GraphQLResponse graphQLResponse = shopClient.perform(REQUEST_PASSWORD_RESET, variables);
        Boolean result = graphQLResponse.get("$.data.requestPasswordReset", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        passwordResetToken = (String) emailDetails.getModel().get("passwordResetToken");
        assertThat(passwordResetToken).isNotNull();
    }

    @Test
    @Order(17)
    public void resetPassword_fails_with_wrong_token() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("password", "newPassword");
        variables.put("token", "bad-token");

        try {
            shopClient.perform(RESET_PASSWORD, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Password reset token not recognized");
        }
    }

    @Test
    @Order(18)
    public void resetPassword_works_with_valid_token() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("password", "newPassword");
        variables.put("token", passwordResetToken);

        GraphQLResponse graphQLResponse = shopClient.perform(RESET_PASSWORD, variables);
        LoginResult loginResult = graphQLResponse.get("$.data.resetPassword", LoginResult.class);
        assertThat(loginResult.getUser().getIdentifier()).isEqualTo(customer.getEmailAddress());

        shopClient.asAnonymousUser();

        graphQLResponse = shopClient.asUserWithCredentials(customer.getEmailAddress(), "newPassword");
        loginResult = graphQLResponse.get("$.data.login", LoginResult.class);
        assertThat(loginResult.getUser().getIdentifier()).isEqualTo(customer.getEmailAddress());
    }

    @Test
    @Order(19)
    public void customer_history_for_password_reset() throws Exception {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setCurrentPage(2);
        options.setPageSize(3); // skip CUSTOMER_ADDRESS_CREATED entry
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer resultCustomer = graphQLResponse.get("$.data.customer", Customer.class);

        HistoryEntry historyEntry0 = resultCustomer.getHistory().getItems().get(0);
        assertThat(historyEntry0.getType()).isEqualTo(HistoryEntryType.CUSTOMER_PASSWORD_RESET_REQUESTED);
        assertThat(historyEntry0.getData()).isEmpty();

        HistoryEntry historyEntry1 = resultCustomer.getHistory().getItems().get(1);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.CUSTOMER_PASSWORD_RESET_VERIFIED);
        assertThat(historyEntry1.getData()).isEmpty();
    }

    /**
     * updating emailAddress test suite
     */

    String emailUpdateToken;
    final String NEW_EMAIL_ADDRESS = "new@address.com";
    final String PASSWORD = "newPassword";

    @Test
    @Order(20)
    public void throws_if_not_logged_in() throws IOException {
        customer = this.getCustomerById(1L);

        shopClient.asAnonymousUser();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("password", PASSWORD);
        variables.put("newEmailAddress", NEW_EMAIL_ADDRESS);

        try {
            shopClient.perform(REQUEST_UPDATE_EMAIL_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("FORBIDDEN");
        }
    }

    @Test
    @Order(21)
    public void throws_if_password_is_incorrect() throws IOException {
        try {
            shopClient.asUserWithCredentials(customer.getEmailAddress(), PASSWORD);

            ObjectNode variables = objectMapper.createObjectNode();
            variables = objectMapper.createObjectNode();
            variables.put("password", "bad password");
            variables.put("newEmailAddress", NEW_EMAIL_ADDRESS);

            shopClient.perform(REQUEST_UPDATE_EMAIL_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Test
    @Order(22)
    public void throws_if_email_address_already_in_use() throws IOException {
        try {
            shopClient.asUserWithCredentials(customer.getEmailAddress(), PASSWORD);
            Customer otherCustomer = getCustomerById(2L);

            ObjectNode variables = objectMapper.createObjectNode();
            variables = objectMapper.createObjectNode();
            variables.put("password", PASSWORD);
            variables.put("newEmailAddress", otherCustomer.getEmailAddress());

            shopClient.perform(REQUEST_UPDATE_EMAIL_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("This email address is not available");
        }
    }

    @Test
    @Order(23)
    public void triggers_event_with_token() throws Exception {
        shopClient.asUserWithCredentials(customer.getEmailAddress(), PASSWORD);

        ObjectNode variables = objectMapper.createObjectNode();
        variables = objectMapper.createObjectNode();
        variables.put("password", PASSWORD);
        variables.put("newEmailAddress", NEW_EMAIL_ADDRESS);

        GraphQLResponse graphQLResponse = shopClient.perform(REQUEST_UPDATE_EMAIL_ADDRESS, variables);
        assertThat(graphQLResponse.isOk());

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        emailUpdateToken = (String) emailDetails.getModel().get("identifierChangeToken");
        String pendingIdentifier = (String) emailDetails.getModel().get("pendingIdentifier");
        assertThat(emailUpdateToken).isNotNull();
        assertThat(pendingIdentifier).isEqualTo(NEW_EMAIL_ADDRESS);
    }

    @Test
    @Order(24)
    public void cannot_login_with_new_email_address_before_verification() throws Exception {
        try {
            shopClient.asUserWithCredentials(NEW_EMAIL_ADDRESS, PASSWORD);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Test
    @Order(25)
    public void throws_with_bad_token() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", "bad token");

        try {
            shopClient.perform(UPDATE_EMAIL_ADDRESS, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat("Identifier change token not recognized");
        }
    }

    @Test
    @Order(26)
    public void verify_the_new_email_address() throws Exception {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", emailUpdateToken);

        GraphQLResponse graphQLResponse = shopClient.perform(UPDATE_EMAIL_ADDRESS, variables);
        Boolean result = graphQLResponse.get("$.data.updateCustomerEmailAddress", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        assertThat(emailDetails.getEvent()).isInstanceOf(IdentifierChangeEvent.class);
    }

    @Test
    @Order(27)
    public void can_login_with_new_email_address_after_verification() throws IOException {
        shopClient.asUserWithCredentials(NEW_EMAIL_ADDRESS, PASSWORD);

        Customer activeCustomer = getActiveCustomer();
        assertThat(activeCustomer.getId()).isEqualTo(customer.getId());
        assertThat(activeCustomer.getEmailAddress()).isEqualTo(NEW_EMAIL_ADDRESS);
    }

    @Test
    @Order(28)
    public void cannot_login_with_old_email_address_after_verification() throws IOException {
        try {
            shopClient.asUserWithCredentials(customer.getEmailAddress(), PASSWORD);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorCode()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Test
    @Order(29)
    public void customer_history_for_email_update() throws IOException {
        HistoryEntryListOptions options = new HistoryEntryListOptions();
        options.setCurrentPage(2);
        options.setPageSize(5);
        JsonNode optionsNode = objectMapper.valueToTree(options);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customer.getId());
        variables.set("options", optionsNode);

        GraphQLResponse graphQLResponse = adminClient.perform(GET_CUSTOMER_HISTORY, variables);
        assertThat(graphQLResponse.isOk());

        Customer resultCustomer = graphQLResponse.get("$.data.customer", Customer.class);

        HistoryEntry historyEntry0 = resultCustomer.getHistory().getItems().get(0);
        assertThat(historyEntry0.getType()).isEqualTo(HistoryEntryType.CUSTOMER_EMAIL_UPDATE_REQUESTED);
        assertThat(historyEntry0.getData()).containsEntry("newEmailAddress", "new@address.com");
        assertThat(historyEntry0.getData()).containsKey("oldEmailAddress");


        HistoryEntry historyEntry1 = resultCustomer.getHistory().getItems().get(1);
        assertThat(historyEntry1.getType()).isEqualTo(HistoryEntryType.CUSTOMER_EMAIL_UPDATE_VERIFIED);
        assertThat(historyEntry0.getData()).containsEntry("newEmailAddress", "new@address.com");
        assertThat(historyEntry0.getData()).containsKey("oldEmailAddress");
    }

    private Customer getCustomerById(Long customerId) throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customerId);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_CUSTOMER, variables, Arrays.asList(CUSTOMER_FRAGMENT, ADDRESS_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        return graphQLResponse.get("$.data.customer", Customer.class);
    }

    private void beforeTest30() {
        configOptions.getAuthOptions().setVerificationTokenDuration("1ms");
    }

    /**
     * Expiring tokens
     */
    @Test
    @Order(30)
    public void attempting_to_verify_after_token_has_expired_throws() throws Exception {
        beforeTest30();

        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Barry");
        input.setLastName("Wallace");
        input.setEmailAddress("barry.wallace@test.com");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(REGISTER_ACCOUNT, variables);
        Boolean result = graphQLResponse.get("$.data.registerCustomerAccount", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        String verificationToken = (String) emailDetails.getModel().get("verificationToken");
        assertThat(verificationToken).isNotNull();

        try {
            variables = objectMapper.createObjectNode();
            variables.put("token", verificationToken);
            variables.put("password", MockDataService.TEST_PASSWORD);

            shopClient.perform(VERIFY_EMAIL, variables);

            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage())
                    .isEqualTo("Verification token has expired. Use refreshCustomerVerification to send a new token.");
        }
    }

    @Test
    @Order(31)
    public void attempting_to_reset_password_after_token_has_expired_throws() throws Exception {
        Customer customer = getCustomerById(1L);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("identifier", customer.getEmailAddress());

        GraphQLResponse graphQLResponse = shopClient.perform(REQUEST_PASSWORD_RESET, variables);
        Boolean result = graphQLResponse.get("$.data.requestPasswordReset", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        String passwordResetToken = (String) emailDetails.getModel().get("passwordResetToken");
        assertThat(passwordResetToken).isNotNull();

        variables = objectMapper.createObjectNode();
        variables.put("password", MockDataService.TEST_PASSWORD);
        variables.put("token", passwordResetToken);

        try {
            shopClient.perform(RESET_PASSWORD, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage()).isEqualTo("Password reset token has expired.");
        }
    }

    /**
     * Registration without email verification
     */

    private void beforeTest32() {
        configOptions.getAuthOptions().setRequireVerification(false);
    }

    final String userEmailAddress = "glen.beardsley@test.com";

    @Test
    @Order(32)
    public void errors_if_no_password_is_provided() throws IOException {
        beforeTest32();

        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Glen");
        input.setLastName("Beardsley");
        input.setEmailAddress(userEmailAddress);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            shopClient.perform(REGISTER_ACCOUNT, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getErrorMessage())
                    .isEqualTo("A password must be provided when `authOptions.requireVerification` is set to \"false\"");
        }
    }

    @Test
    @Order(33)
    public void register_a_new_account_with_password() throws Exception {
        RegisterCustomerInput input = new RegisterCustomerInput();
        input.setFirstName("Glen");
        input.setLastName("Beardsley");
        input.setEmailAddress(userEmailAddress);
        input.setPassword(MockDataService.TEST_PASSWORD);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse = shopClient.perform(REGISTER_ACCOUNT, variables);
        Boolean result = graphQLResponse.get("$.data.registerCustomerAccount", Boolean.class);
        assertThat(result).isTrue();

        verify(emailSender, timeout(ASYNC_TIMEOUT).times(0)).send(any());
    }


    @Test
    @Order(34)
    public void can_login_after_registering() throws IOException {
        shopClient.asUserWithCredentials(userEmailAddress, MockDataService.TEST_PASSWORD);

        GraphQLResponse graphQLResponse = shopClient.perform(GET_ME, null);
        CurrentUser currentUser = graphQLResponse.get("$.data.me", CurrentUser.class);
        assertThat(currentUser.getIdentifier()).isEqualTo(userEmailAddress);
    }

    /**
     * Updating email address without email verification
     */
    @Test
    @Order(35)
    public void updating_email_address_without_email_verification() throws Exception {
        shopClient.asUserWithCredentials(userEmailAddress, MockDataService.TEST_PASSWORD);

        ObjectNode variables = objectMapper.createObjectNode();
        variables = objectMapper.createObjectNode();
        variables.put("password", MockDataService.TEST_PASSWORD);
        variables.put("newEmailAddress", "newest@address.com");

        GraphQLResponse graphQLResponse = shopClient.perform(REQUEST_UPDATE_EMAIL_ADDRESS, variables);
        assertThat(graphQLResponse.isOk());
        Boolean result = graphQLResponse.get("$.data.requestUpdateCustomerEmailAddress", Boolean.class);
        assertThat(result).isTrue();

        ArgumentCaptor<EmailDetails> captor = ArgumentCaptor.forClass(EmailDetails.class);
        verify(emailSender, timeout(ASYNC_TIMEOUT).times(1)).send(captor.capture());
        EmailDetails emailDetails = captor.getValue();
        assertThat(emailDetails.getEvent()).isInstanceOf(IdentifierChangeEvent.class);
    }
}
