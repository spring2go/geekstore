/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.mybatis_plus;

import io.geekstore.common.Constant;
import io.geekstore.config.session_cache.CachedSessionUser;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Date;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
public class AuditHandler implements MetaObjectHandler {
    static final String CREATED_AT = "createdAt";
    static final String UPDATED_AT = "updatedAt";
    static final String CREATED_BY = "createdBy";
    static final String UPDATED_BY = "updatedBy";

    String getCurrentUser() {
        if (RequestContextHolder.getRequestAttributes() == null) {
            // No thread-bound request context, use system username
            return Constant.USERNAME_SYSTEM;
        }
        CachedSessionUser user = (CachedSessionUser) RequestContextHolder.getRequestAttributes()
                .getAttribute(Constant.REQUEST_ATTRIBUTE_CURRENT_USER, RequestAttributes.SCOPE_REQUEST);
        if (user == null || user.getIdentifier() == null) {
            return Constant.USERNAME_ANONYMOUS;
        }
        return user.getIdentifier();
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, CREATED_AT, Date.class, new Date());
        this.strictUpdateFill(metaObject, UPDATED_AT, Date.class, new Date());
        this.strictInsertFill(metaObject, CREATED_BY, String.class, getCurrentUser());
        this.strictUpdateFill(metaObject, UPDATED_BY, String.class, getCurrentUser());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATED_AT, Date.class, new Date());
        this.strictUpdateFill(metaObject, UPDATED_BY, String.class, getCurrentUser());
    }
}
