/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.collection;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
@AllArgsConstructor
public class CollectionBreadcrumb {
    private Long id;
    private String name;
    private String slug;
}
