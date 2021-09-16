/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.graphql;

import io.geekstore.exception.UserInputException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import java.util.Set;

/**
 * 输入参数校验切面
 *
 * 参考：
 * https://www.mirkosertic.de/blog/2013/06/method-validation-with-jsr303-and-aspectj/
 *
 * Created on Nov, 2020 by @author bobo
 */
@Aspect
@Component
@Order(2) // 安全校验之后执行
@Slf4j
public class GraphQLValidationAspect {
    @Autowired
    private Validator validator;

    /**
     * validate graphQLResolver method inputs
     */
    @Around("allGraphQLMutationOrQueryResolverMethods() && isDefinedInApplication() && " +
            "(isMethodParameterAnnotatedWithValid() || isMethodParameterAnnotatedWithConstraints() || " +
            "isMethodParameterAnnotatedWithCustomConstraints())")
    public Object validate(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        ExecutableValidator executableValidator = validator.forExecutables();
        Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(
                joinPoint.getTarget(),
                methodSignature.getMethod(),
                joinPoint.getArgs()
        );

        if (violations.size() == 0) return joinPoint.proceed();

        UserInputException userInputException = new UserInputException();
        violations.forEach(violation -> {
            userInputException.getExtensions().put(violation.getPropertyPath().toString(), violation.getMessage());
        });
        throw userInputException;
    }

    @Pointcut("allGraphQLMutationResolverMethods() || allGraphQLQueryResolverMethods()")
    private void allGraphQLMutationOrQueryResolverMethods() {
    }

    @Pointcut("target(graphql.kickstart.tools.GraphQLMutationResolver)")
    private void allGraphQLMutationResolverMethods() {
    }

    @Pointcut("target(graphql.kickstart.tools.GraphQLQueryResolver)")
    private void allGraphQLQueryResolverMethods() {
    }

    /**
     * Matches all beans in io.geekstore package
     * resolvers must be in this package (subpackages)
     */
    @Pointcut("within(io.geekstore..*)")
    private void isDefinedInApplication() {
    }

    /**
     * Any method parameter annotated with @Valid
     */
    @Pointcut("execution(public * * (.., @javax.validation.Valid (*), ..))")
    private void isMethodParameterAnnotatedWithValid() {
    }

    /**
     * Any method parameter annotated with validation constraints annotation
     */
    @Pointcut("execution(public * * (.., @javax.validation.constraints.* (*), ..))")
    private void isMethodParameterAnnotatedWithConstraints() {
    }

    /**
     * Any method parameter annotated with custom validation constraints annotation
     */
    @Pointcut("execution(public * * (.., @io.geekstore.custom.validator.* (*), ..))")
    private void isMethodParameterAnnotatedWithCustomConstraints() {
    }
}
