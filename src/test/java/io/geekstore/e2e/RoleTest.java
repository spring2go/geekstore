/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.common.RoleCode;
import io.geekstore.config.TestConfig;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.common.Permission;
import io.geekstore.types.role.CreateRoleInput;
import io.geekstore.types.role.Role;
import io.geekstore.types.role.RoleList;
import io.geekstore.types.role.UpdateRoleInput;
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

import static org.assertj.core.api.Assertions.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class RoleTest {

    public static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    public static final String ROLE_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "role_fragment");

    static final String ADMIN_ROLE_GRAPHQL_RESOURCE_TEMPLATE = "graphql/admin/role/%s.graphqls";
    static final String GET_ROLES =
            String.format(ADMIN_ROLE_GRAPHQL_RESOURCE_TEMPLATE, "get_roles");
    static final String CREATE_ROLE =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "create_role");
    static final String GET_ROLE =
            String.format(ADMIN_ROLE_GRAPHQL_RESOURCE_TEMPLATE, "get_role");
    static final String UPDATE_ROLE =
            String.format(ADMIN_ROLE_GRAPHQL_RESOURCE_TEMPLATE, "update_role");
    static final String DELETE_ROLE =
            String.format(ADMIN_ROLE_GRAPHQL_RESOURCE_TEMPLATE, "delete_role");

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    Role createdRole;
    List<Role>  defaultRoles;

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
    public void roles() throws IOException {
        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ROLES, null, Arrays.asList(ROLE_FRAGMENT));
        RoleList roleList = graphQLResponse.get("$.data.roles", RoleList.class);

        assertThat(roleList.getItems()).hasSize(2);
        assertThat(roleList.getTotalItems()).isEqualTo(2);
        defaultRoles = roleList.getItems();
    }

    @Test
    @Order(2)
    public void createRole_with_invalid_permission() throws IOException {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("code", "test");
        input.put("description", "test role");
        input.putArray("permissions").add("bad permission");

        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", input);

        try {
            adminClient.perform(CREATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo(
                    "Variable 'permissions' has an invalid value : Invalid input for Enum 'Permission'. No value found for name 'bad permission'"
            );
        }
    }

    @Test
    @Order(3)
    public void createRole_with_no_permissions_includes_Authenticated() throws IOException {
        CreateRoleInput input = new CreateRoleInput();
        input.setCode("test");
        input.setDescription("test code");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(CREATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role role = graphQLResponse.get("$.data.createRole", Role.class);
        assertThat(role.getCode()).isEqualTo(input.getCode());
        assertThat(role.getDescription()).isEqualTo(input.getDescription());
        assertThat(role.getId()).isEqualTo(3L);
        assertThat(role.getPermissions()).containsExactly(Permission.Authenticated);
    }

    @Test
    @Order(4)
    public void createRole_deduplicates_permissions() throws IOException {
        CreateRoleInput input = new CreateRoleInput();
        input.setCode("test2");
        input.setDescription("test role2");
        input.getPermissions().add(Permission.ReadSettings);
        input.getPermissions().add(Permission.ReadSettings);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(CREATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role role = graphQLResponse.get("$.data.createRole", Role.class);
        assertThat(role.getCode()).isEqualTo(input.getCode());
        assertThat(role.getDescription()).isEqualTo(input.getDescription());
        assertThat(role.getId()).isEqualTo(4L);
        assertThat(role.getPermissions()).containsExactlyInAnyOrder(Permission.Authenticated, Permission.ReadSettings);
    }

    @Test
    @Order(5)
    public void createRole_with_permissions() throws IOException {
        CreateRoleInput input = new CreateRoleInput();
        input.setCode("test");
        input.setDescription("test role");
        input.getPermissions().add(Permission.ReadCustomer);
        input.getPermissions().add(Permission.UpdateCustomer);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(CREATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        createdRole = graphQLResponse.get("$.data.createRole", Role.class);
        assertThat(createdRole.getCode()).isEqualTo(input.getCode());
        assertThat(createdRole.getDescription()).isEqualTo(input.getDescription());
        assertThat(createdRole.getId()).isEqualTo(5L);
        assertThat(createdRole.getPermissions()).containsExactlyInAnyOrder(
                Permission.Authenticated, Permission.ReadCustomer, Permission.UpdateCustomer);
    }

    @Test
    @Order(6)
    public void role() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdRole.getId());

        GraphQLResponse graphQLResponse =
                adminClient.perform(GET_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role role = graphQLResponse.get("$.data.role", Role.class);
        assertThat(role).isEqualTo(createdRole);
    }

    @Test
    @Order(7)
    public void updateRole() throws IOException {
        UpdateRoleInput input = new UpdateRoleInput();
        input.setId(createdRole.getId());
        input.setCode("test-modified");
        input.setDescription("test role modified");
        input.getPermissions().add(Permission.ReadCustomer);
        input.getPermissions().add(Permission.UpdateCustomer);
        input.getPermissions().add(Permission.DeleteCustomer);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role updatedRole = graphQLResponse.get("$.data.updateRole", Role.class);
        assertThat(updatedRole.getCode()).isEqualTo(input.getCode());
        assertThat(updatedRole.getDescription()).isEqualTo(input.getDescription());
        assertThat(updatedRole.getId()).isEqualTo(5L);
        assertThat(updatedRole.getPermissions()).containsExactlyInAnyOrder(
                Permission.Authenticated,
                Permission.ReadCustomer,
                Permission.UpdateCustomer,
                Permission.DeleteCustomer);
    }

    @Test
    @Order(8)
    public void updateRole_works_with_partial_input() throws IOException {
        UpdateRoleInput input = new UpdateRoleInput();
        input.setId(createdRole.getId());
        input.setCode("test-modified-again");

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role updatedRole = graphQLResponse.get("$.data.updateRole", Role.class);
        assertThat(updatedRole.getCode()).isEqualTo(input.getCode());
        assertThat(updatedRole.getDescription()).isEqualTo("test role modified");
        assertThat(updatedRole.getId()).isEqualTo(5L);
        assertThat(updatedRole.getPermissions()).containsExactlyInAnyOrder(
                Permission.Authenticated,
                Permission.ReadCustomer,
                Permission.UpdateCustomer,
                Permission.DeleteCustomer);
    }

    @Test
    @Order(9)
    public void updateRole_deduplicates_permissions() throws IOException {
        UpdateRoleInput input = new UpdateRoleInput();
        input.setId(createdRole.getId());
        input.getPermissions().add(Permission.Authenticated);
        input.getPermissions().add(Permission.Authenticated);
        input.getPermissions().add(Permission.ReadCustomer);
        input.getPermissions().add(Permission.ReadCustomer);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        GraphQLResponse graphQLResponse =
                adminClient.perform(UPDATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role updatedRole = graphQLResponse.get("$.data.updateRole", Role.class);
        assertThat(updatedRole.getPermissions()).containsExactlyInAnyOrder(
                Permission.Authenticated,
                Permission.ReadCustomer);
    }

    @Test
    @Order(10)
    public void updateRole_is_not_allowed_for_SuperAdmin_role() throws IOException {
        Role superAdminRole = defaultRoles.stream().filter(r -> RoleCode.SUPER_ADMIN_ROLE_CODE.equals(r.getCode()))
                .findFirst().orElse(null);
        if (superAdminRole == null) {
            fail("Could not find SuperAdmin role");
            return;
        }
        UpdateRoleInput input = new UpdateRoleInput();
        input.setId(superAdminRole.getId());
        input.setCode("superadmin-modified");
        input.setDescription("superadmin modified");
        input.getPermissions().add(Permission.Authenticated);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(UPDATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("The role '__super_admin_role__' cannot be modified");
        }
    }

    @Test
    @Order(11)
    public void updateRole_is_not_allowed_for_Customer_role() throws IOException {
        Role customerRole = defaultRoles.stream().filter(r -> RoleCode.CUSTOMER_ROLE_CODE.equals(r.getCode()))
                .findFirst().orElse(null);
        if (customerRole == null) {
            fail("Could not find Customer role");
            return;
        }
        UpdateRoleInput input = new UpdateRoleInput();
        input.setId(customerRole.getId());
        input.setCode("customer-modified");
        input.setDescription("customer modified");
        input.getPermissions().add(Permission.Authenticated);
        input.getPermissions().add(Permission.DeleteAdministrator);

        JsonNode inputNode = objectMapper.valueToTree(input);
        ObjectNode variables = objectMapper.createObjectNode();
        variables.set("input", inputNode);

        try {
            adminClient.perform(UPDATE_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("The role '__customer_role__' cannot be modified");
        }
    }

    @Test
    @Order(12)
    public void deleteRole_is_not_allowed_for_Customer_role() throws IOException {
        Role customerRole = defaultRoles.stream().filter(r -> RoleCode.CUSTOMER_ROLE_CODE.equals(r.getCode()))
                .findFirst().orElse(null);
        if (customerRole == null) {
            fail("Could not find Customer role");
            return;
        }
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", customerRole.getId());

        try {
            adminClient.perform(DELETE_ROLE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("The role '__customer_role__' cannot be deleted");
        }
    }

    @Test
    @Order(13)
    public void deleteRole_is_not_allowed_for_SuperAdmin_role() throws IOException {
        Role superAdminRole = defaultRoles.stream().filter(r -> RoleCode.SUPER_ADMIN_ROLE_CODE.equals(r.getCode()))
                .findFirst().orElse(null);
        if (superAdminRole == null) {
            fail("Could not find SuperAdmin role");
            return;
        }
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", superAdminRole.getId());

        try {
            adminClient.perform(DELETE_ROLE, variables);
            fail("should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("The role '__super_admin_role__' cannot be deleted");
        }
    }

    @Test
    @Order(14)
    public void deleteRole_deletes_a_role() throws IOException {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("id", createdRole.getId());

        GraphQLResponse graphQLResponse = adminClient.perform(DELETE_ROLE, variables);
        DeletionResponse deletionResponse = graphQLResponse.get("$.data.deleteRole", DeletionResponse.class);
        assertThat(deletionResponse.getResult()).isEqualTo(DeletionResult.DELETED);

        graphQLResponse =
                adminClient.perform(GET_ROLE, variables, Arrays.asList(ROLE_FRAGMENT));
        Role role = graphQLResponse.get("$.data.role", Role.class);
        assertThat(role).isNull();
    }
}
