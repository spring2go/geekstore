/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.config.TestConfig;
import io.geekstore.types.administrator.Administrator;
import io.geekstore.types.administrator.AdministratorList;
import io.geekstore.types.administrator.CreateAdministratorInput;
import io.geekstore.types.administrator.UpdateAdministratorInput;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class AdministratorTest {

    public static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    public static final String ADMINISTRATOR_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "administrator_fragment");
    public static final String CREATE_ADMINISTRATOR =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_administrator");

    public static final String ADMINISTRATOR_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/administrator/%s.graphqls";
    public static final String GET_ADMINISTRATORS =
            String.format(ADMINISTRATOR_GRAPHQL_RESOURCE_TEMPLATE, "get_administrators");
    public static final String GET_ADMINISTRATOR =
            String.format(ADMINISTRATOR_GRAPHQL_RESOURCE_TEMPLATE, "get_administrator");
    public static final String UPDATE_ADMINISTRATOR =
            String.format(ADMINISTRATOR_GRAPHQL_RESOURCE_TEMPLATE, "update_administrator");
    public static final String DELETE_ADMINISTRATOR =
            String.format(ADMINISTRATOR_GRAPHQL_RESOURCE_TEMPLATE, "delete_administrator");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    Administrator createdAdmin;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asSuperAdmin();
    }

    @Test
    @Order(1)
    public void administrators() throws IOException {
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ADMINISTRATORS, null, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        AdministratorList administratorList =
                graphQLResponse.get("$.data.administrators", AdministratorList.class);
        assertThat(administratorList.getItems()).hasSize(1);
        assertThat(administratorList.getTotalItems()).isEqualTo(1);
    }

    @Test
    @Order(2)
    public void createAdministrator() throws IOException {
        CreateAdministratorInput input = new CreateAdministratorInput();
        input.setEmailAddress("test@test.com");
        input.setFirstName("First");
        input.setLastName("Last");
        input.setPassword("password1234");
        input.getRoleIds().add(1L);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(CREATE_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        createdAdmin = graphQLResponse.get("$.data.createAdministrator", Administrator.class);

        assertThat(createdAdmin.getEmailAddress()).isEqualTo(input.getEmailAddress());
        assertThat(createdAdmin.getFirstName()).isEqualTo(input.getFirstName());
        assertThat(createdAdmin.getLastName()).isEqualTo(input.getLastName());
        assertThat(createdAdmin.getUser().getRoles().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @Order(3)
    public void administrator() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdAdmin.getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Administrator administrator = graphQLResponse.get("$.data.administrator", Administrator.class);
        assertThat(administrator).isEqualTo(createdAdmin);
    }

    @Test
    @Order(4)
    public void updateAdministrator() throws IOException {
        UpdateAdministratorInput input = new UpdateAdministratorInput();
        input.setId(createdAdmin.getId());
        input.setEmailAddress("new_email@test.com");
        input.setFirstName("new first");
        input.setLastName("new last");
        input.setPassword("new_password1234");
        input.getRoleIds().add(2L);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(UPDATE_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Administrator administrator = graphQLResponse.get("$.data.updateAdministrator", Administrator.class);

        assertThat(administrator.getEmailAddress()).isEqualTo(input.getEmailAddress());
        assertThat(administrator.getFirstName()).isEqualTo(input.getFirstName());
        assertThat(administrator.getLastName()).isEqualTo(input.getLastName());
        assertThat(administrator.getUser().getRoles().get(0).getId()).isEqualTo(2L);
    }

    @Test
    @Order(5)
    public void updateAdministrator_works_with_partial_input() throws IOException {
        UpdateAdministratorInput input = new UpdateAdministratorInput();
        input.setId(createdAdmin.getId());
        input.setEmailAddress("newest_email@test.com");
        input.getRoleIds().add(2L);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(UPDATE_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Administrator administrator = graphQLResponse.get("$.data.updateAdministrator", Administrator.class);

        assertThat(administrator.getEmailAddress()).isEqualTo(input.getEmailAddress());
        assertThat(administrator.getFirstName()).isEqualTo("new first");
        assertThat(administrator.getLastName()).isEqualTo("new last");
        assertThat(administrator.getUser().getRoles().get(0).getId()).isEqualTo(2L);
    }

    @Test
    @Order(6)
    public void updateAdministrator_throws_with_invalid_roleId() throws IOException {
        UpdateAdministratorInput input = new UpdateAdministratorInput();
        input.setId(createdAdmin.getId());
        input.setEmailAddress("new_email@test.com");
        input.setFirstName("new first");
        input.setLastName("new last");
        input.setPassword("new_password1234");
        input.getRoleIds().add(999L);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            this.adminClient.perform(UPDATE_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("No Role with the id '999' could be found");
        }
    }

    @Test
    @Order(7)
    public void deleteAdministrator() throws IOException {
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ADMINISTRATORS, null, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        AdministratorList administratorsBefore =
                graphQLResponse.get("$.data.administrators", AdministratorList.class);
        assertThat(administratorsBefore.getTotalItems()).isEqualTo(2);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdAdmin.getId());

        graphQLResponse =
                this.adminClient.perform(DELETE_ADMINISTRATOR, variables);
        assertThat(graphQLResponse.isOk());
        DeletionResponse deletionResponse =
                graphQLResponse.get("$.data.deleteAdministrator", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        graphQLResponse =
                this.adminClient.perform(GET_ADMINISTRATORS, null, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        AdministratorList administratorsAfter =
                graphQLResponse.get("$.data.administrators", AdministratorList.class);
        assertThat(administratorsAfter.getTotalItems()).isEqualTo(1);
    }

    @Test
    @Order(8)
    public void cannot_query_a_deleted_Administrator() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdAdmin.getId());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(GET_ADMINISTRATOR, variables, Arrays.asList(ADMINISTRATOR_FRAGMENT));
        assertThat(graphQLResponse.isOk());
        Administrator administrator = graphQLResponse.get("$.data.administrator", Administrator.class);
        assertThat(administrator).isNull();
    }
}
