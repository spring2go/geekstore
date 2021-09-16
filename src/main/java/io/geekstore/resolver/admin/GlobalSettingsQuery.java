/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.GlobalSettingsEntity;
import io.geekstore.service.GlobalSettingsService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.settings.GlobalSettings;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class GlobalSettingsQuery implements GraphQLQueryResolver {

    private final GlobalSettingsService globalSettingsService;

    @Allow(Permission.ReadSettings)
    public GlobalSettings globalSettings(DataFetchingEnvironment dfe) {
        GlobalSettingsEntity globalSettingsEntity = this.globalSettingsService.getSettings();
        return BeanMapper.map(globalSettingsEntity, GlobalSettings.class);
    }
}
