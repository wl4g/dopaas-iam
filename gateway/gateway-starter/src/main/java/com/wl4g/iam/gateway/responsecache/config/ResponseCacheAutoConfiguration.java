/*
 * Copyright 2017 ~ 2025 the original authors James Wong.
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
package com.wl4g.iam.gateway.responsecache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.wl4g.iam.common.constant.GatewayIAMConstants;
import com.wl4g.iam.gateway.config.ReactiveByteArrayRedisTemplate;
import com.wl4g.iam.gateway.metrics.IamGatewayMetricsFacade;
import com.wl4g.iam.gateway.responsecache.ResponseCacheFilterFactory;

/**
 * {@link ResponseCacheAutoConfiguration}
 * 
 * @author James Wong &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-05-11 v3.0.0
 * @since v3.0.0
 */
public class ResponseCacheAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = GatewayIAMConstants.CONF_PREFIX_IAM_GATEWAY_RESPONSECACHE)
    public ResponseCacheProperties responseCacheProperties() {
        return new ResponseCacheProperties();
    }

    @Bean
    public ResponseCacheFilterFactory responseCacheFilterFactory(
            ResponseCacheProperties config,
            ReactiveByteArrayRedisTemplate redisTemplate,
            IamGatewayMetricsFacade metricsFacade) {
        return new ResponseCacheFilterFactory(config, redisTemplate, metricsFacade);
    }

}
