/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.customer;

import io.geekstore.types.address.Address;
import io.geekstore.types.common.Node;
import io.geekstore.types.history.HistoryEntryList;
import io.geekstore.types.order.OrderList;
import io.geekstore.types.user.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Data
public class Customer implements Node {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
    private String title;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String emailAddress;
    private List<Address> addresses = new ArrayList<>();
    private OrderList orders;
    private User user;
    private Long userId; // 该字段仅内部使用，GraphQL对外不可见
    private List<CustomerGroup> groups = new ArrayList<>(); // 该字段只有Admin可见
    private HistoryEntryList history; // 该字段只有Admin可见
}
