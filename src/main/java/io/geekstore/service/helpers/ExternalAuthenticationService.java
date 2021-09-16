/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import io.geekstore.common.RequestContext;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.*;
import io.geekstore.mapper.*;
import io.geekstore.service.*;
import io.geekstore.service.args.CreateCustomerHistoryEntryArgs;
import io.geekstore.service.helpers.input.CreateAdministratorAndUserInput;
import io.geekstore.service.helpers.input.CreateCustomerAndUserInput;
import io.geekstore.types.history.HistoryEntryType;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This is a helpers importer which exposes methods related to looking up and creating Users based on an
 * external {@link io.geekstore.config.auth.AuthenticationStrategy}
 *
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class ExternalAuthenticationService {
    private final RoleService roleService;
    /**
     * 下面都使用EntityMapper，而不是直接用Service，目的是避免对ConfigService的循环依赖，也就是通过冗余避免循环依赖
     */
    private final CustomerHistoryEntryEntityMapper customerHistoryEntryEntityMapper;
    private final AuthenticationMethodEntityMapper authenticationMethodEntityMapper;
    private final UserEntityMapper userEntityMapper;
    private final UserRoleJoinEntityMapper userRoleJoinEntityMapper;
    private final CustomerEntityMapper customerEntityMapper;
    private final AdministratorEntityMapper administratorEntityMapper;

    /**
     * Looks up a User based on their identifier from an external authentication
     * provider, ensuring this User is associated with a Customer account.
     */
    public UserEntity findCustomerUser(String strategy, String externalIdentifier) {
        UserEntity userEntity = this.findUserEntity(strategy, externalIdentifier);

        if (userEntity != null) {
            // Ensure this User is associated with a Customer
            QueryWrapper<CustomerEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(CustomerEntity::getUserId, userEntity.getId()).isNull(CustomerEntity::getDeletedAt);
            CustomerEntity customerEntity = this.customerEntityMapper.selectOne(queryWrapper);
            if (customerEntity != null) {
                return userEntity;
            }
        }
        return null;
    }

    /**
     * Looks up a User based on their identifier from an external authentication
     * provider, ensuring this User is associated with an Administrator account.
     */
    public UserEntity findAdministratorUser(String strategy, String externalIdentifier) {
        UserEntity userEntity = this.findUserEntity(strategy, externalIdentifier);

        if (userEntity != null) {
            // Ensure this User is associated with an Administrator
            QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(AdministratorEntity::getUserId, userEntity.getId())
                    .isNull(AdministratorEntity::getDeletedAt);
            AdministratorEntity administratorEntity = this.administratorEntityMapper.selectOne(queryWrapper);
            if (administratorEntity != null) {
                return userEntity;
            }
        }
        return null;
    }

    public UserEntity findUserEntity(String strategy, String externalIdentifier) {
        QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthenticationMethodEntity::getStrategy, strategy)
                .eq(AuthenticationMethodEntity::getExternalIdentifier, externalIdentifier);
        AuthenticationMethodEntity authenticationMethodEntity =
                this.authenticationMethodEntityMapper.selectOne(queryWrapper);
        if (authenticationMethodEntity == null) return null;
        QueryWrapper<UserEntity> userEntityQueryWrapper = new QueryWrapper<>();
        userEntityQueryWrapper.lambda().eq(UserEntity::getId, authenticationMethodEntity.getUserId())
                .isNull(UserEntity::getDeletedAt);
        UserEntity userEntity = this.userEntityMapper.selectOne(userEntityQueryWrapper);
        return userEntity;
    }

    /**
     * If a customer has been successfully authenticated by an external authentication provider, yet cannot
     * be found using `findCustomerUser`, then we need to create a new User and
     * Customer record in GeekStore for that user. This method encapsulates that logic as well as additional
     * housekeeping such as adding a record to the Customer's history.
     */
    @Transactional
    public UserEntity createCustomerAndUser(RequestContext ctx, CreateCustomerAndUserInput input) {
        RoleEntity customerRoleEntity = this.roleService.getCustomerRole();

        UserEntity userEntity = new UserEntity();
        userEntity.setIdentifier(input.getEmailAddress());
        userEntity.setVerified(BooleanUtils.toBoolean(input.getVerified()));
        this.userEntityMapper.insert(userEntity);

        UserRoleJoinEntity userRoleJoinEntity = new UserRoleJoinEntity();
        userRoleJoinEntity.setUserId(userEntity.getId());
        userRoleJoinEntity.setRoleId(customerRoleEntity.getId());
        this.userRoleJoinEntityMapper.insert(userRoleJoinEntity);

        AuthenticationMethodEntity authenticationMethodEntity = new AuthenticationMethodEntity();
        authenticationMethodEntity.setExternalIdentifier(input.getExternalIdentifier());
        authenticationMethodEntity.setStrategy(input.getStrategy());
        authenticationMethodEntity.setExternal(true);
        authenticationMethodEntity.setUserId(userEntity.getId());
        this.authenticationMethodEntityMapper.insert(authenticationMethodEntity);

        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setEmailAddress(input.getEmailAddress());
        customerEntity.setFirstName(input.getFirstName());
        customerEntity.setLastName(input.getLastName());
        customerEntity.setUserId(userEntity.getId());
        this.customerEntityMapper.insert(customerEntity);

        createHistoryEntryForCustomer(
                ctx, input.getStrategy(), customerEntity.getId(), HistoryEntryType.CUSTOMER_REGISTERED);

        if (BooleanUtils.isTrue(input.getVerified())) {
            createHistoryEntryForCustomer(
                    ctx, input.getStrategy(), customerEntity.getId(), HistoryEntryType.CUSTOMER_VERIFIED);
        }

        return userEntity;
    }

    private void createHistoryEntryForCustomer(
            RequestContext ctx, String strategy, Long customerId, HistoryEntryType type) {
        CreateCustomerHistoryEntryArgs args = ServiceHelper.buildCreateCustomerHistoryEntryArgs(
                ctx,
                customerId,
                type,
                ImmutableMap.of(HistoryService.KEY_STRATEGY, strategy)
        );
        AdministratorEntity administratorEntity =
                this.getAdministratorFromContext(args.getCtx());
        CustomerHistoryEntryEntity customerHistoryEntryEntity =
                BeanMapper.map(args, CustomerHistoryEntryEntity.class);
        customerHistoryEntryEntity.setAdministratorId(
                administratorEntity == null ? null : administratorEntity.getId());
        customerHistoryEntryEntity.setPrivateOnly(true);
        this.customerHistoryEntryEntityMapper.insert(customerHistoryEntryEntity);
    }

    private AdministratorEntity getAdministratorFromContext(RequestContext ctx) {
        if (ctx.getActiveUserId() == null) return null;

        QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AdministratorEntity::getUserId, ctx.getActiveUserId())
                .isNull(AdministratorEntity::getDeletedAt);
        return this.administratorEntityMapper.selectOne(queryWrapper);
    }

    /**
     * If an administrator has been successfully authenticated by an external authentication provider, yet cannot
     * be found using `findAdministratorUser`, then we need to creat a new User and
     * Administrator record in GeekStore for that user.
     */
    @Transactional
    public UserEntity createAdministratorAndUser(RequestContext ctx, CreateAdministratorAndUserInput input) {
        UserEntity userEntity = new UserEntity();
        userEntity.setIdentifier(input.getIdentifier());
        userEntity.setVerified(true);
        this.userEntityMapper.insert(userEntity);

        input.getRoles().forEach(roleEntity -> {
            UserRoleJoinEntity userRoleJoinEntity = new UserRoleJoinEntity();
            userRoleJoinEntity.setUserId(userEntity.getId());
            userRoleJoinEntity.setRoleId(roleEntity.getId());
            this.userRoleJoinEntityMapper.insert(userRoleJoinEntity);
        });

        AuthenticationMethodEntity authenticationMethodEntity = new AuthenticationMethodEntity();
        authenticationMethodEntity.setExternalIdentifier(input.getExternalIdentifier());
        authenticationMethodEntity.setStrategy(input.getStrategy());
        authenticationMethodEntity.setExternal(true);
        authenticationMethodEntity.setUserId(userEntity.getId());
        this.authenticationMethodEntityMapper.insert(authenticationMethodEntity);

        AdministratorEntity administratorEntity = new AdministratorEntity();
        administratorEntity.setEmailAddress(input.getEmailAddress());
        administratorEntity.setFirstName(input.getFirstName());
        administratorEntity.setLastName(input.getLastName());
        administratorEntity.setUserId(userEntity.getId());
        this.administratorEntityMapper.insert(administratorEntity);

        return userEntity;
    }
}
