/*
 * Copyright (c) 2020 GeekStore.
 * All rights reserved.
 */

package io.geekstore.service.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created on Dec, 2020 by @author bobo
 *
 * 参考：
 * https://better-coding.com/spring-how-to-autowire-bean-in-a-static-class/
 * https://gist.github.com/ufuk/5264e0a3c222ed8fc71dca9e6d821959
 */
@Component
public class SpringContext {
   @Autowired
   private ApplicationContext instanceContext;

   private static ApplicationContext staticContext;

   public static <T> T getBean(Class<T> clazz) {
       // 非Spring容器的测试环境情况
       if (staticContext ==  null) return null;
       // Spring容器环境，但是容器环境已经关闭，测试环境情况
       if (staticContext instanceof ConfigurableApplicationContext &&
               !((ConfigurableApplicationContext)staticContext).isActive()) return null;

       return staticContext.getBean(clazz);
   }

   @PostConstruct
   public void init() {
       staticContext = instanceContext;
   }
}
