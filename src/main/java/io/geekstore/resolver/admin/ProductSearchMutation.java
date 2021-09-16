/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.custom.security.Allow;
import io.geekstore.service.SearchService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.search.SearchReindexResponse;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Jan, 2021 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ProductSearchMutation implements GraphQLMutationResolver {

    private final SearchService searchService;

    @Allow(Permission.UpdateCatalog)
    public SearchReindexResponse reindex(DataFetchingEnvironment dfe) {
        Boolean result = searchService.reindex();
        SearchReindexResponse reindexResponse = new SearchReindexResponse();
        reindexResponse.setSuccess(result);
        return reindexResponse;
    }
}
