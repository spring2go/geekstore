/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion.conditions;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.entity.OrderEntity;
import io.geekstore.service.CustomerService;
import io.geekstore.types.common.ConfigArgDefinition;
import io.geekstore.types.customer.CustomerGroup;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@SuppressWarnings("Duplicates")
public class CustomerGroupCondition extends PromotionCondition {

    private final Map<String, ConfigArgDefinition> argSpec;

    private Cache<Long, Set<Long>> customerGroupIdsCache =
            CacheBuilder.newBuilder().expireAfterWrite(5 * 60 * 1000, TimeUnit.MILLISECONDS).build();

    @Autowired
    private CustomerService customerService;

    public CustomerGroupCondition() {
        super(
                "customer_group",
                "Customer is a member of the specified group"
        );

        Map<String, ConfigArgDefinition> argDefMap = new HashMap<>();
        ConfigArgDefinition argDef = new ConfigArgDefinition();
        argDef.setType("ID");
        argDef.setUi(ImmutableMap.of("component", "customer-group-form-input"));
        argDef.setLabel("Customer group");
        argDefMap.put("customerGroupId", argDef);

        argSpec = argDefMap;
    }

    @Override
    public boolean check(OrderEntity orderEntity, ConfigArgValues argValues) {
        if (orderEntity.getCustomerId() == null) {
            return false;
        }
        Set<Long> groupIds = customerGroupIdsCache.getIfPresent(orderEntity.getCustomerId());
        if (groupIds == null) {
            groupIds = customerService.getCustomerGroups(orderEntity.getCustomerId())
                    .stream().map(CustomerGroup::getId).collect(Collectors.toSet());
            customerGroupIdsCache.put(orderEntity.getCustomerId(), groupIds);
        }
        return groupIds.contains(argValues.getId("customerGroupId"));
    }

    /**
     * 仅用于测试的方法
     */
    public void clearCache() {
        customerGroupIdsCache.invalidateAll();
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        return argSpec;
    }
}
