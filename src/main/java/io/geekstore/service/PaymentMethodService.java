/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.ConfigArgValues;
import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.config.payment_method.CreatePaymentResult;
import io.geekstore.config.payment_method.CreateRefundResult;
import io.geekstore.config.payment_method.PaymentMethodHandler;
import io.geekstore.config.payment_method.SettlePaymentResult;
import io.geekstore.entity.*;
import io.geekstore.eventbus.events.PaymentStateTransitionEvent;
import io.geekstore.eventbus.events.RefundStateTransitionEvent;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.OrderItemEntityMapper;
import io.geekstore.mapper.PaymentEntityMapper;
import io.geekstore.mapper.PaymentMethodEntityMapper;
import io.geekstore.mapper.RefundEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.service.helpers.payment_state_machine.PaymentStateMachine;
import io.geekstore.service.helpers.refund_state_machine.RefundState;
import io.geekstore.service.helpers.refund_state_machine.RefundStateMachine;
import io.geekstore.types.common.ConfigArg;
import io.geekstore.types.common.ConfigArgDefinition;
import io.geekstore.types.order.RefundOrderInput;
import io.geekstore.types.payment.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.eventbus.EventBus;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class PaymentMethodService {
    private final ConfigService configService;
    private final PaymentStateMachine paymentStateMachine;
    private final RefundStateMachine refundStateMachine;
    private final PaymentMethodEntityMapper paymentMethodEntityMapper;
    private final PaymentEntityMapper paymentEntityMapper;
    private final RefundEntityMapper refundEntityMapper;
    private final OrderItemEntityMapper orderItemEntityMapper;
    private final EventBus eventBus;

    public PaymentMethodList findAll(PaymentMethodListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<PaymentMethodEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<PaymentMethodEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<PaymentMethodEntity> paymentMethodEntityPage =
                this.paymentMethodEntityMapper.selectPage(page, queryWrapper);

        PaymentMethodList paymentMethodList = new PaymentMethodList();
        paymentMethodList.setTotalItems((int) paymentMethodEntityPage.getTotal());

        if (CollectionUtils.isEmpty(paymentMethodEntityPage.getRecords())) return paymentMethodList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        paymentMethodEntityPage.getRecords().forEach(paymentMethodEntity -> {
            PaymentMethod paymentMethod = BeanMapper.map(paymentMethodEntity, PaymentMethod.class);
            paymentMethodList.getItems().add(paymentMethod);
        });

        return paymentMethodList;
    }

    @PostConstruct
    void initPaymentMethods() {
        this.ensurePaymentMethodsExist();
    }

    private void buildSortOrder(QueryWrapper queryWrapper, PaymentMethodSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCode(), "code");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, PaymentMethodFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getCode(), "code");
        QueryHelper.buildOneBooleanOperatorFilter(queryWrapper, filterParameter.getEnabled(), "enabled");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    public PaymentMethodEntity findOne(Long paymentMethodId) {
        return this.paymentMethodEntityMapper.selectById(paymentMethodId);
    }

    public PaymentMethodEntity update(UpdatePaymentMethodInput input) {
        PaymentMethodEntity paymentMethod =
                ServiceHelper.getEntityOrThrow(this.paymentMethodEntityMapper, PaymentMethodEntity.class, input.getId());
        BeanMapper.patch(input, paymentMethod);
        if (!CollectionUtils.isEmpty(input.getConfigArgs())) {
            PaymentMethodHandler handler = this.configService.getPaymentOptions().getPaymentMethodHandlers().stream()
                    .filter(h -> Objects.equals(h.getCode(), paymentMethod.getCode()))
                    .findFirst().orElse(null);
            if (handler != null) {
                List<ConfigArg> configArgs = input.getConfigArgs().stream()
                        .filter(configArgInput -> handler.getArgSpec().containsKey(configArgInput.getName()))
                        .map(configArgInput -> {
                            ConfigArg configArg = new ConfigArg();
                            configArg.setName(configArgInput.getName());
                            configArg.setValue(configArgInput.getValue());
                            return configArg;
                        }).collect(Collectors.toList());
                paymentMethod.setConfigArgs(configArgs);
            }
        }
        this.paymentMethodEntityMapper.updateById(paymentMethod);
        return paymentMethod;
    }

    @Transactional
    public PaymentEntity createPayment(
            RequestContext ctx, OrderEntity order, String method, Map<String, String> metadata) {
        Pair<PaymentMethodEntity, PaymentMethodHandler> pair = this.getMethodAndHandler(method);
        PaymentMethodEntity paymentMethod = pair.getLeft();
        PaymentMethodHandler handler = pair.getRight();
        CreatePaymentResult result =
                handler.createPayment(order, new ConfigArgValues(paymentMethod.getConfigArgs()), metadata);
        PaymentState initialState = PaymentState.Created;
        PaymentEntity payment = new PaymentEntity();
        payment.setState(initialState);
        payment.setAmount(result.getAmount());
        payment.setTransactionId(result.getTransactionId());
        payment.setErrorMessage(result.getErrorMessage());
        payment.setOrderId(order.getId());
        payment.setMethod(method);
        payment.setMetadata(result.getMetadata());
        this.paymentEntityMapper.insert(payment);
        this.paymentStateMachine.transition(ctx, order, payment, result.getState());
        this.paymentEntityMapper.updateById(payment);

        PaymentStateTransitionEvent event = new PaymentStateTransitionEvent(
                initialState,
                result.getState(),
                ctx,
                payment,
                order
        );
        this.eventBus.post(event);

        return payment;
    }

    public SettlePaymentResult settlePayment(PaymentEntity payment, OrderEntity order) {
        Pair<PaymentMethodEntity, PaymentMethodHandler> pair = this.getMethodAndHandler(payment.getMethod());
        PaymentMethodEntity paymentMethod = pair.getLeft();
        PaymentMethodHandler handler = pair.getRight();
        return handler.settlePayment(order, payment, new ConfigArgValues(paymentMethod.getConfigArgs()));
    }

    @Transactional
    public RefundEntity createRefund(RequestContext ctx,
            RefundOrderInput input, OrderEntity order, List<OrderItemEntity> items, PaymentEntity payment) {
        Pair<PaymentMethodEntity, PaymentMethodHandler> pair = this.getMethodAndHandler(payment.getMethod());
        PaymentMethodEntity paymentMethod = pair.getLeft();
        PaymentMethodHandler handler = pair.getRight();
        Integer itemAmount = 0;
        for(OrderItemEntity item : items) {
            itemAmount += item.getUnitPrice();
        }
        Integer refundAmount = itemAmount + input.getShipping() + input.getAdjustment();
        RefundEntity refund = new RefundEntity();
        refund.setPaymentId(payment.getId());
        refund.setItems(itemAmount);
        refund.setReason(input.getReason());
        refund.setAdjustment(input.getAdjustment());
        refund.setShipping(input.getShipping());
        refund.setTotal(refundAmount);
        refund.setMethod(payment.getMethod());
        refund.setState(RefundState.Pending);
        CreateRefundResult createRefundResult = handler.createRefund(
                input,
                refundAmount,
                order,
                payment,
                new ConfigArgValues(paymentMethod.getConfigArgs())
        );
        if (createRefundResult != null) {
            refund.setTransactionId(
                    createRefundResult.getTransactionId() == null ? "" : createRefundResult.getTransactionId());
            refund.setMetadata(createRefundResult.getMetadata());
        }
        this.refundEntityMapper.insert(refund);
        for(OrderItemEntity item : items) {
            item.setRefundId(refund.getId());
            this.orderItemEntityMapper.updateById(item);
        }
        if (createRefundResult != null) {
            RefundState fromState = refund.getState();
            this.refundStateMachine.transition(ctx, order, refund, createRefundResult.getState());
            this.refundEntityMapper.updateById(refund);
            RefundStateTransitionEvent event = new RefundStateTransitionEvent(
                    fromState,
                    createRefundResult.getState(),
                    ctx,
                    refund,
                    order
            );
            this.eventBus.post(event);
        }
        return refund;
    }

    public PaymentMethodHandler getPaymentMethodHandler(String code) {
        PaymentMethodHandler handler = this.configService.getPaymentOptions().getPaymentMethodHandlers().stream()
                .filter(h -> Objects.equals(h.getCode(), code)).findFirst().orElse(null);
        if (handler == null) {
            throw new UserInputException("No payment handler with code { " + code + " }");
        }
        return handler;
    }

    public Pair<PaymentMethodEntity, PaymentMethodHandler> getMethodAndHandler(String method) {
        QueryWrapper<PaymentMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PaymentMethodEntity::getCode, method).eq(PaymentMethodEntity::isEnabled, true);
        PaymentMethodEntity paymentMethod = this.paymentMethodEntityMapper.selectOne(queryWrapper);
        if (paymentMethod == null) {
            throw new UserInputException("Payment method { " + method + " } not found");
        }
        PaymentMethodHandler handler = this.getPaymentMethodHandler(paymentMethod.getCode());
        return Pair.of(paymentMethod, handler);
    }

    private void ensurePaymentMethodsExist() {
        List<PaymentMethodHandler> paymentMethodHandlers =
                this.configService.getPaymentOptions().getPaymentMethodHandlers();
        List<PaymentMethodEntity> existingPaymentMethods = paymentMethodEntityMapper.selectList(null);
        List<PaymentMethodHandler> toCreate = paymentMethodHandlers.stream()
                .filter(h -> !existingPaymentMethods.stream().anyMatch(pm -> Objects.equals(pm.getCode(), h.getCode())))
                .collect(Collectors.toList());
        List<PaymentMethodEntity> toRemove = existingPaymentMethods.stream()
                .filter(h -> !paymentMethodHandlers.stream().anyMatch(pm -> Objects.equals(pm.getCode(), h.getCode())))
                .collect(Collectors.toList());
        List<PaymentMethodEntity> toUpdate = existingPaymentMethods.stream()
                .filter(h -> !toCreate.stream().anyMatch(x -> Objects.equals(x.getCode(), h.getCode()) &&
                        !toRemove.stream().anyMatch(y -> Objects.equals(y.getCode(), h.getCode()))))
                .collect(Collectors.toList());

        for(PaymentMethodEntity paymentMethod : toUpdate) {
            PaymentMethodHandler handler = paymentMethodHandlers.stream()
                    .filter(h -> Objects.equals(h.getCode(), paymentMethod.getCode())).findFirst().orElse(null);
            if (handler == null) continue;
            paymentMethod.setConfigArgs(this.buildConfigArgsList(handler, paymentMethod.getConfigArgs()));
            this.paymentMethodEntityMapper.updateById(paymentMethod);
        }

        for(PaymentMethodHandler handler : toCreate) {
            PaymentMethodEntity paymentMethod = existingPaymentMethods.stream()
                    .filter(pm -> Objects.equals(pm.getCode(), handler.getCode())).findFirst().orElse(null);

            if (paymentMethod == null) {
                paymentMethod = new PaymentMethodEntity();
                paymentMethod.setCode(handler.getCode());
                paymentMethod.setEnabled(true);
            }
            paymentMethod.setConfigArgs(this.buildConfigArgsList(handler, paymentMethod.getConfigArgs()));
            if (paymentMethod.getId() == null) {
                this.paymentMethodEntityMapper.insert(paymentMethod);
            } else {
                this.paymentMethodEntityMapper.updateById(paymentMethod);
            }
        }
        if (!CollectionUtils.isEmpty(toRemove)) {
            List<Long> toRemoveIds = toRemove.stream().map(PaymentMethodEntity::getId).collect(Collectors.toList());
            this.paymentMethodEntityMapper.deleteBatchIds(toRemoveIds);
        }
    }

    private List<ConfigArg> buildConfigArgsList(
            PaymentMethodHandler handler,
            List<ConfigArg> existingConfigArgs) {
        List<ConfigArg> configArgs = new ArrayList<>();
        for(String name : handler.getArgSpec().keySet()) {
            ConfigArgDefinition def = handler.getArgSpec().get(name);
            if (!existingConfigArgs.stream().anyMatch(ca -> Objects.equals(ca.getName(), name))) {
                ConfigArg configArg = new ConfigArg();
                configArg.setName(name);
                configArg.setValue(this.getDefaultValue(def.getType()));
                configArgs.add(configArg);
            }
        }
//        configArgs = configArgs.stream().filter(ca -> handler.getArgSpec().containsKey(ca.getName()))
//                .collect(Collectors.toList());
        List<ConfigArg> result = new ArrayList<>(existingConfigArgs);
        result.addAll(configArgs);
        return result;
    }

    private String getDefaultValue(String configArgType) {
        switch (configArgType) {
            case "string":
            case "ID":
                return "";
            case "boolean":
                return "false";
            case "int":
            case "float":
                return "0";
            case "datetime":
                return String.valueOf(new Date().getTime());
            default:
                assert false : "invalid type { " + configArgType + " }";
                return "";
        }
    }
}
