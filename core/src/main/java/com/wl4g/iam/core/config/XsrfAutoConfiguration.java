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
package com.wl4g.iam.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_XSRF_BASE;
import static com.wl4g.iam.core.config.AbstractIamConfiguration.ORDER_XSRF_PRECEDENCE;
import static com.wl4g.iam.core.config.XsrfProperties.KEY_XSRF_PREFIX;

import com.wl4g.infra.core.web.mapping.PrefixHandlerMappingSupport;
import com.wl4g.iam.core.annotation.XsrfController;
import com.wl4g.iam.core.security.xsrf.RequiresXsrfMatcher;
import com.wl4g.iam.core.security.xsrf.XsrfProtectionSecurityFilter;
import com.wl4g.iam.core.security.xsrf.handler.DefaultXsrfRejectHandler;
import com.wl4g.iam.core.security.xsrf.handler.XsrfRejectHandler;
import com.wl4g.iam.core.security.xsrf.repository.CookieXsrfTokenRepository;
import com.wl4g.iam.core.web.XsrfSecurityController;

/**
 * XSRF protection auto configuration.
 *
 * @author James Wong <983708408@qq.com>
 * @version v1.0 2020年05月06日
 * @since
 */
public class XsrfAutoConfiguration extends PrefixHandlerMappingSupport {

    //
    // X S R F _ F I L T E R _ C O N F I G's.
    //

    @Bean
    @ConditionalOnProperty(name = KEY_XSRF_PREFIX + ".enabled", matchIfMissing = false)
    @ConfigurationProperties(prefix = KEY_XSRF_PREFIX)
    public XsrfProperties xsrfProperties() {
        return new XsrfProperties();
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    public CookieXsrfTokenRepository cookieXsrfTokenRepository() {
        return new CookieXsrfTokenRepository();
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    @ConditionalOnMissingBean(XsrfRejectHandler.class)
    public DefaultXsrfRejectHandler defaultXsrfRejectHandler() {
        return new DefaultXsrfRejectHandler();
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    public XsrfProtectionSecurityFilter xsrfProtectionSecurityFilter() {
        return new XsrfProtectionSecurityFilter();
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    public RequiresXsrfMatcher requiresXsrfMatcher() {
        return new RequiresXsrfMatcher();
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    public FilterRegistrationBean<XsrfProtectionSecurityFilter> xsrfProtectionSecurityFilterBean(
            XsrfProtectionSecurityFilter filter) {
        // Register XSRF filter
        FilterRegistrationBean<XsrfProtectionSecurityFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(ORDER_XSRF_PRECEDENCE);
        // Cannot use '/*' or it will not be added to the container chain (only
        // '/**')
        filterBean.addUrlPatterns("/*");
        return filterBean;
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    public XsrfSecurityController xsrfProtectionEndpoint() {
        return new XsrfSecurityController();
    }

    @Bean
    @ConditionalOnBean(XsrfProperties.class)
    public Object xsrfProtectionEndpointPrefixHandlerMapping() {
        return super.newPrefixHandlerMapping(URI_XSRF_BASE, XsrfController.class);
    }

}