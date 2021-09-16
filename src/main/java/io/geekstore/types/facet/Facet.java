/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.facet;

import io.geekstore.types.common.Node;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Facet implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String name;
    private String code;
    private Boolean privateOnly; // admin only
    private List<FacetValue> values = new ArrayList<>();
}
