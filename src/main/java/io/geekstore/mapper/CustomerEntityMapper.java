/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.mapper;

import io.geekstore.entity.CustomerEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Mapper
public interface CustomerEntityMapper extends BaseMapper<CustomerEntity> {
}
