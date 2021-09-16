/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.common;

/**
 * Created on Nov, 2020 by @author bobo
 */
public enum DeletionResult {
    /**
     * The entity was successfully deleted
     */
    DELETED,
    /**
     * Deletion did not take place, reason given in message
     */
    NOT_DELETED
}
