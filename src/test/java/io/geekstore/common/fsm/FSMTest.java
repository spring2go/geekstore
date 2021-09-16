/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.fsm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created on Dec, 2020 by @author bobo
 */
public class FSMTest {
    @Test
    public void test_initialState_works() {
        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(new TestFSMConfig(), initialState);
        assertThat(fsm.getInitialState()).isEqualTo(initialState);
    }

    @Test
    public void test_getNextStates_works() {
        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(new TestFSMConfig(), initialState);
        assertThat(fsm.getNextStates()).containsExactly(TestState.Moving, TestState.DoorsOpen);
    }

    @Test
    public void test_allow_valid_transitions() {
        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(new TestFSMConfig(), initialState);

        fsm.transitionTo(TestState.Moving);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.Moving);
        fsm.transitionTo(TestState.DoorsClosed);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.DoorsClosed);
        fsm.transitionTo(TestState.DoorsOpen);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.DoorsOpen);
        fsm.transitionTo(TestState.DoorsClosed);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.DoorsClosed);
    }

    @Test
    public void test_does_not_allow_invalid_transitions() {
        TestState initialState = TestState.DoorsOpen;
        FSM<TestState, Object> fsm = new FSM<>(new TestFSMConfig(), initialState);

        fsm.transitionTo(TestState.Moving);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.DoorsOpen);
        fsm.transitionTo(TestState.DoorsClosed);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.DoorsClosed);
        fsm.transitionTo(TestState.Moving);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.Moving);
        fsm.transitionTo(TestState.DoorsOpen);
        assertThat(fsm.getCurrentState()).isEqualTo(TestState.Moving);
    }

    @Test
    public void test_onTransitionStart_is_invoked_before_a_transition_takes_place() {
        TestFSMConfig config = spy(TestFSMConfig.class);

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving, 123);

        verify(config, times(1)).onTransitionStart(initialState, TestState.Moving, 123);
    }

    @Test
    public void test_onTransitionEnd_is_invoked_after_a_transition_takes_place() {
        TestFSMConfig config = spy(TestFSMConfig.class);

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving, 123);

        verify(config, times(1)).onTransitionEnd(initialState, TestState.Moving, 123);
    }

    @Test
    public void test_onTransitionStart_cancels_transition_when_it_returns_false() {
        TestFSMConfig config = spy(TestFSMConfig.class);
        doReturn(false).when(config).onTransitionStart(any(), any(), any());

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving, 123);

        assertThat(fsm.getCurrentState()).isEqualTo(initialState);
    }

    @Test
    public void test_onTransitionStart_cancels_transition_when_it_returns_a_string() {
        TestFSMConfig config = spy(TestFSMConfig.class);
        doReturn("foo").when(config).onTransitionStart(any(), any(), any());

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving);

        assertThat(fsm.getCurrentState()).isEqualTo(initialState);
    }

    @Test
    public void test_onTransitionStart_allows_transition_when_it_returns_true() {
        TestFSMConfig config = spy(TestFSMConfig.class);
        doReturn(true).when(config).onTransitionStart(any(), any(), any());

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving);

        assertThat(fsm.getCurrentState()).isEqualTo(TestState.Moving);
    }

    @Test
    public void test_onTransitionStart_allows_transition_when_it_returns_null() {
        TestFSMConfig config = spy(TestFSMConfig.class);
        doReturn(null).when(config).onTransitionStart(any(), any(), any());

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving);

        assertThat(fsm.getCurrentState()).isEqualTo(TestState.Moving);
    }

    @Test
    public void test_onError_is_invoked_for_invalid_transitions() {
        TestFSMConfig config = spy(TestFSMConfig.class);

        TestState initialState = TestState.DoorsOpen;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving);

        verify(config, times(1)).onError(initialState, TestState.Moving, null);
    }

    @Test
    public void test_onTransitionStart_invokes_onError_if_it_returns_a_string() {
        TestFSMConfig config = spy(TestFSMConfig.class);
        doReturn("error").when(config).onTransitionStart(any(), any(), any());

        TestState initialState = TestState.DoorsClosed;
        FSM<TestState, Object> fsm = new FSM<>(config, initialState);

        fsm.transitionTo(TestState.Moving);

        verify(config, times(1)).onError(initialState, TestState.Moving, "error");
    }
}
