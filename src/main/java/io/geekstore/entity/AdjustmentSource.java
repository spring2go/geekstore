/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.entity;

import io.geekstore.types.common.Adjustment;
import io.geekstore.types.common.AdjustmentType;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Getter
@Setter
public abstract class AdjustmentSource extends BaseEntity {
    @TableField(exist = false)
    private AdjustmentType type;

    public String getSourceId() {
        return this.type + ":" +super.getId();
    }

    public static SourceData decodeSourceId(String sourceId) {
        String[] strings = sourceId.split(":");
        SourceData sourceData = new SourceData();
        sourceData.setType(AdjustmentType.valueOf(strings[0]));
        sourceData.setId(Long.parseLong(strings[1]));
        return sourceData;
    }

    public abstract boolean test(Object...args);

    public abstract Adjustment apply(Object... args);

    @Data
    public static class SourceData {
        private AdjustmentType type;
        private Long id;
    }
}
