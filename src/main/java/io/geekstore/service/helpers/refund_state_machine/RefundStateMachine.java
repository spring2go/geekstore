/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.refund_state_machine;

import io.geekstore.common.RequestContext;
import io.geekstore.common.fsm.FSM;
import io.geekstore.common.fsm.StateMachineConfig;
import io.geekstore.common.fsm.Transitions;
import io.geekstore.entity.OrderEntity;
import io.geekstore.entity.RefundEntity;
import io.geekstore.exception.IllegalOperationException;
import io.geekstore.service.HistoryService;
import io.geekstore.service.args.CreateOrderHistoryEntryArgs;
import io.geekstore.types.history.HistoryEntryType;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Created on Dec, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
public class RefundStateMachine {
    private final HistoryService historyService;

    private StateMachineConfig<RefundState, RefundTransitionData> config;

    public List<RefundState> getNextStates(RefundEntity refundEntity) {
        FSM<RefundState, RefundTransitionData> fsm = new FSM<>(this.config, refundEntity.getState());
        return fsm.getNextStates();
    }

    public void transition(RequestContext ctx, OrderEntity orderEntity, RefundEntity refundEntity, RefundState state) {
        FSM<RefundState, RefundTransitionData> fsm = new FSM<>(this.config, refundEntity.getState());
        RefundTransitionData data = new RefundTransitionData();
        data.setCtx(ctx);
        data.setOrderEntity(orderEntity);
        data.setRefundEntity(refundEntity);
        fsm.transitionTo(state, data);
        refundEntity.setState(fsm.getCurrentState());
    }

    @PostConstruct
    void initConfig() {
        this.config = new StateMachineConfig<RefundState, RefundTransitionData>() {
            @Override
            public Transitions<RefundState> getTransitions() {
                return RefundStateMachine.this.getRefundStateTransitions();
            }

            @Override
            public Object onTransitionStart(
                    RefundState fromState, RefundState toState, RefundTransitionData refundTransitionData) {
                return true;
            }

            @Override
            public void onTransitionEnd(
                    RefundState fromState, RefundState toState, RefundTransitionData data) {
                CreateOrderHistoryEntryArgs args = new CreateOrderHistoryEntryArgs();
                args.setCtx(data.getCtx());
                args.setOrderId(data.getOrderEntity().getId());
                args.setType(HistoryEntryType.ORDER_REFUND_TRANSITION);
                args.setData(ImmutableMap.of(
                        "refundId", data.getRefundEntity().getId().toString(),
                        "from", fromState.name(),
                        "to", toState.name(),
                        "reason", data.getRefundEntity().getReason()));
                RefundStateMachine.this.historyService.createHistoryEntryForOrder(args);
            }

            @Override
            public void onError(RefundState fromState, RefundState toState, String message) {
                String errorMessage = message;
                if (errorMessage == null) {
                    errorMessage = "Cannot transition Refund from { " + fromState + " } to { " + toState + " }";
                }
                throw new IllegalOperationException(errorMessage);
            }
        };
    }

    private Transitions<RefundState> getRefundStateTransitions() {
        Transitions<RefundState> transitions = new Transitions<>();
        transitions.put(RefundState.Pending, Arrays.asList(RefundState.Settled, RefundState.Failed));
        transitions.put(RefundState.Settled, Arrays.asList());
        transitions.put(RefundState.Failed, Arrays.asList());
        return transitions;
    }
}
