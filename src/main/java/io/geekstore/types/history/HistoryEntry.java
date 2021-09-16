/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.history;

import io.geekstore.types.administrator.Administrator;
import io.geekstore.types.common.Node;
import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class HistoryEntry implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private Boolean privateOnly;
    private HistoryEntryType type;
    private Administrator administrator;
    private Long administratorId; // 内部使用字段，GraphQL不可见
    private Map<String, String> data = new HashMap<>();
}
