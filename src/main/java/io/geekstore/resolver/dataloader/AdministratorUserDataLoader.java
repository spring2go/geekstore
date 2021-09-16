/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AdministratorEntity;
import io.geekstore.entity.UserEntity;
import io.geekstore.mapper.AdministratorEntityMapper;
import io.geekstore.mapper.UserEntityMapper;
import io.geekstore.types.user.User;
import org.dataloader.MappedBatchLoader;
import org.springframework.util.CollectionUtils;

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
@SuppressWarnings("Duplicates")
public class AdministratorUserDataLoader implements MappedBatchLoader<Long, User> {
    private final UserEntityMapper userEntityMapper;
    private final AdministratorEntityMapper administratorEntityMapper;

    public AdministratorUserDataLoader(UserEntityMapper userEntityMapper,
                                       AdministratorEntityMapper administratorEntityMapper) {
        this.userEntityMapper = userEntityMapper;
        this.administratorEntityMapper = administratorEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, User>> load(Set<Long> administratorIds) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Long, User> administratorUserMap = new HashMap<>();
            List<AdministratorEntity> administratorEntityList =
                    this.administratorEntityMapper.selectBatchIds(administratorIds);
            if (CollectionUtils.isEmpty(administratorEntityList)) return administratorUserMap;

            List<Long> userIdList = administratorEntityList.stream()
                    .map(AdministratorEntity::getUserId)
                    .collect(Collectors.toList());

            List<UserEntity> userEntityList = this.userEntityMapper.selectBatchIds(userIdList);
            if (CollectionUtils.isEmpty(userEntityList)) return administratorUserMap;

            Map<Long, UserEntity> userEntityMap = userEntityList.stream()
                    .collect(Collectors.toMap(UserEntity::getId, userEntity -> userEntity));

            administratorEntityList.forEach(administratorEntity -> {
                Long administratorId = administratorEntity.getId();
                Long userId = administratorEntity.getUserId();
                UserEntity userEntity = userEntityMap.get(userId);
                if (userEntity != null) {
                    User user = BeanMapper.map(userEntity, User.class);
                    administratorUserMap.put(administratorId, user);
                }
            });

            return administratorUserMap;
        });
    }
}
