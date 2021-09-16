/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver;

import io.geekstore.common.ApiType;
import io.geekstore.common.RequestContext;
import io.geekstore.service.SearchService;
import io.geekstore.types.search.FacetValueResult;
import io.geekstore.types.search.SearchResponse;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created on Jan, 2021 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class SearchResponseResolver implements GraphQLResolver<SearchResponse> {

    private final SearchService searchService;

    public List<FacetValueResult> getFacetValues(SearchResponse searchResponse, DataFetchingEnvironment dfe) {
        boolean publicOnly = false;
        RequestContext ctx = RequestContext.fromDataFetchingEnvironment(dfe);
        if (ApiType.SHOP.equals(ctx.getApiType())) {
            publicOnly = true;
        }

        return this.searchService.facetValues(searchResponse.getSearchInput(), publicOnly);
    }
}
