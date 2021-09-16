/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when an entity cannot be found in the database. i.e. no entity of
 * the given entityName (Product, User ect.) exists with the provided id.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class EntityNotFoundException extends AbstractGraphqlException {
    public EntityNotFoundException(String entityName, Long id) {
        super(String.format("No %s with the id '%d' could be found", entityName, id),
                ErrorCode.ENTITY_NOT_FOUND);
    }
}
