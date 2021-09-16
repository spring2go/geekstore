/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore;

import io.geekstore.data_import.InitialData;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration options used to initialize a test server environment.
 *
 * Created on Nov, 2020 by @author bobo
 */
@Data
@Builder
public class PopulateOptions {

    /**
     * An object containing non-product data which is used to populate the database.
     */
    private InitialData initialData;
    /**
     * The number of fake Customers to populate into the database.
     *
     * @default 10
     */
    private Integer customerCount;
    /**
     * The path to a CSV file containing product data to import.
     */
    private String productCsvPath;

    /**
     * Set this to `true` to log some information about the database population process.
     *
     * @default false
     */
    private boolean logging;
}
