/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.config;

import io.geekstore.custom.scalar.DateTimeScalar;
import graphql.kickstart.servlet.apollo.ApolloScalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Configuration
public class GraphQLConfig {
    @Bean
    public GraphQLScalarType dateTime() {
        return DateTimeScalar.build();
    }

    @Bean
    public GraphQLScalarType upload() {
        return ApolloScalars.Upload;
    }

    @Bean
    public GraphQLScalarType json() {
        return ExtendedScalars.Json;
    }
}
