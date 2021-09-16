/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.*;
import io.geekstore.exception.EntityNotFoundException;
import io.geekstore.mapper.AdministratorEntityMapper;
import io.geekstore.mapper.AuthenticationMethodEntityMapper;
import io.geekstore.mapper.UserEntityMapper;
import io.geekstore.mapper.UserRoleJoinEntityMapper;
import io.geekstore.options.SuperadminCredentials;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.administrator.*;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.role.Role;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class AdministratorService {
    private final ConfigService configService;
    private final PasswordEncoder passwordEncoder;
    private final UserEntityMapper userEntityMapper;
    private final AdministratorEntityMapper administratorEntityMapper;
    private final UserRoleJoinEntityMapper userRoleJoinEntityMapper;
    private final AuthenticationMethodEntityMapper authenticationMethodEntityMapper;
    private final UserService userService;
    private final RoleService roleService;

    @PostConstruct
    public void initAdministrators() {
        this.ensureSuperAdminExists();
    }

    public AdministratorList findAll(AdministratorListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<AdministratorEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().isNull(AdministratorEntity::getDeletedAt);
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<AdministratorEntity> administratorEntityPage =
                this.administratorEntityMapper.selectPage(page, queryWrapper);

        AdministratorList administratorList = new AdministratorList();
        administratorList.setTotalItems((int) administratorEntityPage.getTotal());

        if (CollectionUtils.isEmpty(administratorEntityPage.getRecords())) return administratorList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        administratorEntityPage.getRecords().forEach(administratorEntity -> {
            Administrator administrator = BeanMapper.map(administratorEntity, Administrator.class);
            administratorList.getItems().add(administrator);
        });

        return administratorList;
    }

    private void buildFilter(QueryWrapper queryWrapper, AdministratorFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getFirstName(), "first_name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getLastName(), "last_name");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getEmailAddress(), "email_address");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }


    private void buildSortOrder(QueryWrapper queryWrapper, AdministratorSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getFirstName(), "first_name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getLastName(), "last_name");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }


    public AdministratorEntity findOneEntity(Long administratorId) {
        QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AdministratorEntity::getId, administratorId).isNull(AdministratorEntity::getDeletedAt);
        AdministratorEntity administratorEntity = this.administratorEntityMapper.selectOne(queryWrapper);
        return administratorEntity;
    }

    public AdministratorEntity findOneEntityByUserId(Long userId) {
        QueryWrapper<AdministratorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(AdministratorEntity::getUserId, userId).isNull(AdministratorEntity::getDeletedAt);
        AdministratorEntity administratorEntity = this.administratorEntityMapper.selectOne(queryWrapper);
        return administratorEntity;
    }

    @Transactional
    public AdministratorEntity create(CreateAdministratorInput input) {
        AdministratorEntity administratorEntity = BeanMapper.map(input, AdministratorEntity.class);
        UserEntity userEntity = this.userService.createAdminUser(input.getEmailAddress(), input.getPassword());
        administratorEntity.setUserId(userEntity.getId());
        this.administratorEntityMapper.insert(administratorEntity);
        input.getRoleIds().forEach(roleId -> this.assignRole(administratorEntity.getId(), roleId));
        return administratorEntity;
    }

    @Transactional
    public AdministratorEntity update(UpdateAdministratorInput input) {
        AdministratorEntity administratorEntity =
                ServiceHelper.getEntityOrThrow(this.administratorEntityMapper, AdministratorEntity.class, input.getId());
        BeanMapper.patch(input, administratorEntity);
        this.administratorEntityMapper.updateById(administratorEntity);

        if (!StringUtils.isEmpty(input.getPassword())) {
            AuthenticationMethodEntity nativeAuthMethodEntity =
                    userService.getNativeAuthMethodEntityByUserId(administratorEntity.getUserId());
            nativeAuthMethodEntity.setPasswordHash(this.passwordEncoder.encode(input.getPassword()));
            this.authenticationMethodEntityMapper.updateById(nativeAuthMethodEntity);
        }
        if (!CollectionUtils.isEmpty(input.getRoleIds())) {
            // 清除现有roles
            QueryWrapper<UserRoleJoinEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(UserRoleJoinEntity::getUserId, administratorEntity.getUserId());
            this.userRoleJoinEntityMapper.delete(queryWrapper);

            input.getRoleIds().forEach(roleId -> this.assignRole(administratorEntity.getId(), roleId));
        }
        return administratorEntity;
    }

    /**
     * Assigns a Role to the Administrator's User entity.
     */
    public AdministratorEntity assignRole(Long administratorId, Long roleId) {
        AdministratorEntity administratorEntity = this.administratorEntityMapper.selectById(administratorId);
        if (administratorEntity == null) throw new EntityNotFoundException("Administrator", administratorId);
        Role role = this.roleService.findOne(roleId);
        if (role == null) throw new EntityNotFoundException("Role", roleId);
        QueryWrapper<UserRoleJoinEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserRoleJoinEntity::getRoleId, roleId)
                .eq(UserRoleJoinEntity::getUserId, administratorEntity.getUserId());
        if (this.userRoleJoinEntityMapper.selectCount(queryWrapper) == 0) { // 确保不存在
            UserRoleJoinEntity userRoleJoinEntity = new UserRoleJoinEntity();
            userRoleJoinEntity.setRoleId(roleId);
            userRoleJoinEntity.setUserId(administratorEntity.getUserId());
            this.userRoleJoinEntityMapper.insert(userRoleJoinEntity);
        }
        return administratorEntity;
    }

    public DeletionResponse softDelete(Long id) {
        AdministratorEntity administratorEntity =
                ServiceHelper.getEntityOrThrow(this.administratorEntityMapper, AdministratorEntity.class, id);
        administratorEntity.setDeletedAt(new Date());
        this.administratorEntityMapper.updateById(administratorEntity);
        this.userService.softDelete(administratorEntity.getUserId());
        DeletionResponse deletionResponse = new DeletionResponse();
        deletionResponse.setResult(DeletionResult.DELETED);
        return deletionResponse;
    }

    /**
     * There must always exists a SuperAdmin, otherwise full administration via API will
     * no longer be possible.
     */
    private void ensureSuperAdminExists() {
        SuperadminCredentials superadminCredentials =
                this.configService.getAuthOptions().getSuperadminCredentials();

        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getIdentifier, superadminCredentials.getIdentifier());
        UserEntity superAdminUserEntity = userEntityMapper.selectOne(queryWrapper);
        if (superAdminUserEntity == null) {
            RoleEntity superAdminRole = this.roleService.getSuperAdminRoleEntity();
            CreateAdministratorInput input = new CreateAdministratorInput();
            input.setEmailAddress(superadminCredentials.getIdentifier());
            input.setPassword(superadminCredentials.getPassword());
            input.setFirstName("Super");
            input.setLastName("Admin");
            input.getRoleIds().add(superAdminRole.getId());
            this.create(input);
        }
    }
}
