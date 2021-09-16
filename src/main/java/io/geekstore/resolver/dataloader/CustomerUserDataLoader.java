/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.CustomerEntity;
import io.geekstore.entity.UserEntity;
import io.geekstore.mapper.CustomerEntityMapper;
import io.geekstore.mapper.UserEntityMapper;
import io.geekstore.types.user.User;
import org.dataloader.MappedBatchLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class CustomerUserDataLoader implements MappedBatchLoader<Long, User> {
    private final UserEntityMapper userEntityMapper;
    private final CustomerEntityMapper customerEntityMapper;

    public CustomerUserDataLoader(UserEntityMapper userEntityMapper, CustomerEntityMapper customerEntityMapper) {
        this.userEntityMapper = userEntityMapper;
        this.customerEntityMapper = customerEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, User>> load(Set<Long> customerIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, User> customerUserMap = new HashMap<>();
            List<CustomerEntity> customerEntityList =
                    this.customerEntityMapper.selectBatchIds(customerIds);
            if (customerEntityList.size() == 0) return customerUserMap;
            List<Long> userIdList = customerEntityList.stream()
                    .map(CustomerEntity::getUserId)
                    .collect(Collectors.toList());

            List<UserEntity> userEntityList = this.userEntityMapper.selectBatchIds(userIdList);
            if (userEntityList.size() == 0) return customerUserMap;

            Map<Long, UserEntity> userEntityMap = userEntityList.stream()
                    .collect(Collectors.toMap(UserEntity::getId, userEntity -> userEntity));

            customerEntityList.forEach(customerEntity -> {
                Long customerId = customerEntity.getId();
                Long userId = customerEntity.getUserId();
                UserEntity userEntity = userEntityMap.get(userId);
                if (userEntity != null) {
                    User user = BeanMapper.patch(userEntity, User.class);
                    customerUserMap.put(customerId, user);
                }
            });

            return customerUserMap;
        });
    }
}
