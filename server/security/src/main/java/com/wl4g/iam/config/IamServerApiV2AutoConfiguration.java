/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
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
 */
package com.wl4g.iam.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wl4g.iam.core.config.GenericApiAutoConfiguration;
import com.wl4g.iam.web.api.V2ServiceApiController;

/**
 * Generic API v1 auto configuration.
 *
 * @author James Wong <983708408@qq.com>
 * @version v1.0 2019年1月8日
 * @since
 */
@Configuration
@ConditionalOnBean(IamServerMarkerConfiguration.Marker.class)
@AutoConfigureAfter({ IamAutoConfiguration.class })
public class IamServerApiV2AutoConfiguration extends GenericApiAutoConfiguration {

    @Bean
    public V2ServiceApiController iamServerApiV2Controller() {
        return new V2ServiceApiController();
    }

}