/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.entity.GlobalSettingsEntity;
import io.geekstore.service.ConfigService;
import io.geekstore.service.GlobalSettingsService;
import io.geekstore.service.OrderService;
import io.geekstore.types.settings.GlobalSettings;
import io.geekstore.types.settings.ServerConfig;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class GlobalSettingsResolver  implements GraphQLResolver<GlobalSettings> {

    private final GlobalSettingsService globalSettingsService;
    private final ConfigService configService;
    private final OrderService orderService;

    /**
     * Exposes a subset of the GeekStoreConfig which may be of use to clients
     */
    public ServerConfig getServerConfig(GlobalSettings globalSettings) {
        ServerConfig serverConfig = new ServerConfig();
        GlobalSettingsEntity globalSettingsEntity = globalSettingsService.getSettings();
        serverConfig.getCustomFields().putAll(globalSettingsEntity.getCustomFields());
        serverConfig.setOrderProcess(orderService.getOrderProcessStates());
        List<String> permittedAssetTypes = configService.getAssetOptions().getPermittedFileTypes();
        serverConfig.getPermittedAssetTypes().addAll(permittedAssetTypes);
        return serverConfig;
    }
}
