/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.custom.mybatis_plus.LongListTypeHandler;
import io.geekstore.types.asset.Coordinate;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Jan, 2021 by @author bobo
 */
@TableName(value = "tb_search_index_item", autoResultMap = true)
@Data
public class SearchIndexItemEntity {
    @TableId(value = "product_variant_id", type = IdType.INPUT)
    private Long productVariantId;
    private Long productId;
    private boolean enabled;
    private String productName;
    private String productVariantName;
    private String description;
    private String slug;
    private String sku;
    private Integer price;
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> facetIds = new ArrayList<>();
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> facetValueIds = new ArrayList<>();
    @TableField(typeHandler = LongListTypeHandler.class)
    private List<Long> collectionIds = new ArrayList<>();
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> collectionSlugs = new ArrayList<>();
    private String productPreview;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Coordinate productPreviewFocalPoint;
    private String productVariantPreview;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Coordinate productVariantPreviewFocalPoint;
    private Long productAssetId;
    private Long productVariantAssetId;
}
