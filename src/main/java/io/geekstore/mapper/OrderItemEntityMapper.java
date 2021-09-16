/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.mapper;

import io.geekstore.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Mapper
public interface OrderItemEntityMapper extends BaseMapper<OrderItemEntity> {
}
