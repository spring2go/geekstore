/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.validator;

import io.geekstore.common.Constant;
import com.google.common.base.Joiner;
import org.passay.*;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * Created on Nov, 2020 by @author bobo
 */
// http://www.passay.org/
// https://www.baeldung.com/registration-password-strength-and-rules
// https://stackabuse.com/spring-custom-password-validation/
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return true; // 可以为空，不空就继续校验。如果需要不空由GraphQL Schema和引擎保证
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(Constant.PASSWORD_LENGTH_MIN, Constant.PASSWORD_LENGTH_MAX),
                new CharacterRule(EnglishCharacterData.Alphabetical, 1), // 至少一个字符(不能全为数字或特殊字符)
                new WhitespaceRule() // 不能有空格
        ));

        RuleResult result = validator.validate(new PasswordData(password));
        if (result.isValid()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                Joiner.on(",").join(validator.getMessages(result)))
                .addConstraintViolation();
        return false;
    }
}
