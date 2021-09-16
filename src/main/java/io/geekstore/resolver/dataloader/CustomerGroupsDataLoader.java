/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CustomerGroupEntity;
import io.geekstore.entity.CustomerGroupJoinEntity;
import io.geekstore.mapper.CustomerGroupEntityMapper;
import io.geekstore.mapper.CustomerGroupJoinEntityMapper;
import io.geekstore.types.customer.CustomerGroup;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dataloader.MappedBatchLoader;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class CustomerGroupsDataLoader implements MappedBatchLoader<Long, List<CustomerGroup>> {

    private final CustomerGroupJoinEntityMapper customerGroupJoinEntityMapper;
    private final CustomerGroupEntityMapper customerGroupEntityMapper;

    public CustomerGroupsDataLoader(
            CustomerGroupJoinEntityMapper customerGroupJoinEntityMapper,
            CustomerGroupEntityMapper customerGroupEntityMapper) {
        this.customerGroupJoinEntityMapper = customerGroupJoinEntityMapper;
        this.customerGroupEntityMapper = customerGroupEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, List<CustomerGroup>>> load(Set<Long> customerIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, List<CustomerGroup>> customerGroupMap = new HashMap<>();
            customerIds.forEach(id -> customerGroupMap.put(id, new ArrayList<>()));

            QueryWrapper<CustomerGroupJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(CustomerGroupJoinEntity::getCustomerId, customerIds);
            List<CustomerGroupJoinEntity> customerGroupJoinEntityList =
                    this.customerGroupJoinEntityMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(customerGroupJoinEntityList)) return customerGroupMap;

            Set<Long> groupIds = customerGroupJoinEntityList.stream()
                    .map(CustomerGroupJoinEntity::getGroupId).collect(Collectors.toSet());
            List<CustomerGroupEntity> customerGroupEntityList = customerGroupEntityMapper.selectBatchIds(groupIds);
            if (CollectionUtils.isEmpty(customerGroupEntityList)) return customerGroupMap;

            Map<Long, CustomerGroupEntity> customerGroupEntityMap = customerGroupEntityList.stream()
                    .collect(Collectors.toMap(CustomerGroupEntity::getId, customerGroupEntity -> customerGroupEntity));

            customerGroupJoinEntityList.forEach(customerGroupJoinEntity -> {
                Long customerId = customerGroupJoinEntity.getCustomerId();
                Long groupId = customerGroupJoinEntity.getGroupId();
                List<CustomerGroup> customerGroupList = customerGroupMap.get(customerId);
                CustomerGroupEntity customerGroupEntity = customerGroupEntityMap.get(groupId);
                CustomerGroup customerGroup = BeanMapper.map(customerGroupEntity, CustomerGroup.class);
                customerGroupList.add(customerGroup);
            });

            return customerGroupMap;
        });
    }
}
