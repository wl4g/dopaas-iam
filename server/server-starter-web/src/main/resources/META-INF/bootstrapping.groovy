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
import static org.springframework.boot.context.config.ConfigFileApplicationListener.*

import org.springframework.boot.Banner

import com.wl4g.infra.core.boot.listener.IBootstrappingConfigurer

/**
 * IAM web implementation of {@link IBootstrappingConfigurer}
 */
class IamWebBootstrappingConfigurer implements IBootstrappingConfigurer {

    @Override
    def int getOrder() {
        return -100
    }

    def Banner.Mode bannerMode(Banner.Mode prevMode) {
        return Banner.Mode.LOG;
    }

    @Override
    void defaultProperties(Properties prevDefaultProperties) {
        // for example: spring auto load for 'application-dev.yml/application-data-dev.yml'
        def configName = new StringBuffer("application,iam-web,iam-web-etc")

        // Preset spring.config.location
        // for example: spring auto load for 'classpath:/application-web-dev.yml'
        def location = new StringBuffer("classpath:/")
        if (isPresent("org.springframework.cloud.openfeign.FeignClient") && isPresent("org.springframework.cloud.openfeign.FeignAutoConfiguration")) {
            configName.append(",iam-web-scf");
            location.append(",classpath:/scf/")
        } else if (isPresent("com.wl4g.infra.integration.feign.core.annotation.FeignConsumer")) {
            configName.append(",iam-web-sbf");
            location.append(",classpath:/sbf/")
        } else if (isPresent("com.alibaba.dubbo.rpc.Filter") && isPresent("com.alibaba.boot.dubbo.autoconfigure.DubboAutoConfiguration")) {
            configName.append(",iam-web-dubbo");
            location.append(",classpath:/dubbo/")
        }

        // Preset 'spring.config.additional-location', external resources does not override resources in classpath.
        prevDefaultProperties.put(CONFIG_NAME_PROPERTY, configName.toString())
        prevDefaultProperties.put(CONFIG_ADDITIONAL_LOCATION_PROPERTY, location.toString())
    }

}