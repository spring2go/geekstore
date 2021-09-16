/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.dataloader;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AdministratorEntity;
import io.geekstore.mapper.AdministratorEntityMapper;
import io.geekstore.types.administrator.Administrator;
import org.dataloader.MappedBatchLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class HistoryEntryAdministratorDataLoader implements MappedBatchLoader<Long, Administrator> {

    private final AdministratorEntityMapper administratorEntityMapper;

    public HistoryEntryAdministratorDataLoader(AdministratorEntityMapper administratorEntityMapper) {
        this.administratorEntityMapper = administratorEntityMapper;
    }

    @Override
    public CompletionStage<Map<Long, Administrator>> load(Set<Long> administratorIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<AdministratorEntity> administratorEntityList =
                    this.administratorEntityMapper.selectBatchIds(administratorIds);
            Map<Long, Administrator> administratorMap = administratorEntityList.stream()
                            .collect(Collectors.toMap(AdministratorEntity::getId,
                                    administratorEntity -> BeanMapper.map(administratorEntity, Administrator.class)));
            return administratorMap;
        });
    }
}
