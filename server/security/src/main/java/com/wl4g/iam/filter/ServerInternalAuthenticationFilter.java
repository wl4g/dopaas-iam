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
package com.wl4g.iam.filter;

import com.wl4g.iam.core.annotation.IamFilter;
import com.wl4g.iam.core.config.AbstractIamProperties;
import com.wl4g.iam.core.config.AbstractIamProperties.ParamProperties;
import com.wl4g.iam.core.filter.AbstractWhiteListInternalAuthenticationFilter;

import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_IAM_SERVER_BASE;

import com.wl4g.infra.core.kit.access.IPAccessControl;

/**
 * Interactive authentication processing filter for internal and application
 * services
 * <p>
 * {@link org.apache.shiro.web.filter.authz.HostFilter}
 *
 * @author James Wong <983708408@qq.com>
 * @version v1.0
 * @date 2018年11月30日
 * @since
 */
@IamFilter
public class ServerInternalAuthenticationFilter extends AbstractWhiteListInternalAuthenticationFilter {
    final public static String NAME = "serverInternalFilter";

    public ServerInternalAuthenticationFilter(IPAccessControl control, AbstractIamProperties<? extends ParamProperties> config) {
        super(control, config);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUriMapping() {
        return URI_IAM_SERVER_BASE + "/**";
    }

}