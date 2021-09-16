/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * This error should be thrown when user input is not as expected.
 *
 * Created on Nov, 2020 by @author bobo
 */
public class UserInputException extends AbstractGraphqlException {
    public UserInputException(String errorMessage) {
        super(errorMessage, ErrorCode.USER_INPUT_ERROR);
    }

    public UserInputException() {
        super(ErrorCode.USER_INPUT_ERROR);
    }
}
