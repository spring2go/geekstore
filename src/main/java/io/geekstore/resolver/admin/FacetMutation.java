/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.FacetEntity;
import io.geekstore.entity.FacetValueEntity;
import io.geekstore.service.FacetService;
import io.geekstore.service.FacetValueService;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import io.geekstore.types.facet.*;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class FacetMutation implements GraphQLMutationResolver {

    private final FacetService facetService;
    private final FacetValueService facetValueService;

    /**
     * Create a new Facet
     */
    @Allow(Permission.CreateCatalog)
    public Facet createFacet(CreateFacetInput input, DataFetchingEnvironment dfe) {
        FacetEntity facetEntity = this.facetService.create(input);
        return BeanMapper.map(facetEntity, Facet.class);
    }

    /**
     * Update an existing Facet
     */
    @Allow(Permission.UpdateCatalog)
    public Facet updateFacet(UpdateFacetInput input, DataFetchingEnvironment dfe) {
        FacetEntity facetEntity = this.facetService.update(input);
        return BeanMapper.map(facetEntity, Facet.class);
    }

    /**
     * Delete an existing Facet
     */
    @Allow(Permission.DeleteCatalog)
    public DeletionResponse deleteFacet(Long id, DataFetchingEnvironment dfe) {
        return this.facetService.delete(id);
    }

    /**
     * Create one or more FacetValues
     */
    @Allow(Permission.CreateCatalog)
    public List<FacetValue> createFacetValues(List<CreateFacetValueInput> input, DataFetchingEnvironment dfe) {
        List<FacetValue> facetValueList = new ArrayList<>();
        input.forEach(createFacetValueInput -> {
            FacetValueEntity facetValueEntity = this.facetValueService.create(createFacetValueInput);
            FacetValue facetValue = BeanMapper.map(facetValueEntity, FacetValue.class);
            facetValueList.add(facetValue);
        });
        return facetValueList;
    }

    /**
     * Update one or more FacetValues
     */
    @Allow(Permission.UpdateCatalog)
    public List<FacetValue> updateFacetValues(List<UpdateFacetValueInput> input, DataFetchingEnvironment dfe) {
        List<FacetValue> facetValueList = new ArrayList<>();
        input.forEach(updateFacetValueInput -> {
            FacetValueEntity facetValueEntity = this.facetValueService.update(updateFacetValueInput);
            FacetValue facetValue = BeanMapper.map(facetValueEntity, FacetValue.class);
            facetValueList.add(facetValue);
        });
        return facetValueList;
    }

    /**
     * Delete one or more FacetValues
     */
    @Allow(Permission.DeleteCatalog)
    public List<DeletionResponse> deleteFacetValues(List<Long> ids, DataFetchingEnvironment dfe) {
        List<DeletionResponse> deletionResponseList = new ArrayList<>();
        ids.forEach(id -> {
            DeletionResponse deletionResponse =
                    this.facetValueService.delete(id);
            deletionResponseList.add(deletionResponse);
        });
        return deletionResponseList;
    }
}
