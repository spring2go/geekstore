/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.config.promotion.PromotionAction;
import io.geekstore.config.promotion.PromotionCondition;
import io.geekstore.config.promotion.PromotionOptions;
import io.geekstore.entity.*;
import io.geekstore.exception.CouponCodeExpiredException;
import io.geekstore.exception.CouponCodeInvalidException;
import io.geekstore.exception.CouponCodeLimitException;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.OrderEntityMapper;
import io.geekstore.mapper.OrderPromotionJoinEntityMapper;
import io.geekstore.mapper.PromotionEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.common.*;
import io.geekstore.types.promotion.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class PromotionService {
    private final PromotionOptions promotionOptions;
    private final PromotionEntityMapper promotionEntityMapper;
    private final OrderPromotionJoinEntityMapper orderPromotionJoinEntityMapper;
    private final OrderEntityMapper orderEntityMapper;
    private List<PromotionCondition> availableConditions;
    private List<PromotionAction> availableActions;

    /**
     * All active AdjustmentSources are checked in memory because they are needed
     * every item an order is changed, which will happen often. Caching them means
     * a DB call is not required newly each time.
     */
    private List<PromotionEntity> activePromotions = new ArrayList<>();

    @PostConstruct
    void init() {
        this.availableConditions = promotionOptions.getPromotionConditions();
        this.availableActions = promotionOptions.getPromotionActions();
    }


    public PromotionList findAll(PromotionListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<PromotionEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<PromotionEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        queryWrapper.lambda().isNull(PromotionEntity::getDeletedAt); // 未删除
        IPage<PromotionEntity> promotionEntityPage =
                this.promotionEntityMapper.selectPage(page, queryWrapper);

        PromotionList promotionList = new PromotionList();
        promotionList.setTotalItems((int) promotionEntityPage.getTotal());

        if (CollectionUtils.isEmpty(promotionEntityPage.getRecords())) return promotionList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        promotionEntityPage.getRecords().forEach(promotionEntity -> {
            Promotion promotion = BeanMapper.map(promotionEntity, Promotion.class);
            promotionList.getItems().add(promotion);
        });

        return promotionList;
    }

    private void buildSortOrder(QueryWrapper queryWrapper, PromotionSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getStartsAt(), "starts_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getEndsAt(), "ends_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCouponCode(), "coupon_code");
        QueryHelper.buildOneSortOrder(
                queryWrapper, sortParameter.getPerCustomerUsageLimit(), "per_customer_usage_limit");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getName(), "name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, PromotionFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getStartsAt(), "starts_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getEndsAt(), "ends_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getCouponCode(), "coupon_code");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getName(), "name");
        QueryHelper.buildOneNumberOperatorFilter(
                queryWrapper, filterParameter.getPerCustomerUsageLimit(), "per_customer_usage_limit");
        QueryHelper.buildOneBooleanOperatorFilter(queryWrapper, filterParameter.getEnabled(), "enabled");
    }

    public PromotionEntity findOne(Long adjustmentSourceId) {
        QueryWrapper<PromotionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PromotionEntity::getId, adjustmentSourceId).isNull(PromotionEntity::getDeletedAt);
        return promotionEntityMapper.selectOne(queryWrapper);
    }


    public List<ConfigurableOperationDefinition> getPromotionConditions() {
        return this.availableConditions.stream().map(x -> x.toGraphQLType()).collect(Collectors.toList());
    }

    public List<ConfigurableOperationDefinition> getPromotionActions() {
        return this.availableActions.stream().map(x -> x.toGraphQLType()).collect(Collectors.toList());
    }

    /**
     * Returns all active AdjustmentSources.
     */
    public List<PromotionEntity> getActivePromotions() {
        if (CollectionUtils.isEmpty(activePromotions)) {
            this.updatePromotions();
        }
        return this.activePromotions;
    }

    public PromotionEntity createPromotion(CreatePromotionInput input) {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setName(input.getName());
        promotion.setEnabled(BooleanUtils.toBoolean(input.getEnabled()));
        promotion.setCouponCode(input.getCouponCode());
        promotion.setPerCustomerUsageLimit(input.getPerCustomerUsageLimit());
        promotion.setStartsAt(input.getStartsAt());
        promotion.setEndsAt(input.getEndsAt());
        promotion.setConditions(input.getConditions().stream()
                .map(c -> this.parseOperationArgs(true, c)).collect(Collectors.toList()));
        promotion.setActions(input.getActions().stream()
                .map(a -> this.parseOperationArgs(false, a)).collect(Collectors.toList()));
        promotion.setPriorityScore(this.calculatePriorityScore(input.getConditions(), input.getActions()));
        this.validatePromotionConditions(promotion);
        this.promotionEntityMapper.insert(promotion);
        this.updatePromotions();
        return promotion;
    }

    public PromotionEntity updatePromotion(UpdatePromotionInput input) {
        PromotionEntity promotion =
                ServiceHelper.getEntityOrThrow(this.promotionEntityMapper, PromotionEntity.class, input.getId());
        BeanMapper.patch(input, promotion);
        if (input.getConditions() != null) {
            promotion.setConditions(input.getConditions().stream()
                    .map(c -> this.parseOperationArgs(true, c)).collect(Collectors.toList()));
        }
        if (input.getActions() != null) {
            promotion.setActions(input.getActions().stream()
                    .map(a -> this.parseOperationArgs(false, a)).collect(Collectors.toList()));
        }
        this.validatePromotionConditions(promotion);
        promotion.setPriorityScore(this.calculatePriorityScore(input.getConditions(), input.getActions()));
        this.promotionEntityMapper.updateById(promotion);
        this.updatePromotions();
        return promotion;
    }

    public DeletionResponse softDeletePromotion(Long promotionId) {
        PromotionEntity promotion =
                ServiceHelper.getEntityOrThrow(this.promotionEntityMapper, PromotionEntity.class, promotionId);
        promotion.setDeletedAt(new Date());
        this.promotionEntityMapper.updateById(promotion);

        DeletionResponse response = new DeletionResponse();
        response.setResult(DeletionResult.DELETED);
        return response;
    }

    public PromotionEntity validateCouponCode(String couponCode, Long customerId) {
        QueryWrapper<PromotionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PromotionEntity::getCouponCode, couponCode)
                .eq(PromotionEntity::isEnabled, true)
                .isNull(PromotionEntity::getDeletedAt);
        PromotionEntity promotion = this.promotionEntityMapper.selectOne(queryWrapper);

        if (promotion == null) {
            throw new CouponCodeInvalidException(couponCode);
        }

        if (promotion.getEndsAt() != null && promotion.getEndsAt().getTime() < new Date().getTime()) {
            throw new CouponCodeExpiredException(couponCode);
        }

        if (customerId != null && promotion.getPerCustomerUsageLimit() != null) {
            Integer usageCount = this.countPromotionUsagesForCustomer(promotion.getId(), customerId);
            if (usageCount >= promotion.getPerCustomerUsageLimit()) {
                throw new CouponCodeLimitException(promotion.getPerCustomerUsageLimit());
            }
        }

        return promotion;
    }

    @Transactional
    public OrderEntity addPromotionsToOrder(OrderEntity order) {
        List<Adjustment> allAdjustments = new ArrayList<>();
        for(OrderLineEntity line : order.getLines()) {
            allAdjustments.addAll(line.getAdjustments());
        }
        allAdjustments.addAll(order.getAdjustments());
        Set<Long> promotionIds = allAdjustments.stream()
                .filter(a -> Objects.equals(a.getType(), AdjustmentType.PROMOTION))
                .map(a -> AdjustmentSource.decodeSourceId(a.getAdjustmentSource()).getId())
                .collect(Collectors.toSet());
        for(Long promotionId : promotionIds) {
            OrderPromotionJoinEntity joinEntity = new OrderPromotionJoinEntity();
            joinEntity.setPromotionId(promotionId);
            joinEntity.setOrderId(order.getId());
            this.orderPromotionJoinEntityMapper.insert(joinEntity);
        }
        return order;
    }

    private Integer countPromotionUsagesForCustomer(Long promotionId, Long customerId) {
        QueryWrapper<OrderPromotionJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OrderPromotionJoinEntity::getPromotionId, promotionId);
        List<Long> orderIds = this.orderPromotionJoinEntityMapper.selectList(queryWrapper)
                .stream().map(OrderPromotionJoinEntity::getOrderId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(orderIds)) return 0;

        QueryWrapper<OrderEntity> orderEntityQueryWrapper = new QueryWrapper<>();
        orderEntityQueryWrapper.lambda().in(OrderEntity::getId, orderIds).eq(OrderEntity::getCustomerId, customerId);
        return orderEntityMapper.selectCount(orderEntityQueryWrapper);
    }

    /**
     * Converts the input values of the "create" and "update" mutations into the format expected by the AdjustmentSource
     * entity.
     */
    private ConfigurableOperation parseOperationArgs(
            boolean condition/* conditon or action*/, ConfigurableOperationInput input) {
        if (condition) {
            this.getPromotionConditionByCode(input.getCode());
        } else {
            this.getPromotionActionByCode(input.getCode());
        }
        ConfigurableOperation output = new ConfigurableOperation();
        output.setCode(input.getCode());
        List<ConfigArg> args = input.getArguments().stream().map(arg -> {
            ConfigArg configArg = new ConfigArg();
            configArg.setName(arg.getName());
            configArg.setValue(arg.getValue());
            return configArg;
        }).collect(Collectors.toList());
        output.setArgs(args);
        return output;
    }

    private Integer calculatePriorityScore(
            List<ConfigurableOperationInput> inputConditions, List<ConfigurableOperationInput> inputActions) {
        List<PromotionCondition> conditions = inputConditions == null ? new ArrayList<>() :
                inputConditions.stream()
                        .map(c -> this.getPromotionConditionByCode(c.getCode())).collect(Collectors.toList());
        List<PromotionAction> actions = inputActions == null ? new ArrayList<>() :
                inputActions.stream()
                        .map(a -> this.getPromotionActionByCode(a.getCode())).collect(Collectors.toList());
        int score = 0;
        for(PromotionCondition c : conditions) {
            score += c.getPriorityValue();
        }
        for(PromotionAction a: actions) {
            score += a.getPriorityValue();
        }
        return score;
    }

    private PromotionCondition getPromotionConditionByCode(String code) {
        PromotionCondition match = this.availableConditions
                .stream().filter(condition -> Objects.equals(condition.getCode(), code)).findFirst().orElse(null);
        if (match == null) {
            throw new UserInputException("adjustment operation with code { " + code + " } is not found");
        }
        return match;
    }

    private PromotionAction getPromotionActionByCode(String code) {
        PromotionAction match = this.availableActions
                .stream().filter(action -> Objects.equals(action.getCode(), code)).findFirst().orElse(null);
        if (match == null) {
            throw new UserInputException("adjustment operation with code { " + code + " } is not found");
        }
        return match;
    }

    /**
     * Update the activeSource cache.
     */
    private void updatePromotions() {
        QueryWrapper<PromotionEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PromotionEntity::isEnabled, true);
        activePromotions = this.promotionEntityMapper.selectList(queryWrapper);
    }

    /**
     * Ensure the Promotion has at least one condition or a couponCode specified
     */
    private void validatePromotionConditions(PromotionEntity promotion) {
        if (CollectionUtils.isEmpty(promotion.getConditions()) && StringUtils.isEmpty(promotion.getCouponCode())) {
            throw new UserInputException("A Promotion must have either at least one condition or a coupon code set");
        }
    }
}
