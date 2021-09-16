/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CustomerEntity;
import io.geekstore.mapper.CustomerEntityMapper;
import io.geekstore.types.customer.Customer;
import org.dataloader.MappedBatchLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
public class CustomerDataLoader implements MappedBatchLoader<Long, Customer> {
    private final CustomerEntityMapper customerEntityMapper;

    public CustomerDataLoader(CustomerEntityMapper customerEntityMapper) {
        this.customerEntityMapper = customerEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, Customer>> load(Set<Long> customerIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<CustomerEntity> customerEntities =
                    this.customerEntityMapper.selectBatchIds(customerIds);
            List<Customer> customers = customerEntities.stream()
                    .map(customerEntity -> BeanMapper.map(customerEntity, Customer.class))
                    .collect(Collectors.toList());
            Map<Long, Customer> customerMap = customers.stream()
                    .collect(Collectors.toMap(Customer::getId, c -> c));
            return customerMap;
        });
    }
}
