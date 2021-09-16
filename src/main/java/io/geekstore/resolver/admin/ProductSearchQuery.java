/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.custom.security.Allow;
import io.geekstore.service.SearchService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.common.SearchInput;
import io.geekstore.types.search.SearchResponse;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Jan, 2021 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class ProductSearchQuery implements GraphQLQueryResolver {

    private final SearchService searchService;

    @Allow(Permission.ReadCatalog)
    public SearchResponse searchByAdmin(SearchInput input, DataFetchingEnvironment dfe) {
        SearchResponse searchResponse = this.searchService.search(input);
        searchResponse.setSearchInput(input);
        return searchResponse;
    }
}
