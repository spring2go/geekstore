/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.resolver.admin;

import io.geekstore.custom.security.Allow;
import io.geekstore.data_import.importer.Importer;
import io.geekstore.exception.InternalServerError;
import io.geekstore.types.ImportInfo;
import io.geekstore.types.common.Permission;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportMutation implements GraphQLMutationResolver {
    private final Importer importer;

    @Allow(Permission.SuperAdmin)
    public ImportInfo importProducts(Part csvFile, DataFetchingEnvironment dfe) {
        try {
            InputStream input = csvFile.getInputStream();
            return this.importer.parseAndImport(input);
        } catch (IOException ex) {
            log.error("IO exception during products import", ex);
            throw new InternalServerError("IO exception during products import");
        }
    }
}
