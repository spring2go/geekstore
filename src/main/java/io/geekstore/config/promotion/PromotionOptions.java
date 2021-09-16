/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.promotion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Configures the Conditions and Actions available when creating Promotions.
 *
 * Created on Dec, 2020 by @author bobo
 */
@Getter
@RequiredArgsConstructor
public class PromotionOptions {
    /**
     * A list of conditions which can be used to construct Promotions
     */
    private final List<PromotionCondition> promotionConditions;
    /**
     * A list of actions which can be used to construct Promotions
     */
    private final List<PromotionAction> promotionActions;
}
