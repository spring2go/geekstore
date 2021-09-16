/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config.collection;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.entity.ProductVariantEntity;
import io.geekstore.exception.UserInputException;
import io.geekstore.types.common.ConfigArgDefinition;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class VariantNameCollectionFilter extends CollectionFilter {

    final static String CONFIG_NAME_OPERATOR = "operator";
    final static String CONFIG_NAME_TERM = "term";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getCode() {
        return "variant-name-filter";
    }

    @Override
    public Map<String, ConfigArgDefinition> getArgSpec() {
        Map<String, ConfigArgDefinition> args = new HashMap<>();

        ConfigArgDefinition configArgDefinition = new ConfigArgDefinition();
        configArgDefinition.setType("string");
        configArgDefinition.getUi().put("component", "select-form-input");

        ArrayNode arrayNode = objectMapper.createArrayNode();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("value", "startsWith");
        arrayNode.add(objectNode);
        objectNode = objectMapper.createObjectNode();
        objectNode.put("value", "endsWith");
        arrayNode.add(objectNode);
        objectNode.put("value", "contains");
        arrayNode.add(objectNode);
        objectNode.put("value", "doesNotContain");
        arrayNode.add(objectNode);
        configArgDefinition.getUi().put("options", arrayNode);

        args.put(CONFIG_NAME_OPERATOR, configArgDefinition);

        configArgDefinition = new ConfigArgDefinition();
        configArgDefinition.setType("string");
        args.put(CONFIG_NAME_TERM, configArgDefinition);

        return args;
    }

    @Override
    public String getDescription() {
        return "Filter by ProductVariant name";
    }

    @Override
    public QueryWrapper<ProductVariantEntity> apply(
            ConfigArgValues configArgValues,
            QueryWrapper<ProductVariantEntity> resultQueryWrapper) {
        String operator = configArgValues.getString(CONFIG_NAME_OPERATOR);
        String term = configArgValues.getString(CONFIG_NAME_TERM);
        if (StringUtils.isEmpty(operator) || StringUtils.isEmpty(term)) {
            throw new UserInputException("Either operator or term is empty");
        };
        switch (operator) {
            case "contains":
                resultQueryWrapper.like("lower(name)", term.toLowerCase());
                break;
            case "doesNotContain":
                resultQueryWrapper.notLike("lower(name)", term.toLowerCase());
                break;
            case "startsWith":
                resultQueryWrapper.likeRight("lower(name)", term.toLowerCase());
                break;
            case "endsWith":
                resultQueryWrapper.likeLeft("lower(name)", term.toLowerCase());
                break;
            default:
                throw new UserInputException("'" + operator + "' is not a valid operator");
        }
        return resultQueryWrapper;
    }

}
