/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.custom.security.Allow;
import io.geekstore.entity.PromotionEntity;
import io.geekstore.service.PromotionService;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.Permission;
import io.geekstore.types.promotion.CreatePromotionInput;
import io.geekstore.types.promotion.Promotion;
import io.geekstore.types.promotion.UpdatePromotionInput;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class PromotionMutation implements GraphQLMutationResolver {

    private final PromotionService promotionService;

    @Allow(Permission.CreatePromotion)
    public Promotion createPromotion(CreatePromotionInput input, DataFetchingEnvironment dfe) {
        PromotionEntity promotionEntity = this.promotionService.createPromotion(input);
        return BeanMapper.map(promotionEntity, Promotion.class);
    }

    @Allow(Permission.UpdatePromotion)
    public Promotion updatePromotion(UpdatePromotionInput input, DataFetchingEnvironment dfe) {
        PromotionEntity promotionEntity = this.promotionService.updatePromotion(input);
        return BeanMapper.map(promotionEntity, Promotion.class);
    }

    @Allow(Permission.DeletePromotion)
    public DeletionResponse deletePromotion(Long id, DataFetchingEnvironment dfe) {
        return this.promotionService.softDeletePromotion(id);
    }
}
