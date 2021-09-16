/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.AuthenticationMethodEntity;
import io.geekstore.entity.RoleEntity;
import io.geekstore.entity.UserEntity;
import io.geekstore.entity.UserRoleJoinEntity;
import io.geekstore.exception.*;
import io.geekstore.mapper.AuthenticationMethodEntityMapper;
import io.geekstore.mapper.RoleEntityMapper;
import io.geekstore.mapper.UserEntityMapper;
import io.geekstore.mapper.UserRoleJoinEntityMapper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.service.helpers.VerificationTokenGenerator;
import io.geekstore.types.role.Role;
import io.geekstore.types.user.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserEntityMapper userEntityMapper;
    private final RoleEntityMapper roleEntityMapper;
    private final UserRoleJoinEntityMapper userRoleJoinEntityMapper;
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final AuthenticationMethodEntityMapper authenticationMethodEntityMapper;

    public boolean checkHasNativeAuthMethod(Long userId) {
        QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthenticationMethodEntity::getUserId, userId);
        List<AuthenticationMethodEntity> authMethods =
                this.authenticationMethodEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(authMethods)) {
            return false;
        }
        return authMethods.stream().anyMatch(m -> !m.isExternal());
    }

    public AuthenticationMethodEntity getNativeAuthMethodEntityByUserId(Long userId) {
        QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthenticationMethodEntity::getUserId, userId);
        List<AuthenticationMethodEntity> authMethods =
                this.authenticationMethodEntityMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(authMethods)) {
            throw new InternalServerError("User's native authentication methods not loaded");
        }
        AuthenticationMethodEntity nativeAuthMethod =
                authMethods.stream().filter(m -> !m.isExternal())
                        .findFirst().orElse(null);
        if (nativeAuthMethod == null) {
            throw new InternalServerError("User's native authentication method not found");
        }
        return nativeAuthMethod;
    }

    public UserEntity findUserEntityById(Long id) {
        return this.userEntityMapper.selectById(id);
    }

    public User findUserWithRolesById(Long userId) {
        UserEntity userEntity = this.findUserEntityById(userId);
        if (userEntity == null) return null;
        User user = BeanMapper.map(userEntity, User.class);
        List<Role> roles = this.findRolesByUserId(userId);
        user.setRoles(roles);
        return user;
    }

    public UserEntity findUserEntityByIdentifier(String identifier) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getIdentifier, identifier);
        UserEntity userEntity = this.userEntityMapper.selectOne(queryWrapper);
        return userEntity;
    }

    public UserEntity findUserEntityByEmailAddress(String emailAddress) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getIdentifier, emailAddress).isNull(UserEntity::getDeletedAt);
        return this.userEntityMapper.selectOne(queryWrapper);
    }

    public User findUserWithRoleByIdentifier(String identifier) {
        UserEntity userEntity = this.findUserEntityByIdentifier(identifier);
        if (userEntity == null) return null;
        User user = BeanMapper.map(userEntity, User.class);
        List<Role> roles = this.findRolesByUserId(user.getId());
        user.setRoles(roles);
        return user;
    }

    public List<Role> findRolesByUserId(Long userId) {
        QueryWrapper<UserRoleJoinEntity> userRoleJoinEntityQueryWrapper = new QueryWrapper<>();
        userRoleJoinEntityQueryWrapper.lambda().eq(UserRoleJoinEntity::getUserId, userId);
        List<UserRoleJoinEntity> userRoleJoinEntities =
                this.userRoleJoinEntityMapper.selectList(userRoleJoinEntityQueryWrapper);
        if (CollectionUtils.isEmpty(userRoleJoinEntities)) return new ArrayList<>();

        List<Long> roleIds =
                userRoleJoinEntities.stream().map(UserRoleJoinEntity::getRoleId).collect(Collectors.toList());
        QueryWrapper<RoleEntity> roleEntityQueryWrapper = new QueryWrapper<>();
        roleEntityQueryWrapper.lambda().in(RoleEntity::getId, roleIds);
        List<RoleEntity> roleEntities = this.roleEntityMapper.selectList(roleEntityQueryWrapper);
        return roleEntities.stream().map(roleEntity -> BeanMapper.map(roleEntity, Role.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserEntity createCustomerUser(String identifier, String password) {
        UserEntity userEntity = new UserEntity();
        userEntity.setIdentifier(identifier);
        this.userEntityMapper.insert(userEntity); // 需要生成userId

        userEntity = addNativeAuthenticationMethod(userEntity, identifier, password);

        this.userEntityMapper.updateById(userEntity); // verified字段有更新

        RoleEntity customerRoleEntity = this.roleService.getCustomerRole();
        UserRoleJoinEntity userRoleJoinEntity = new UserRoleJoinEntity();
        userRoleJoinEntity.setUserId(userEntity.getId());
        userRoleJoinEntity.setRoleId(customerRoleEntity.getId());
        this.userRoleJoinEntityMapper.insert(userRoleJoinEntity);

        return userEntity;
    }

    public UserEntity addNativeAuthenticationMethod(UserEntity userEntity, String identifier, String password) {
        AuthenticationMethodEntity authenticationMethodEntity = new AuthenticationMethodEntity();
        authenticationMethodEntity.setExternal(false); // native
        if (this.configService.getAuthOptions().isRequireVerification()) {
            authenticationMethodEntity.setVerificationToken(
                    this.verificationTokenGenerator.generateVerificationToken());
            userEntity.setVerified(false);
        } else {
            userEntity.setVerified(true);
        }
        if (password != null) {
            authenticationMethodEntity.setPasswordHash(this.passwordEncoder.encode(password));
        } else {
            authenticationMethodEntity.setPasswordHash("");
        }
        authenticationMethodEntity.setIdentifier(identifier);
        authenticationMethodEntity.setUserId(userEntity.getId());
        this.authenticationMethodEntityMapper.insert(authenticationMethodEntity);
        return userEntity;
    }

    @Transactional
    public UserEntity createAdminUser(String identifier, String password) {
        UserEntity userEntity = new UserEntity();
        userEntity.setIdentifier(identifier);
        userEntity.setVerified(true);
        this.userEntityMapper.insert(userEntity); // 需要生成userId
        AuthenticationMethodEntity authenticationMethodEntity = new AuthenticationMethodEntity();
        authenticationMethodEntity.setExternal(false); // native
        authenticationMethodEntity.setIdentifier(identifier);
        authenticationMethodEntity.setPasswordHash(this.passwordEncoder.encode(password));
        authenticationMethodEntity.setUserId(userEntity.getId());
        this.authenticationMethodEntityMapper.insert(authenticationMethodEntity);
        return userEntity;
    }

    public void softDelete(Long userId) {
        UserEntity userEntity = ServiceHelper.getEntityOrThrow(this.userEntityMapper, UserEntity.class, userId);
        userEntity.setDeletedAt(new Date());
        this.userEntityMapper.updateById(userEntity);
    }

    @Transactional
    public UserEntity setVerificationToken(UserEntity userEntity) {
        AuthenticationMethodEntity nativeAuthMethodEntity = getNativeAuthMethodEntityByUserId(userEntity.getId());
        nativeAuthMethodEntity.setVerificationToken(this.verificationTokenGenerator.generateVerificationToken());
        this.authenticationMethodEntityMapper.updateById(nativeAuthMethodEntity);
        userEntity.setVerified(false);
        this.userEntityMapper.updateById(userEntity);
        return userEntity;
    }

    @Transactional
    public UserEntity verifyUserByToken(String verificationToken, String password) {
        QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthenticationMethodEntity::getVerificationToken, verificationToken);
        AuthenticationMethodEntity authMethodEntity =
                this.authenticationMethodEntityMapper.selectOne(queryWrapper);
        if (authMethodEntity == null) return null;

        if (!this.verificationTokenGenerator.verifyVerificationToken(verificationToken)) {
            throw new VerificationTokenException(ErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }

        AuthenticationMethodEntity nativeAuthMethod =
                this.getNativeAuthMethodEntityByUserId(authMethodEntity.getUserId());
        if (StringUtils.isEmpty(password)) {
            if (StringUtils.isEmpty(nativeAuthMethod.getPasswordHash())) {
                throw new UserInputException("A password must be provided as it was not set during registration");
            }
        } else {
            if (!StringUtils.isEmpty(nativeAuthMethod.getPasswordHash())) {
                throw new UserInputException("A password has already been set during registration");
            }
            nativeAuthMethod.setPasswordHash(this.passwordEncoder.encode(password));
        }
        nativeAuthMethod.setVerificationToken(null);
        this.authenticationMethodEntityMapper.updateById(nativeAuthMethod);

        UserEntity userEntity = this.userEntityMapper.selectById(authMethodEntity.getUserId());
        userEntity.setVerified(true);
        this.userEntityMapper.updateById(userEntity);

        return userEntity;
    }

    public UserEntity setPasswordResetToken(String emailAddress) {
        UserEntity userEntity = this.findUserEntityByEmailAddress(emailAddress);
        if (userEntity == null) return null;

        AuthenticationMethodEntity nativeAuthMethod =
                this.getNativeAuthMethodEntityByUserId(userEntity.getId());
        nativeAuthMethod.setPasswordResetToken(this.verificationTokenGenerator.generateVerificationToken());
        this.authenticationMethodEntityMapper.updateById(nativeAuthMethod);
        return userEntity;
    }

    public UserEntity resetPasswordByToken(String passwordResetToken, String password) {
        QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthenticationMethodEntity::getPasswordResetToken, passwordResetToken);
        AuthenticationMethodEntity authMethodEntity =
                this.authenticationMethodEntityMapper.selectOne(queryWrapper);
        if (authMethodEntity == null) return null;

        if (!this.verificationTokenGenerator.verifyVerificationToken(passwordResetToken)) {
            throw new PasswordResetTokenExpiredException();
        }

        AuthenticationMethodEntity nativeAuthMethod =
                this.getNativeAuthMethodEntityByUserId(authMethodEntity.getUserId());
        nativeAuthMethod.setPasswordHash(this.passwordEncoder.encode(password));
        nativeAuthMethod.setPasswordResetToken(null);

        this.authenticationMethodEntityMapper.updateById(nativeAuthMethod);

        return this.findUserEntityById(authMethodEntity.getUserId());
    }

    public Pair<UserEntity, String> changeIdentifierByToken(String token) {
        QueryWrapper<AuthenticationMethodEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AuthenticationMethodEntity::getIdentifierChangeToken, token);
        AuthenticationMethodEntity authMethodEntity =
                this.authenticationMethodEntityMapper.selectOne(queryWrapper);
        if (authMethodEntity == null) throw new IdentifierChangeTokenException();

        if (!this.verificationTokenGenerator.verifyVerificationToken(token)) {
            throw new IdentifierChangeTokenExpiredException();
        }

        AuthenticationMethodEntity nativeAuthMethod =
                this.getNativeAuthMethodEntityByUserId(authMethodEntity.getUserId());
        String pendingIdentifier = nativeAuthMethod.getPendingIdentifier();
        if (StringUtils.isEmpty(pendingIdentifier)) {
            throw new InternalServerError("Pending identifier is missing");
        }

        UserEntity userEntity = this.userEntityMapper.selectById(authMethodEntity.getUserId());
        final String oldIdentifier = userEntity.getIdentifier();
        userEntity.setIdentifier(pendingIdentifier);
        nativeAuthMethod.setIdentifier(pendingIdentifier);
        nativeAuthMethod.setIdentifierChangeToken(null);
        nativeAuthMethod.setPendingIdentifier(null);

        this.authenticationMethodEntityMapper.updateById(nativeAuthMethod);
        this.userEntityMapper.updateById(userEntity);

        return Pair.of(userEntity, oldIdentifier);
    }

    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        // 确保用户存在
        ServiceHelper.getEntityOrThrow(this.userEntityMapper, UserEntity.class, userId);
        AuthenticationMethodEntity nativeAuthMethod =
                this.getNativeAuthMethodEntityByUserId(userId);
        boolean matches = this.passwordEncoder.matches(currentPassword, nativeAuthMethod.getPasswordHash());
        if (!matches) {
            throw new UnauthorizedException();
        }
        nativeAuthMethod.setPasswordHash(this.passwordEncoder.encode(newPassword));
        this.authenticationMethodEntityMapper.updateById(nativeAuthMethod);
        return true;
    }

    public void changeIdentifierAndSetToken(Long userId, String newIdentifier) {
        AuthenticationMethodEntity nativeAuthMethod =
                this.getNativeAuthMethodEntityByUserId(userId);
        nativeAuthMethod.setPendingIdentifier(newIdentifier);
        nativeAuthMethod.setIdentifierChangeToken(this.verificationTokenGenerator.generateVerificationToken());
        this.authenticationMethodEntityMapper.updateById(nativeAuthMethod);
    }

}
