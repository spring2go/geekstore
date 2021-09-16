/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore;

import io.geekstore.options.ConfigOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created on Nov, 2020 by @author bobo
 */
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
@EnableConfigurationProperties(ConfigOptions.class)
@RestController
@Slf4j
public class GeekStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeekStoreApplication.class, args);
        log.info("GeekStore Started! Have Fun!");
    }

    @GetMapping
    public String hello() {return "Hello GeekStore!";}
}
