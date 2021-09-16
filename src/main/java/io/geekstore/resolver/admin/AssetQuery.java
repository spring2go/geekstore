/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.AssetEntity;
import io.geekstore.service.AssetService;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.asset.AssetList;
import io.geekstore.types.asset.AssetListOptions;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class AssetQuery implements GraphQLQueryResolver {
    private final AssetService assetService;

    /**
     * Get a list of Assets
     */
    @Allow(Permission.ReadCatalog)
    public AssetList assets(AssetListOptions options, DataFetchingEnvironment dfe) {
        return this.assetService.findAll(options);
    }

    public Asset asset(Long id, DataFetchingEnvironment dfe) {
        AssetEntity assetEntity = this.assetService.findOne(id);
        if (assetEntity == null) return null;
        return BeanMapper.map(assetEntity, Asset.class);
    }
}
