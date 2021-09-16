/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service;

import io.geekstore.common.RequestContext;
import io.geekstore.common.RoleCode;
import io.geekstore.common.utils.BeanMapper;
import io.geekstore.entity.RoleEntity;
import io.geekstore.exception.EntityNotFoundException;
import io.geekstore.exception.ForbiddenException;
import io.geekstore.exception.InternalServerError;
import io.geekstore.exception.UserInputException;
import io.geekstore.mapper.RoleEntityMapper;
import io.geekstore.service.helpers.PageInfo;
import io.geekstore.service.helpers.QueryHelper;
import io.geekstore.service.helpers.ServiceHelper;
import io.geekstore.types.common.DeletionResponse;
import io.geekstore.types.common.DeletionResult;
import io.geekstore.types.common.Permission;
import io.geekstore.types.role.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("Duplicates")
public class RoleService {
    private final RoleEntityMapper roleEntityMapper;

    public RoleList findAll(RoleListOptions options) {
        PageInfo pageInfo = ServiceHelper.getListOptions(options);
        IPage<RoleEntity> page = new Page<>(pageInfo.current, pageInfo.size);
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        if (options != null) {
            buildFilter(queryWrapper, options.getFilter());
            buildSortOrder(queryWrapper, options.getSort());
        }
        IPage<RoleEntity> roleEntityPage = this.roleEntityMapper.selectPage(page, queryWrapper);

        RoleList roleList = new RoleList();
        roleList.setTotalItems((int) roleEntityPage.getTotal());

        if (CollectionUtils.isEmpty(roleEntityPage.getRecords())) return roleList; // 返回空

        // 将持久化实体类型转换成GraphQL传输类型
        roleEntityPage.getRecords().forEach(roleEntity -> {
            Role role = BeanMapper.map(roleEntity, Role.class);
            roleList.getItems().add(role);
        });

        return roleList;
    }

    private void buildSortOrder(QueryWrapper queryWrapper, RoleSortParameter sortParameter) {
        if (sortParameter == null) return ;
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getId(), "id");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCode(), "code");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getDescription(), "description");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneSortOrder(queryWrapper, sortParameter.getUpdatedAt(), "updated_at");
    }

    private void buildFilter(QueryWrapper queryWrapper, RoleFilterParameter filterParameter) {
        if (filterParameter == null) return;
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getCode(), "code");
        QueryHelper.buildOneStringOperatorFilter(queryWrapper, filterParameter.getDescription(), "description");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getCreatedAt(), "created_at");
        QueryHelper.buildOneDateOperatorFilter(queryWrapper, filterParameter.getUpdatedAt(), "updated_at");
    }

    public Role findOne(Long roleId) {
        RoleEntity roleEntity = this.roleEntityMapper.selectById(roleId);
        if (roleEntity == null) return null;
        return BeanMapper.map(roleEntity, Role.class);
    }

    public RoleEntity getSuperAdminRoleEntity() {
        RoleEntity roleEntity = this.getRoleEntityByCode(RoleCode.SUPER_ADMIN_ROLE_CODE);
        if (roleEntity == null) throw new InternalServerError("SuperAdmin role not found");
        return roleEntity;
    }

    public RoleEntity getCustomerRole() {
        RoleEntity roleEntity = this.getRoleEntityByCode(RoleCode.CUSTOMER_ROLE_CODE);
        if (roleEntity == null) throw new InternalServerError("Customer role not found");
        return roleEntity;
    }

    /**
     * Returns all the valid Permission values
     */
    public List<Permission> getAllPermissions() {
        return Arrays.asList(Permission.values());
    }

    public Role create(RequestContext ctx, CreateRoleInput input) {
        if (ctx.getActiveUserId() == null) throw new ForbiddenException();

        this.checkPermissionsAreValid(input.getPermissions());

        RoleEntity roleEntity = this.createRoleEntity(input);
        return BeanMapper.map(roleEntity, Role.class);
    }

    public Role update(RequestContext ctx, UpdateRoleInput input) {
        this.checkPermissionsAreValid(input.getPermissions());
        RoleEntity roleEntity = this.roleEntityMapper.selectById(input.getId());
        if (roleEntity == null) throw new EntityNotFoundException("Role", input.getId());
        if (RoleCode.SUPER_ADMIN_ROLE_CODE.equals(roleEntity.getCode()) ||
                RoleCode.CUSTOMER_ROLE_CODE.equals(roleEntity.getCode())) {
            throw new InternalServerError("The role '" +roleEntity.getCode() + "' cannot be modified");
        }
        if (!StringUtils.isEmpty(input.getCode())) {
            roleEntity.setCode(input.getCode());
        }
        if (!StringUtils.isEmpty(input.getDescription())) {
            roleEntity.setDescription(input.getDescription());
        }
        if (!CollectionUtils.isEmpty(input.getPermissions())) {
            // 去重 + Authenticated
            Set<Permission> permissions = new HashSet<>(input.getPermissions());
            permissions.add(Permission.Authenticated);
            roleEntity.setPermissions(new ArrayList<>(permissions));
        }
        this.roleEntityMapper.updateById(roleEntity);
        return BeanMapper.map(roleEntity, Role.class);
    }

    public DeletionResponse delete(RequestContext ctx, Long id) {
        RoleEntity roleEntity = this.roleEntityMapper.selectById(id);
        if (roleEntity == null) throw new EntityNotFoundException("Role", id);
        if (RoleCode.SUPER_ADMIN_ROLE_CODE.equals(roleEntity.getCode()) ||
                RoleCode.CUSTOMER_ROLE_CODE.equals(roleEntity.getCode())) {
            throw new InternalServerError("The role '" +roleEntity.getCode() + "' cannot be deleted");
        }
        this.roleEntityMapper.deleteById(id);
        DeletionResponse response = new DeletionResponse();
        response.setResult(DeletionResult.DELETED);
        return response;
    }

    private void checkPermissionsAreValid(List<Permission> permissions) {
        if (CollectionUtils.isEmpty(permissions)) return;
        List allPermissions = this.getAllPermissions();
        permissions.forEach(p -> {
            if (!allPermissions.contains(p)) {
                throw new UserInputException("The permission { " + p + " } is not valid");
            }
        });
    }

    @PostConstruct
    public void initRoles() {
        this.ensureSuperAdminRoleExists();
        this.ensureCustomerRoleExists();
    }

    private void ensureSuperAdminRoleExists() {
        List<Permission> allPermissions = Arrays.asList(Permission.values())
                .stream().filter(p -> !Permission.Owner.equals(p)).collect(Collectors.toList());
        try {
            RoleEntity superAdminRole = this.getSuperAdminRoleEntity();
            boolean hasAllPermissions = allPermissions.stream()
                    .allMatch(p -> superAdminRole.getPermissions().contains(p));
            hasAllPermissions = hasAllPermissions && allPermissions.size() == superAdminRole.getPermissions().size();
            if (!hasAllPermissions) {
                superAdminRole.setPermissions(allPermissions);
                this.roleEntityMapper.updateById(superAdminRole);
            }
        } catch (Exception ex) {
            CreateRoleInput input = new CreateRoleInput();
            input.setCode(RoleCode.SUPER_ADMIN_ROLE_CODE);
            input.setDescription(RoleCode.SUPER_ADMIN_ROLE_DESCRIPTION);
            input.setPermissions(allPermissions);
            this.createRoleEntity(input);
        }
    }

    private void ensureCustomerRoleExists() {
        try {
            this.getCustomerRole();
        } catch (Exception ex) {
            CreateRoleInput input = new CreateRoleInput();
            input.setCode(RoleCode.CUSTOMER_ROLE_CODE);
            input.setDescription(RoleCode.CUSTOMER_ROLE_DESCRIPTION);
            input.getPermissions().add(Permission.Authenticated);
            this.createRoleEntity(input);
        }
    }

    private RoleEntity createRoleEntity(CreateRoleInput input) {
        // 去重 + Authenticated
        Set<Permission> permissions = new HashSet<>(input.getPermissions());
        permissions.add(Permission.Authenticated);
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setCode(input.getCode());
        roleEntity.setDescription(input.getDescription());
        roleEntity.setPermissions(new ArrayList<>(permissions));
        this.roleEntityMapper.insert(roleEntity);
        return roleEntity;
    }

    private RoleEntity getRoleEntityByCode(String code) {
        QueryWrapper<RoleEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RoleEntity::getCode, code);
        return this.roleEntityMapper.selectOne(queryWrapper);
    }
}
