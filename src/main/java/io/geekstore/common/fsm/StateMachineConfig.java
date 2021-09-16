/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.fsm;

/**
 * The config object used to instantiate a new {@link FSM} instance.
 *
 * Created on Dec, 2020 by @author bobo
 */
public interface StateMachineConfig<T, Data> {
    /**
     * Defines the available states of the state machine as well as the premitted
     * transitions from one state to another.
     */
    Transitions<T> getTransitions();

    /**
     * Called before a transition takes place. If the function resolves to `false` or a string, then the transition
     * will be cancelled. In the case of a string, the string (error message) will be forwarded to the onError handler.
     *
     * If this function returns a value resolving to `true` or `void` (no return value), then the transition
     * will be permitted.
     */
    Object onTransitionStart(T fromState, T toState, Data data);

    /**
     * Called after a transition has taken place.
     */
    void onTransitionEnd(T fromState, T toState, Data data);


    /**
     * Called when a transition is prevented and the `onTransitionStart` handler has returned an error message.
     */
    void onError(T fromState, T toState, String message);
}
