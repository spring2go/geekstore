/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore;

import io.geekstore.config.TestConfig;
import io.geekstore.data_import.importer.Importer;
import io.geekstore.data_import.populator.Populator;
import io.geekstore.service.ConfigService;
import io.geekstore.types.ImportInfo;
import io.geekstore.types.common.CreateAddressInput;
import io.geekstore.types.common.CreateCustomerInput;
import io.geekstore.types.customer.Customer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import com.graphql.spring.boot.test.GraphQLResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.util.stream.Collectors;

/**
 * An importer for creating mock data via the GraphQL API.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Slf4j
public class MockDataService {

    public static final String MOCK_GRAPHQL_RESOURCE_TEMPLATE = "graphql/mock/%s.graphqls";
    public static final String CREATE_CUSTOMER =
            String.format(MOCK_GRAPHQL_RESOURCE_TEMPLATE, "create_customer");
    public static final String CREATE_CUSTOMER_ADDRESS =
            String.format(MOCK_GRAPHQL_RESOURCE_TEMPLATE, "create_customer_address");

    public static final String TEST_PASSWORD = "test123456";

    @Autowired
    ObjectMapper mapper;

    @Autowired
    @Qualifier(TestConfig.ADMIN_CLIENT_BEAN)
    ApiClient adminClient;

    @Autowired
    ConfigService configService;

    @Autowired
    Importer importer;
    @Autowired
    Populator populator;

    private final Faker faker = Faker.instance();

    /**
     * Clears all tables from the database and populates with (deterministic) random data.
     */
    public void populate(PopulateOptions options) throws IOException {
        boolean originalRequiredVerification = this.configService.getAuthOptions().isRequireVerification();
        this.configService.getAuthOptions().setRequireVerification(false);

        populator.populateInitialData(options.getInitialData());
        this.populateProducts(options.getProductCsvPath(), options.isLogging());
        populator.populateCollections(options.getInitialData());
        Integer customerCount = options.getCustomerCount() == null ? 10 : options.getCustomerCount();

        adminClient.asSuperAdmin();
        populateCustomers(customerCount);

        this.configService.getAuthOptions().setRequireVerification(originalRequiredVerification);
    }

    public void populateCustomers() throws IOException {
        this.populateCustomers(10);
    }

    public void populateCustomers(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();

            CreateCustomerInput createCustomerInput = new CreateCustomerInput();
            createCustomerInput.setFirstName(firstName);
            createCustomerInput.setLastName(lastName);
            createCustomerInput.setEmailAddress(faker.internet().emailAddress());
            createCustomerInput.setPhoneNumber(faker.phoneNumber().phoneNumber());

            JsonNode inputNode = mapper.valueToTree(createCustomerInput);
            ObjectNode variables = mapper.createObjectNode();
            variables.set("input", inputNode);
            variables.put("password", TEST_PASSWORD);

            GraphQLResponse graphQLResponse = this.adminClient.perform(CREATE_CUSTOMER, variables);
            Customer customer = graphQLResponse.get("$.data.createCustomer", Customer.class);

            if (customer == null) continue;

            CreateAddressInput createAddressInput = new CreateAddressInput();
            createAddressInput.setFullName(firstName + " " + lastName);
            createAddressInput.setStreetLine1(faker.address().streetAddress());
            createAddressInput.setCity(faker.address().city());
            createAddressInput.setProvince(faker.address().state());
            createAddressInput.setPostalCode(faker.address().zipCode());

            inputNode = mapper.valueToTree(createAddressInput);
            variables = mapper.createObjectNode();
            variables.set("input", inputNode);
            variables.put("customerId", customer.getId());

            this.adminClient.perform(CREATE_CUSTOMER_ADDRESS, variables);
        }
        log.info("Created " + count + " Customers");
    }

    public void populateProducts(String productsCsvPath, boolean logging) throws FileNotFoundException {
        ImportInfo importResult = importProductsFromCsv(productsCsvPath);
        if (!CollectionUtils.isEmpty(importResult.getErrors())) {
            log.error(importResult.getErrors().size() + " errors encountered when importing product data:");
            log.error(importResult.getErrors().stream().collect(Collectors.joining("\n")));
        }
        if (logging) {
            log.info("Imported " + importResult.getImported() + " products");
        }
    }

    private ImportInfo importProductsFromCsv(String productCsvPath) throws FileNotFoundException {
        InputStream productDataInputStream = new FileInputStream(new File(productCsvPath));
        return importer.parseAndImport(productDataInputStream);
    }


}
