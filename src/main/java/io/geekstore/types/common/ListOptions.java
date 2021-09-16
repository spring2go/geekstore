/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.types.common;

/**
 * Created on Nov, 2020 by @author bobo
 */
public interface ListOptions {
    Integer getCurrentPage();
    void setCurrentPage(Integer currentPage);
    Integer getPageSize();
    void setPageSize(Integer pageSize);
}
