/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.fsm;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * A simple type-safe finite state machine. This is used internally to
 * control the Order process, ensuring that the state of Orders, Payments and Refunds follows a well-defined behaviour.
 *
 * Created on Dec, 2020 by @author bobo
 */
public class FSM<T, Data> {
    private final T _initialState;
    private T _currentState;
    private StateMachineConfig<T, Data> config;

    public FSM(StateMachineConfig<T, Data> config, T initialState) {
        this.config = config;
        this._currentState = initialState;
        this._initialState = initialState;
    }

    /**
     * Returns the state with which the FSM was initialized.
     */
    public T getInitialState() {
        return this._initialState;
    }

    /**
     * Returns the current state.
     */
    public T getCurrentState() {
        return this._currentState;
    }

    public void transitionTo(T state) {
        this.transitionTo(state, null);
    }

    /**
     * Attempts to transition from the current state to the given state. If this transition is not allowed
     * per the config, then an error will be logged.
     */
    public void transitionTo(T state, Data data) {
        if (this.canTransitionTo(state)) {
            // If the onTransitionStart callback is defined, invoke it. If it returns false,
            // then the transition will be cancelled.
            Object transitionResult = this.config.onTransitionStart(this._currentState, state, data);
            if (transitionResult != null) {
                if (transitionResult instanceof Boolean) {
                    Boolean canTransition = (Boolean) transitionResult;
                    if (BooleanUtils.isFalse(canTransition)) {
                        return;
                    }
                } else if (transitionResult instanceof String) {
                    String errorMessage = (String) transitionResult;
                    this.config.onError(this._currentState, state, errorMessage);
                    return;
                }
            }
            T fromState = this._currentState;
            // All is well, so transition to the new state.
            this._currentState = state;
            // If the onTransitionEnd callback is defined, invoke it.
            this.config.onTransitionEnd(fromState, state, data);
        } else {
            this.config.onError(this._currentState, state, null);
        }
    }

    /**
     * Jumps from the current state to the given state without regard to whether this transition is allowed or not.
     * None of the lifecycle callbacks will be invoked.
     */
    public void jumtTo(T state) {
        this._currentState = state;
    }

    /**
     * Returns an array of state to which the machine may transition from the current state.
     */
    public ImmutableList<T> getNextStates() {
        List<T> nextStates = this.config.getTransitions().get(this._currentState);
        if (nextStates == null) return ImmutableList.of();
        return ImmutableList.copyOf(nextStates);
    }

    /**
     * Returns true if the machine can transtion from its current state to the given state.
     */
    public boolean canTransitionTo(T state) {
        List<T> nextStates = this.config.getTransitions().get(this._currentState);
        if (CollectionUtils.isEmpty(nextStates)) return false;

        return nextStates.contains(state);
    }
}
