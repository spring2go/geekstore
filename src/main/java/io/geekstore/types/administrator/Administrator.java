/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.administrator;

import io.geekstore.types.common.Node;
import io.geekstore.types.user.User;
import lombok.Data;

import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Administrator implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private User user;
}
