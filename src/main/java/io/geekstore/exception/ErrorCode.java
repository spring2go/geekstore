/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.exception;

/**
 * Created on Nov, 2020 by @author bobo
 */
public enum ErrorCode {
    PERMISSION_DENIED("You are not currently authorized to perform this action!"),

    INVALID_INPUT_PARAMETER("Invalid request input parameters"),

    INVALID_VERIFICATION_TOKEN("Verification token not recognized"),

    EXPIRED_VERIFICATION_TOKEN(
            "Verification token has expired. Use refreshCustomerVerification to send a new token."),

    EXPIRED_PASSWORD_RESET_TOKEN("Password reset token has expired."),

    NOT_RECOGNIZED_IDETIFIER_CHANGE_TOKEN("Identifier change token not recognized"),

    EXPIRED_IDENTIFIER_CHANGE_TOKEN("Identifier change token has expired"),

    INTERNAL_SERVER_ERROR("Unexpected internal server error, please report to our engineering team!"),

    UNAUTHORIZED("The credentials did not match. Please check and try again"),

    NOT_VERIFIED("Email address not verified"),

    FORBIDDEN("You are not currently authorized to perform this action"),

    ENTITY_NOT_FOUND("No entity with the specified id could be found"),

    NO_DATA_FETCHING_ENVIRONMENT("No DataFetchingEnvironment argument in GraphQL resolver"),

    ILLEGAL_OPERATION("The attempted operation is not allowed"),

    BAD_PASSWORD_REST_TOKEN("Password reset token not recognized"),

    NO_ASSET_STORAGE_STRATEGY_CONFIGURED("No asset storage strategy configured"),

    NO_ASSET_PREVIEW_STRATEGY_CONFIGURED("No asset preview strategy configured"),

    USER_INPUT_ERROR("Invalid request input parameters"),

    COUPON_CODE_INVALID("Coupon code is not valid"),

    COUPON_CODE_EXPIRED("Coupon code is expired"),

    COUPON_CODE_LIMIT_REACHED("Coupon code limit has been reached"),

    ORDER_ITEMS_LIMIT_EXCEEDED("Order item limit exceeded");

    public final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
}
