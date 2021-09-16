/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common;

import io.geekstore.exception.UserInputException;
import io.geekstore.types.common.ConfigArg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class ConfigArgValues {
    private Map<String, String> argsMap = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public ConfigArgValues(List<ConfigArg> args) {
        if (!CollectionUtils.isEmpty(args)) {
            args.forEach(arg -> argsMap.put(arg.getName(), arg.getValue()));
        }
    }

    public String getString(String name) {
        return argsMap.get(name);
    }

    public Float getFloat(String name) {
        String value = argsMap.get(name);
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException nfe) {
            throw new UserInputException("Fail to parse float with string '" + value + "'");
        }
    }

    public Integer getInteger(String name) {
        String value = argsMap.get(name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            throw new UserInputException("Fail to parse int with string '" + value + "'");
        }
    }

    public Long getId(String name) {
        String value = argsMap.get(name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            throw new UserInputException("Fail to parse long with string '" + value + "'");
        }
    }

    public Boolean getBoolean(String name) {
        String value = argsMap.get(name);
        return BooleanUtils.toBoolean(value);
    }

    public List<Long> getIdList(String name) {
        String jsonString = argsMap.get(name);
        if (jsonString == null) return null;

        // 参考
        // https://stackoverflow.com/questions/6349421/how-to-use-jackson-to-deserialise-an-array-of-objects
        try {
            List<Long> idList = objectMapper.readValue(jsonString,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, Long.class));
            return idList;
        } catch (JsonProcessingException e) {
            throw new UserInputException("Fail to parse ID list, jsonString = '" + jsonString + "'");
        }
    }
}
