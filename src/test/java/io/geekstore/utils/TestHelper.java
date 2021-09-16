/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.utils;

import io.geekstore.data_import.CollectionDefinition;
import io.geekstore.data_import.FacetValueCollectionFilterDefinition;
import io.geekstore.data_import.InitialData;
import io.geekstore.data_import.ShippingMethodData;
import io.geekstore.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created on Nov, 2020 by @author bobo
 */
@Component
public class TestHelper {

    @Autowired
    @Qualifier("asyncExecutor")
    ThreadPoolTaskExecutor executor;

    @Autowired
    private ConfigService configService;

    public String getImportAssetsDir() {
        Path productsCsvPath = Paths.get("src","test", "resources", "fixtures", "assets");
        return productsCsvPath.toFile().getAbsolutePath();
    }

    public String getTestFixture(String filename) {
        Path productsCsvPath = Paths.get("src","test", "resources", "fixtures", filename);
        return productsCsvPath.toFile().getAbsolutePath();
    }

    public InitialData getInitialData() {
        InitialData initialData = new InitialData();

        ShippingMethodData shippingMethodData = new ShippingMethodData();
        shippingMethodData.setName("Standard Shipping");
        shippingMethodData.setPrice(500);
        initialData.getShippingMethods().add(shippingMethodData);

        shippingMethodData = new ShippingMethodData();
        shippingMethodData.setName("Express Shipping");
        shippingMethodData.setPrice(1000);
        initialData.getShippingMethods().add(shippingMethodData);

        CollectionDefinition collectionDef = new CollectionDefinition();
        collectionDef.setName("Plants");
        FacetValueCollectionFilterDefinition filterDef = new FacetValueCollectionFilterDefinition();
        filterDef.setCode("facet-value-filter");
        filterDef.getFacetValueNames().add("plants");
        filterDef.setContainsAny(false);
        collectionDef.getFilters().add(filterDef);
        initialData.getColletions().add(collectionDef);
        return initialData;
    }

    /**
     * For mutation which trigger background tasks, this can be used to "pause" the execution of
     * the test until those tasks have completed.
     *
     * 缺省超时时间5秒
     */
    public void awaitRunningTasks() {
        awaitRunningTasks(5000);
    }

    public void awaitRunningTasks(long timeout) {
        int runningTasks = 0;
        long startTime = System.currentTimeMillis();
        boolean timedOut = false;

        // Allow a brief period for the jobs to start in the case that
        // e.g. event debouncing is used before triggering the task.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        do {
            runningTasks = this.executor.getActiveCount();
            timedOut = timeout < System.currentTimeMillis() - startTime;
        } while (runningTasks > 0 && !timedOut);
    }

    @PostConstruct
    void initTestConfig() {
        this.configService.getImportExportOptions().setImportAssetsDir(this.getImportAssetsDir());
    }
}
