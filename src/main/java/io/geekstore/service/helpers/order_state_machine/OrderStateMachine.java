/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.order_state_machine;

import io.geekstore.common.RequestContext;
import io.geekstore.common.fsm.FSM;
import io.geekstore.common.fsm.StateMachineConfig;
import io.geekstore.common.fsm.Transitions;
import io.geekstore.entity.OrderEntity;
import io.geekstore.exception.IllegalOperationException;
import io.geekstore.mapper.OrderEntityMapper;
import io.geekstore.service.HistoryService;
import io.geekstore.service.PromotionService;
import io.geekstore.service.StockMovementService;
import io.geekstore.service.args.CreateOrderHistoryEntryArgs;
import io.geekstore.service.helpers.OrderHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.service.helpers.payment_state_machine.PaymentState;
import io.geekstore.types.history.HistoryEntryType;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class OrderStateMachine {

    private final OrderHelper orderHelper;
    private final OrderEntityMapper orderEntityMapper;
    private final StockMovementService stockMovementService;
    private final PromotionService promotionService;
    private final HistoryService historyService;

    private final OrderState initialState = OrderState.AddingItems;
    private StateMachineConfig<OrderState, OrderTransitionData> config;

    public OrderState getInitialState() {
        return this.initialState;
    }

    public boolean canTransition(OrderState currentState, OrderState newState) {
        return new FSM(this.config, currentState).canTransitionTo(newState);
    }

    public List<OrderState> getNextStates(OrderEntity orderEntity) {
        FSM<OrderState, OrderTransitionData> fsm = new FSM(this.config, orderEntity.getState());
        return fsm.getNextStates();
    }

    public void transition(RequestContext ctx, OrderEntity orderEntity, OrderState state) {
        FSM<OrderState, OrderTransitionData> fsm = new FSM(this.config, orderEntity.getState());
        OrderTransitionData data = new OrderTransitionData();
        data.setCtx(ctx);
        data.setOrderEntity(orderEntity);
        fsm.transitionTo(state, data);
        orderEntity.setState(fsm.getCurrentState());
    }

    /**
     * Specific business logic to be executed on Order state transitions.
     */
    private Object onTransitionStart(OrderState fromState, OrderState toState, OrderTransitionData data) {
        if (toState == OrderState.ArrangingPayment) {
            if (CollectionUtils.isEmpty(data.getOrderEntity().getLines())) {
                return "error.cannot-transition-to-payment-when-order-is-empty";
            }
            if (data.getOrderEntity().getCustomerId() == null) {
                return "Cannot transition Order to the \"ArrangingPayment\" state without Customer details";
            }
        }
        if (toState == OrderState.PaymentAuthorized &&
                !orderHelper.orderTotalIsCovered(data.getOrderEntity(), PaymentState.Authorized)) {
            return "Cannot transition Order to the \"PaymentAuthorized\" state when the total is " +
                    "not covered by authorized Payments";
        }
        if (toState == OrderState.PaymentSettled &&
                !orderHelper.orderTotalIsCovered(data.getOrderEntity(), PaymentState.Settled)) {
            return "Cannot transition Order to the \"PaymentSettled\" state when the total is not covered by settled Payments";
        }
        if (toState == OrderState.Cancelled &&
                fromState != OrderState.AddingItems && fromState != OrderState.ArrangingPayment) {
            if (!orderHelper.orderItemsAreAllCancelled(data.getOrderEntity())) {
                return "Cannot transition Order to the \"Cancelled\" state unless all OrderItems are cancelled";
            }
        }
        if (toState == OrderState.PartiallyFulfilled) {
            OrderEntity orderWithItems = orderHelper.getOrderWithItems(data.getOrderEntity().getId());
            if (!orderHelper.orderItemArePartiallyFulfilled(orderWithItems)) {
                return "Cannot transition Order to the \"PartiallyFulfilled\" state unless " +
                        "some OrderItems are fulfilled";
            }
        }
        if (toState == OrderState.Fulfilled) {
            OrderEntity orderWithItems = orderHelper.getOrderWithItems(data.getOrderEntity().getId());
            if (!orderHelper.orderItemsAreFulfilled(orderWithItems)) {
                return "Cannot transition Order to the \"Fulfilled\" state unless all OrderItems are fulfilled";
            }
        }
        return null;
    }

    /**
     * Specific business logic to be executed after Order state transition completes.
     */
    private void onTransitionEnd(OrderState fromState, OrderState toState, OrderTransitionData data) {
        if (toState == OrderState.PaymentAuthorized || toState == OrderState.PaymentSettled) {
            data.getOrderEntity().setActive(false);
            data.getOrderEntity().setOrderPlacedAt(new Date());
            this.stockMovementService.createSalesForOrder(data.getOrderEntity());
            this.promotionService.addPromotionsToOrder(data.getOrderEntity());
        }
        if (toState == OrderState.Cancelled) {
            data.getOrderEntity().setActive(false);
        }
        CreateOrderHistoryEntryArgs args = ServiceHelper.buildCreateOrderHistoryEntryArgs(
                data.getCtx(),
                data.getOrderEntity().getId(),
                HistoryEntryType.ORDER_STATE_TRANSITION,
                ImmutableMap.of("from", fromState.name(), "to", toState.name())
        );
        this.historyService.createHistoryEntryForOrder(args);
    }

    @PostConstruct
    void initConfig() {
        this.config = new StateMachineConfig<OrderState, OrderTransitionData>() {
            @Override
            public Transitions<OrderState> getTransitions() {
                return OrderStateMachine.this.getOrderStateTransitions();
            }

            @Override
            public Object onTransitionStart(
                    OrderState fromState, OrderState toState, OrderTransitionData orderTransitionData) {
                return OrderStateMachine.this.onTransitionStart(fromState, toState, orderTransitionData);
            }

            @Override
            public void onTransitionEnd(OrderState fromState, OrderState toState, OrderTransitionData orderTransitionData) {
                OrderStateMachine.this.onTransitionEnd(fromState, toState, orderTransitionData);
            }

            @Override
            public void onError(OrderState fromState, OrderState toState, String message) {
                String errorMessage = message;
                if (errorMessage == null) {
                    errorMessage = "Cannot transition Order from { " + fromState + " } to { " + toState + " }";
                }
                throw new IllegalOperationException(errorMessage);
            }
        };
    }

    public Transitions<OrderState> getOrderStateTransitions() {
        Transitions<OrderState> transitions = new Transitions<>();
        transitions.put(OrderState.AddingItems,
                Arrays.asList(
                        OrderState.ArrangingPayment,
                        OrderState.Cancelled
                )
        );
        transitions.put(OrderState.ArrangingPayment,
                Arrays.asList(
                        OrderState.PaymentAuthorized,
                        OrderState.PaymentSettled,
                        OrderState.AddingItems,
                        OrderState.Cancelled
                )
        );
        transitions.put(OrderState.PaymentAuthorized,
                Arrays.asList(
                        OrderState.PaymentSettled,
                        OrderState.Cancelled
                )
        );
        transitions.put(OrderState.PaymentSettled,
                Arrays.asList(
                        OrderState.PartiallyFulfilled,
                        OrderState.Fulfilled,
                        OrderState.Cancelled
                )
        );
        transitions.put(OrderState.PartiallyFulfilled,
                Arrays.asList(
                        OrderState.Fulfilled,
                        OrderState.PartiallyFulfilled,
                        OrderState.Cancelled
                )
        );
        transitions.put(OrderState.Fulfilled,
                Arrays.asList(
                        OrderState.Cancelled
                )
        );
        transitions.put(OrderState.Cancelled,
                Arrays.asList()
        );
        return transitions;
    }
}
