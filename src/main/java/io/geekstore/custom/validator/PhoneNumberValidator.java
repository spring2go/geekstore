/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    private static final Pattern phoneNumberPattern = Pattern.compile("^1(3|4|5|7|8)\\d{9}$");

    @Override
    public boolean isValid(String phoneField, ConstraintValidatorContext context) {
        if (phoneField == null) return true; // 可以为空
        return phoneNumberPattern.matcher(phoneField).matches();
    }
}
