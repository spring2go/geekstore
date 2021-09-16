/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.e2e;

import io.geekstore.*;
import io.geekstore.common.Constant;
import io.geekstore.config.TestConfig;
import io.geekstore.config.session_cache.CachedSession;
import io.geekstore.config.session_cache.SessionCacheStrategy;
import io.geekstore.options.ConfigOptions;
import io.geekstore.utils.TestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.graphql.spring.boot.test.GraphQLResponse;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;

/**
 * Created on Nov, 2020 by @author bobo
 */
@GeekStoreGraphQLTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
public class SessionManagementTest {

    public static final String SHARED_GRAPHQL_RESOURCE_TEMPLATE = "graphql/shared/%s.graphqls";
    public static final String ADMIN_ME =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "admin_me");
    public static final String CURRENT_USER_FRAGMENT =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "current_user_fragment");
    public static final String ADMIN_ATTEMPT_LOGIN =
            String.format(SHARED_GRAPHQL_RESOURCE_TEMPLATE, "admin_attempt_login");
    public static final String ADMIN_LOGOUT = "graphql/admin_logout.graphqls";

    @Autowired
    TestHelper testHelper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockDataService mockDataService;

    @SpyBean
    @Autowired
    SessionCacheStrategy sessionCacheStrategy;

    @Autowired
    ConfigOptions configOptions;

    @Autowired
    Map<String, CachedSession> testSessionCache;

    @BeforeAll
    void beforeAll() throws IOException {
        PopulateOptions populateOptions = PopulateOptions.builder().customerCount(1).build();
        populateOptions.setInitialData(testHelper.getInitialData());
        populateOptions.setProductCsvPath(testHelper.getTestFixture("e2e-products-minimal.csv"));

        mockDataService.populate(populateOptions);
        adminClient.asAnonymousUser();
        testSessionCache.clear();
        configOptions.getAuthOptions().setSessionCacheTTL(2);
    }

    @Test
    @Order(1)
    public void populates_the_cache_on_login() throws IOException {
        Mockito.clearInvocations(sessionCacheStrategy);
        assertThat(testSessionCache).isEmpty();

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("username", Constant.SUPER_ADMIN_USER_IDENTIFIER);
        variables.put("password", Constant.SUPER_ADMIN_USER_PASSWORD);

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADMIN_ATTEMPT_LOGIN, variables, Arrays.asList(CURRENT_USER_FRAGMENT));
        assertThat(graphQLResponse.isOk());

        assertThat(testSessionCache).hasSize(1);
        Mockito.verify(sessionCacheStrategy, Mockito.times(1)).set(any());
    }

    @Test
    @Order(2)
    public void takes_user_data_from_cache_on_next_request() throws IOException {
        Mockito.clearInvocations(sessionCacheStrategy);

        queryMe();

        assertThat(testSessionCache).hasSize(1);
        Mockito.verify(sessionCacheStrategy, Mockito.times(1)).get(any());
    }

    @Test
    @Order(3)
    public void sets_fresh_data_after_TTL_expires() throws Exception {
        Mockito.clearInvocations(sessionCacheStrategy);

        queryMe();
        Mockito.verify(sessionCacheStrategy, Mockito.times(0)).set(any());

        queryMe();
        Mockito.verify(sessionCacheStrategy, Mockito.times(0)).set(any());

        Thread.sleep(2100);

        queryMe();
        Mockito.verify(sessionCacheStrategy, Mockito.times(1)).set(any());
    }

    @Test
    @Order(4)
    public void clears_cache_for_that_user_on_logout() throws Exception {
        Mockito.clearInvocations(sessionCacheStrategy);
        Mockito.verify(sessionCacheStrategy, Mockito.times(0)).delete(any());

        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADMIN_LOGOUT, null);
        assertThat(graphQLResponse.isOk());

        assertThat(testSessionCache).isEmpty();
        Mockito.verify(sessionCacheStrategy, Mockito.times(1)).delete(any());
    }

    private void beforeTest5() throws IOException {
        configOptions.getAuthOptions().setSessionCacheTTL(1);
        configOptions.getAuthOptions().setSessionDuration("3s");
        adminClient.asSuperAdmin();
    }

    private void queryMe() throws IOException {
        GraphQLResponse graphQLResponse =
                this.adminClient.perform(ADMIN_ME, null, Arrays.asList(CURRENT_USER_FRAGMENT));
        assertThat(graphQLResponse.isOk());
    }

    /**
     * Session expiry
     */
    @Test
    @Order(5)
    public void session_does_not_expire_with_continued_use() throws Exception {
        beforeTest5();

        adminClient.asSuperAdmin();
        Thread.sleep(1000);
        queryMe();
        Thread.sleep(1000);
        queryMe();
        Thread.sleep(1000);
        queryMe();
        Thread.sleep(1000);
        queryMe();
    }

    @Test
    @Order(6)
    public void session_expires_when_not_used_for_longer_than_sessionDuration() throws Exception {
        adminClient.asSuperAdmin();
        Thread.sleep(3500);
        try {
            queryMe();
            fail("Should have thrown");
        } catch (ApiException apiEx) {
            assertThat(apiEx.getMessage()).isEqualTo("You are not currently authorized to perform this action");
        }
    }
}
