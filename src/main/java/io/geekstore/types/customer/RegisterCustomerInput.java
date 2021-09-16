/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.customer;

import io.geekstore.custom.validator.PhoneNumber;
import io.geekstore.custom.validator.ValidPassword;
import lombok.Data;

import javax.validation.constraints.Email;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class RegisterCustomerInput {
    @Email
    private String emailAddress;
    private String title;
    private String firstName;
    private String lastName;
    @PhoneNumber
    private String phoneNumber;
    @ValidPassword
    private String password;
}
