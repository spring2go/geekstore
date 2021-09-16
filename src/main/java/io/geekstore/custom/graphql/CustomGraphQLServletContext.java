/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.custom.graphql;

import io.geekstore.common.RequestContext;
import graphql.kickstart.execution.context.DefaultGraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.kickstart.servlet.context.GraphQLServletContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.List;
import java.util.Map;

/**
 * Created on Nov, 2020 by @author bobo
 */
public class CustomGraphQLServletContext extends DefaultGraphQLContext implements GraphQLServletContext {

    final DefaultGraphQLServletContext defaultGraphQLServletContext;

    private RequestContext requestContext;

    public RequestContext getRequestContext() {
        return this.requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public CustomGraphQLServletContext(DefaultGraphQLServletContext defaultGraphQLServletContext) {
        super(defaultGraphQLServletContext.getDataLoaderRegistry().orElse(null),
                defaultGraphQLServletContext.getSubject().orElse(null));
        this.defaultGraphQLServletContext = defaultGraphQLServletContext;
    }

    @Override
    public List<Part> getFileParts() {
        return defaultGraphQLServletContext.getFileParts();
    }

    @Override
    public Map<String, List<Part>> getParts() {
        return defaultGraphQLServletContext.getParts();
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return defaultGraphQLServletContext.getHttpServletRequest();
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return defaultGraphQLServletContext.getHttpServletResponse();
    }
}
