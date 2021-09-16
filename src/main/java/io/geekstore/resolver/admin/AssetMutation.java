/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.AssetEntity;
import io.geekstore.service.AssetService;
import io.geekstore.types.asset.Asset;
import io.geekstore.types.asset.UpdateAssetInput;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.servlet.http.Part;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class AssetMutation implements GraphQLMutationResolver {

    private final AssetService assetService;

    /**
     * Create new Assets
     */
    @Allow(Permission.CreateCatalog)
    public List<Asset> createAssets(List<Part> parts, DataFetchingEnvironment dfe) {
        /**
         *  TODO:
         *  Currently we validate _all_ mime types up-front due to limitations
         *  with the  existing error handling mechanisms.
         */
        this.assetService.validateInputMimeTypes(parts);
        /**
         * TODO:
         * Is there some way to parallelize this while still preserving
         * the order of files in the upload? Non-deterministic IDs mess up the e2e tests.
         */
        List<Asset> assets = new ArrayList<>();
        for(Part part : parts) {
            AssetEntity assetEntity =
                    this.assetService.create(RequestContext.fromDataFetchingEnvironment(dfe), part);
            assets.add(BeanMapper.map(assetEntity, Asset.class));
        }
        return assets;
    }

    /**
     * Create one new Asset
     */
    @Allow(Permission.CreateCatalog)
    public Asset createAsset(Part part, DataFetchingEnvironment dfe) {
        this.assetService.validateInputMimeTypes(Arrays.asList(part));
        AssetEntity assetEntity =
                this.assetService.create(RequestContext.fromDataFetchingEnvironment(dfe), part);
        return BeanMapper.map(assetEntity, Asset.class);
    }

    /**
     * Update an existing Asset
     */
    @Allow(Permission.UpdateCatalog)
    public Asset updateAsset(UpdateAssetInput input, DataFetchingEnvironment dfe) {
        AssetEntity assetEntity =
                this.assetService.update(RequestContext.fromDataFetchingEnvironment(dfe), input);
        return BeanMapper.map(assetEntity, Asset.class);
    }

    /**
     * Delete an Asset
     */
    @Allow(Permission.DeleteCatalog)
    public DeletionResponse deleteAsset(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.assetService.delete(ctx, Arrays.asList(id));
    }

    /**
     * Delete multiple Assets
     */
    @Allow(Permission.DeleteCatalog)
    public DeletionResponse deleteAssets(List<Long> ids, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.assetService.delete(ctx, ids);
    }
}
