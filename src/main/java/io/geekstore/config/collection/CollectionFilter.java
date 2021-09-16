/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.collection;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.common.ConfigurableOperationDef;
import io.geekstore.entity.ProductVariantEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

/**
 * A CollectionFilter defines a rule which can be used to associate ProductVariants with a Collection.
 * The filtering is done by defining the `apply()` function, which receives a Mybatis-Plus QueryWrapper
 * object to which clauses may be added.
 *
 * Created on Nov, 2020 by @author bobo
 */
public abstract class CollectionFilter extends ConfigurableOperationDef {
    public abstract QueryWrapper<ProductVariantEntity> apply(
            ConfigArgValues configArgValues,
            QueryWrapper<ProductVariantEntity> resultQueryWrapper
    );
}
