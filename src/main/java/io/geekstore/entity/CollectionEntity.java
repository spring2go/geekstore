/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.custom.mybatis_plus.ConfigurableOperationListTypeHandler;
import io.geekstore.types.common.ConfigurableOperation;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * A Collection is a grouping of {@link io.geekstore.types.product.Product}s based on
 * various configurable criteria.
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_collection", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionEntity extends BaseEntity {
    private boolean root;
    private Integer position;
    private boolean privateOnly;
    private String name;
    private String description;
    private String slug;
    private Long featuredAssetId;
    @TableField(typeHandler = ConfigurableOperationListTypeHandler.class)
    private List<ConfigurableOperation> filters = new ArrayList<>();
    private Long parentId;
}
