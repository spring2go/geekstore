/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.Constant;
import io.geekstore.entity.AuthenticationMethodEntity;
import io.geekstore.mapper.AuthenticationMethodEntityMapper;
import io.geekstore.types.user.AuthenticationMethod;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dataloader.MappedBatchLoader;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */

public class UserAuthenticationMethodsDataLoader implements MappedBatchLoader<Long, List<AuthenticationMethod>> {
    private final AuthenticationMethodEntityMapper authenticationMethodEntityMapper;

    public UserAuthenticationMethodsDataLoader(AuthenticationMethodEntityMapper authenticationMethodEntityMapper) {
        this.authenticationMethodEntityMapper = authenticationMethodEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, List<AuthenticationMethod>>> load(Set<Long> userIds) {
        return CompletableFuture.supplyAsync(() -> {
            QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(AuthenticationMethodEntity::getUserId, userIds);
            List<AuthenticationMethodEntity> authenticationMethodEntityList =
                    this.authenticationMethodEntityMapper.selectList(queryWrapper);

            if (authenticationMethodEntityList.size() == 0) {
                Map<Long, List<AuthenticationMethod>> emptyMap = new HashMap<>();
                userIds.forEach(id -> emptyMap.put(id, new ArrayList<>()));
                return emptyMap;
            }

            Map<Long, List<AuthenticationMethod>> groupByUserId = authenticationMethodEntityList.stream()
                    .collect(Collectors.groupingBy(AuthenticationMethodEntity::getUserId,
                            Collectors.mapping(m -> {
                                AuthenticationMethod authenticationMethod = new AuthenticationMethod();
                                authenticationMethod.setId(m.getId());
                                authenticationMethod.setCreatedAt(m.getCreatedAt());
                                authenticationMethod.setUpdatedAt(m.getUpdatedAt());
                                authenticationMethod.setStrategy(
                                        m.isExternal() ? m.getStrategy() : Constant.NATIVE_AUTH_STRATEGY_NAME);
                                return authenticationMethod;
                                }, Collectors.toList())));

            return groupByUserId;
        });
    }
}
