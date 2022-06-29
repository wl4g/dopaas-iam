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

import static com.wl4g.iam.config.properties.IamProperties.DEFAULT_JSSDK_BASE_URI;
import static com.wl4g.iam.config.properties.IamProperties.DEFAULT_JSSDK_LOCATION;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wl4g.iam.web.jssdk.JssdkWebResourceController;
import com.wl4g.infra.core.web.embed.EmbedWebappAutoConfiguration.SimpleEmbedWebappProperties;
import com.wl4g.infra.core.web.mapping.PrefixHandlerMappingSupport;

/**
 * Iam web jssdk auto configuration.
 * 
 * @author James Wong &lt;Wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0.0 2019-10-20
 * @since
 */
@Configuration
@ConditionalOnBean(IamServerMarkerConfiguration.Marker.class)
public class JssdkWebappAutoConfiguration extends PrefixHandlerMappingSupport {

    @Bean
    public JssdkWebResourceController jssdkWebappsEndpoint() {
        return new JssdkWebResourceController(new SimpleEmbedWebappProperties(DEFAULT_JSSDK_BASE_URI, DEFAULT_JSSDK_LOCATION));
    }

    @Bean
    @ConditionalOnBean(JssdkWebResourceController.class)
    public Object jssdkWebappEndpointPrefixHandlerMapping(JssdkWebResourceController jssdk) {
        return super.newPrefixHandlerMapping(DEFAULT_JSSDK_BASE_URI, jssdk);
    }

}