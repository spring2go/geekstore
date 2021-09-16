/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers.input;

import io.geekstore.entity.RoleEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class CreateAdministratorAndUserInput {
    private String strategy;
    private String externalIdentifier;
    private String identifier;
    private String emailAddress;
    private String firstName;
    private String lastName;
    private List<RoleEntity> roles = new ArrayList<>();
}
