/*
 * Copyright (C) 2017 ~ 2025 the original author or authors.
 * <Wanglsir@gmail.com, 983708408@qq.com> Technology CO.LTD.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Reference to website: http://wl4g.com
 */

import static com.wl4g.infra.common.lang.ClassUtils2.isPresent
import static com.wl4g.infra.common.runtime.JvmRuntimeTool.isJvmInDebugging
import static org.springframework.boot.context.config.ConfigFileApplicationListener.*

import org.springframework.boot.Banner

import com.wl4g.infra.core.boot.listener.IBootstrappingConfigurer

/**
 * IAM Gateway implementation of {@link IBootstrappingConfigurer}
 */
class IamGatewayBootstrappingConfigurer implements IBootstrappingConfigurer {

    @Override
    def int getOrder() {
        return -100
    }

    def Banner.Mode bannerMode(Banner.Mode prevMode) {
        return Banner.Mode.LOG;
    }

    @Override
    def Properties defaultProperties(Properties prevDefaultProperties) {
        def defaultProperties = new Properties()
        // Preset spring.config.name
        // for example: spring auto load for 'application-dev.yml/application-data-dev.yml'
        def configName = new StringBuffer("bootstrap,application,iam-gateway")

        // Preset spring.config.location
        // for example: spring auto load for 'classpath:/application-data-dev.yml'
        def location = new StringBuffer("classpath:/")
        if (isPresent("com.alibaba.cloud.nacos.NacosConfigAutoConfiguration")) {
            configName.append(",iam-gateway-nacos");
            //location.append(",classpath:/nacos/")
        }

        // for demo environment.
        if (isJvmInDebugging) {
            configName.append(",example");
            location.append(",classpath:/example/");
        }

        defaultProperties.put(CONFIG_NAME_PROPERTY, configName.toString())
        defaultProperties.put(CONFIG_ADDITIONAL_LOCATION_PROPERTY, location.toString())

        return defaultProperties
    }

}