/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.FacetEntity;
import io.geekstore.service.FacetService;
import io.geekstore.types.common.Permission;
import io.geekstore.types.facet.Facet;
import io.geekstore.types.facet.FacetList;
import io.geekstore.types.facet.FacetListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class FacetQuery implements GraphQLQueryResolver {
    private final FacetService facetService;

    @Allow(Permission.ReadCatalog)
    public FacetList facets(FacetListOptions options, DataFetchingEnvironment dfe) {
        return this.facetService.findAll(options);
    }

    @Allow(Permission.ReadCatalog)
    public Facet facet(Long id, DataFetchingEnvironment dfe) {
        FacetEntity facetEntity = this.facetService.findOne(id);
        if (facetEntity == null) return null;
        return BeanMapper.map(facetEntity, Facet.class);
    }
}
