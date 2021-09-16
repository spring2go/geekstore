/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.asset;

import io.geekstore.types.common.Node;
import lombok.Data;

import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Asset implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String name;
    private AssetType type;
    private Integer fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private String source;
    private String preview;
    private Coordinate focalPoint;
}
