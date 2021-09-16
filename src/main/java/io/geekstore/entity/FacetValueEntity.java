/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A particular value of {@link io.geekstore.types.facet.Facet}
 *
 * Created on Nov, 2020 by @author bobo
 */
@TableName(value = "tb_facet_value")
@Data
@EqualsAndHashCode(callSuper = true)
public class FacetValueEntity extends BaseEntity {
    private String name;
    private String code;
    private Long facetId;
}
