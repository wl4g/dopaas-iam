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
package com.wl4g.iam.core.security.domain;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import com.wl4g.iam.core.config.AbstractIamProperties;
import com.wl4g.iam.core.config.AbstractIamProperties.ParamProperties;

/**
 * HTTP strict transport security filter
 * 
 * @author James Wong &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2020年6月20日 v1.0.0
 * @see
 */
public class HstsSecurityFilter extends OncePerRequestFilter {

    /**
     * {@link Environment}
     */
    protected final Environment environment;

    /**
     * {@link AbstractIamProperties}
     */
    protected final AbstractIamProperties<? extends ParamProperties> config;

    /**
     * Enabled hsts on profiles active. </br>
     * 'Http-Strict-Transport-Security'
     */
    protected final boolean enableHstsOnProfilesActive;

    public HstsSecurityFilter(AbstractIamProperties<? extends ParamProperties> config, Environment environment) {
        this.config = notNullOf(config, "config");
        this.environment = notNullOf(environment, "environment");

        // HTTP Strict-Transport-Security:
        String active = environment.getRequiredProperty("spring.profiles.active");
        Optional<String> opt = safeList(config.getSecurity().getHstsOnProfilesActive()).stream()
                .filter(a -> equalsIgnoreCase(a, active))
                .findAny();
        this.enableHstsOnProfilesActive = opt.isPresent();
    }

    // see:https://owasp.org/www-project-secure-headers/#hsts
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Sets Http-Strict-Transport-Security:
        if (enableHstsOnProfilesActive) {
            if (!response.containsHeader("Strict-Transport-Security")) {
                response.addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                response.addHeader("Strict-Transport-Security", "max-age=0");
            }
        }

        // X-Download-Options:
        // https://stackoverflow.com/questions/15299325/x-download-options-noopen-equivalent
        response.setHeader("X-Download-Options", "noopen");

        filterChain.doFilter(request, response);
    }

}