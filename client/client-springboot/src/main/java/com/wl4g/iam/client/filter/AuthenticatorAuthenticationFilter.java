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
package com.wl4g.iam.client.filter;

import static com.wl4g.infra.common.web.WebUtils2.getFullRequestURL;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_AUTHENTICATOR;
import static org.apache.shiro.web.util.WebUtils.toHttp;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.wl4g.infra.core.web.error.handler.AbstractSmartErrorHandler;
import com.wl4g.iam.client.configure.ClientSecurityConfigurer;
import com.wl4g.iam.client.configure.ClientSecurityCoprocessor;
import com.wl4g.iam.core.annotation.IamFilter;
import com.wl4g.iam.core.cache.JedisIamCacheManager;

/**
 * IAM client authenticator authentication filter
 * 
 * @author wangl.sir
 * @version v1.0 2019年3月12日
 * @since
 */
@IamFilter
public class AuthenticatorAuthenticationFilter extends ROOTAuthenticationFilter {
    final public static String NAME = "authenticatorFilter";

    public AuthenticatorAuthenticationFilter(AbstractSmartErrorHandler errorHandler, ClientSecurityConfigurer context,
            ClientSecurityCoprocessor coprocessor, JedisIamCacheManager cacheManager) {
        super(errorHandler, context, coprocessor, cacheManager);
    }

    /**
     * Access is not allowed to handle duplicate authentication requests
     * (http://passport.domain/com/devops-iam/authenticator), as this will
     * result in 404 errors.<br/>
     * Final execution: super#executeLogin()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        log.info("Request of: {}", () -> getFullRequestURL(toHttp(request)));
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUriMapping() {
        return URI_AUTHENTICATOR;
    }

}