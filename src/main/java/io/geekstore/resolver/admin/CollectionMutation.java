/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.CollectionEntity;
import io.geekstore.service.CollectionService;
import io.geekstore.types.collection.Collection;
import io.geekstore.types.collection.CreateCollectionInput;
import io.geekstore.types.collection.MoveCollectionInput;
import io.geekstore.types.collection.UpdateCollectionInput;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class CollectionMutation implements GraphQLMutationResolver {

    private final CollectionService collectionService;

    /**
     * Create a new Collection
     */
    @Allow(Permission.CreateCatalog)
    public Collection createCollection(CreateCollectionInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CollectionEntity collectionEntity = this.collectionService.create(ctx, input);
        return BeanMapper.map(collectionEntity, Collection.class);
    }

    /**
     * Update an existing Collection
     */
    @Allow(Permission.UpdateCatalog)
    public Collection updateCollection(UpdateCollectionInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CollectionEntity collectionEntity = this.collectionService.update(ctx, input);
        return BeanMapper.map(collectionEntity, Collection.class);
    }

    /**
     * Delete a Collection and all of its descendents
     */
    @Allow(Permission.DeleteCatalog)
    public DeletionResponse deleteCollection(Long id, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        return this.collectionService.delete(ctx, id);
    }

    /**
     * Move a Collection to a different parent or index
     */
    @Allow(Permission.UpdateCatalog)
    public Collection moveCollection(MoveCollectionInput input, DataFetchingEnvironment dfe) {
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        CollectionEntity collectionEntity = this.collectionService.move(ctx, input);
        return BeanMapper.map(collectionEntity, Collection.class);
    }
}
