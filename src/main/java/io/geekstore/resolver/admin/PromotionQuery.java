/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.PromotionEntity;
import io.geekstore.service.PromotionService;
import io.geekstore.types.common.ConfigurableOperationDefinition;
import io.geekstore.types.common.Permission;
import io.geekstore.types.promotion.Promotion;
import io.geekstore.types.promotion.PromotionList;
import io.geekstore.types.promotion.PromotionListOptions;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class PromotionQuery implements GraphQLQueryResolver {

    private final PromotionService promotionService;

    @Allow(Permission.ReadPromotion)
    public Promotion promotion(Long id, DataFetchingEnvironment dfe) {
        PromotionEntity promotionEntity = this.promotionService.findOne(id);
        if (promotionEntity == null) return null;
        return BeanMapper.map(promotionEntity, Promotion.class);
    }

    @Allow(Permission.ReadPromotion)
    public PromotionList promotions(PromotionListOptions options, DataFetchingEnvironment dfe) {
        return this.promotionService.findAll(options);
    }
    @Allow(Permission.ReadPromotion)
    public List<ConfigurableOperationDefinition> promotionConditions(DataFetchingEnvironment dfe) {
        return this.promotionService.getPromotionConditions();
    }

    @Allow(Permission.ReadPromotion)
    public List<ConfigurableOperationDefinition> promotionActions(DataFetchingEnvironment dfe) {
        return this.promotionService.getPromotionActions();
    }
}
