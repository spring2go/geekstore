/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AddressEntity;
import io.geekstore.mapper.AddressEntityMapper;
import io.geekstore.types.address.Address;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dataloader.MappedBatchLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class CustomerAddressesDataLoader implements MappedBatchLoader<Long, List<Address>> {
    private final AddressEntityMapper addressEntityMapper;

    public CustomerAddressesDataLoader(AddressEntityMapper addressEntityMapper) {
        this.addressEntityMapper = addressEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, List<Address>>> load(Set<Long> customerIds) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<AddressEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(AddressEntity::getCustomerId, customerIds);
            List<AddressEntity> addressEntityList = this.addressEntityMapper.selectList(queryWrapper);

            if (addressEntityList.size() == 0) {
                Map<Long, List<Address>> emptyMap = new HashMap<>();
                customerIds.forEach(id -> emptyMap.put(id, new ArrayList<>()));
                return emptyMap;
            }

            Map<Long, List<Address>> groupByCustomerId = addressEntityList.stream()
                    .collect(Collectors.groupingBy(AddressEntity::getCustomerId,
                            Collectors.mapping(addressEntity -> BeanMapper.map(addressEntity, Address.class),
                                    Collectors.toList()
                            )));

            return groupByCustomerId;
        });
    }
}
