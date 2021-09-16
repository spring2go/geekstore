/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.common.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.*;

/**
 * Created on Nov, 2020 by @author bobo
 */
public abstract class BeanMapper {
    private static String[] getNullOrEmptyCollectionPropertyNames (Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<String>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
            // 空集合也算
            if (srcValue instanceof Collection) {
                Collection collection = (Collection)srcValue;
                if (collection.size() == 0) {
                    emptyNames.add(pd.getName());
                }
            }
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    /**
     * 拷贝相同字段名的字段值
     *
     * @param src 源对象
     * @param target 目标对象
     */
    public static void map(Object src, Object target) {
        BeanUtils.copyProperties(src, target);
    }

    /**
     * 拷贝相同字段名的字段值，仅拷贝源对象上的非null和非空集合属性，忽略null和空集合
     *
     * @param src 源对象
     * @param target 目标对象
     */
    public static void patch(Object src, Object target) {
        BeanUtils.copyProperties(src, target, getNullOrEmptyCollectionPropertyNames(src));
    }

    /**
     * 拷贝相同字段名的字段值
     *
     * @param src 源对象
     * @param clazz 目标类
     * @param <T> 目标类型范型
     * @return 目标类实例
     */
    public static <T> T map(Object src, Class<T> clazz) {
        T target = null;
        try {
            target = (T) clazz.getConstructors()[0].newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("failed to construct instance from class '" + clazz + "'", ex);
        }
        BeanUtils.copyProperties(src, target);
        return target;
    }

    /**
     * 拷贝相同字段名的字段值，仅拷贝源对象上的非null和非空集合属性，忽略null和空集合
     *
     * @param src 源对象
     * @param clazz 目标类
     * @param <T> 目标类型范型
     * @return 目标类实例
     */
    public static <T> T patch(Object src, Class<T> clazz) {
        T target = null;
        try {
            target = (T) clazz.getConstructors()[0].newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("failed to construct instance from class '" + clazz + "'", ex);
        }
        BeanUtils.copyProperties(src, target, getNullOrEmptyCollectionPropertyNames(src));
        return target;
    }
}
