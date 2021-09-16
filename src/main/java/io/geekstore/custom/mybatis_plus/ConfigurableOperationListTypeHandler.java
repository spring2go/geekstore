/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.mybatis_plus;

import io.geekstore.types.common.ConfigurableOperation;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.IOException;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Slf4j
@MappedTypes({List.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ConfigurableOperationListTypeHandler extends AbstractJsonTypeHandler<List<ConfigurableOperation>>  {
    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected List<ConfigurableOperation> parse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<ConfigurableOperation>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String toJson(List<ConfigurableOperation> obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
