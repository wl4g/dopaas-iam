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
package com.wl4g.iam.session.mgt;

import static com.wl4g.iam.common.constant.FastCasIAMConstants.CACHE_PREFIX_IAM_TICKET_CLIENT;

import java.io.Serializable;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.wl4g.iam.config.properties.IamProperties;
import com.wl4g.iam.core.cache.IamCacheManager;
import com.wl4g.iam.core.session.mgt.AbstractIamSessionManager;

/**
 * Custom WEB session manager
 *
 * @author James Wong <983708408@qq.com>
 * @version v1.0
 * @date 2018年11月29日
 * @since
 */
public class IamServerSessionManager extends AbstractIamSessionManager<IamProperties> {

    public IamServerSessionManager(IamProperties config, IamCacheManager cacheManager) {
        super(config, cacheManager, CACHE_PREFIX_IAM_TICKET_CLIENT);
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response) {
        return super.getSessionId(request, response);
    }

}