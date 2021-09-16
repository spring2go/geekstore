/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.types.asset.AssetType;
import io.geekstore.types.asset.Coordinate;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An Asset represents a file such as an image which can be associated with certain other entities
 * such as Products.
 *
 * Created on Nov, 2020 by @author bobo
 */

@TableName(value = "tb_asset", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetEntity extends BaseEntity {
    private String name;
    private AssetType type;
    private String mimeType;
    private Integer width = 0;
    private Integer height = 0;
    private Integer fileSize;
    private String source;
    private String preview;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Coordinate focalPoint;
}
