/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.GlobalSettingsEntity;
import io.geekstore.exception.InternalServerError;
import io.geekstore.mapper.GlobalSettingsEntityMapper;
import io.geekstore.types.settings.UpdateGlobalSettingsInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class GlobalSettingsService {
    private final GlobalSettingsEntityMapper globalSettingsEntityMapper;

    @PostConstruct
    void initGlobalSettings() {
        try {
            this.getSettings();
        } catch (Exception ex) {
            GlobalSettingsEntity globalSettingsEntity = new GlobalSettingsEntity();
            this.globalSettingsEntityMapper.insert(globalSettingsEntity);
        }
    }

    public GlobalSettingsEntity getSettings() {
        GlobalSettingsEntity globalSettingsEntity = this.globalSettingsEntityMapper.selectOne(null);
        if (globalSettingsEntity == null) {
            throw new InternalServerError("Global settings not found");
        }
        return globalSettingsEntity;
    }

    public GlobalSettingsEntity updateSettings(UpdateGlobalSettingsInput input) {
        GlobalSettingsEntity globalSettingsEntity = getSettings();
        BeanMapper.patch(input, globalSettingsEntity);
        this.globalSettingsEntityMapper.updateById(globalSettingsEntity);
        return globalSettingsEntity;
    }

}