/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.fsm;

import com.google.common.collect.ImmutableList;

/**
 * Created on Dec, 2020 by @author bobo
 */
public class TestFSMConfig implements StateMachineConfig<TestState, Object> {
    @Override
    public Transitions<TestState> getTransitions() {
        Transitions<TestState> transitions = new Transitions<>();
        transitions.put(TestState.DoorsClosed, ImmutableList.of(TestState.Moving, TestState.DoorsOpen));
        transitions.put(TestState.DoorsOpen, ImmutableList.of(TestState.DoorsClosed));
        transitions.put(TestState.Moving, ImmutableList.of(TestState.DoorsClosed));
        return transitions;
    }

    @Override
    public Object onTransitionStart(TestState fromState, TestState toState, Object o) {
        return null;
    }

    @Override
    public void onTransitionEnd(TestState fromState, TestState toState, Object o) {

    }

    @Override
    public void onError(TestState fromState, TestState toState, String message) {

    }
}
