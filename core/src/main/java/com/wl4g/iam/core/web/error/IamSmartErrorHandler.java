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
package com.wl4g.iam.core.web.error;

import java.util.Map;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.UnknownSessionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.wl4g.infra.core.web.error.AbstractErrorAutoConfiguration.ErrorHandlerProperties;
import com.wl4g.infra.core.web.error.handler.AbstractSmartErrorHandler;

import static com.wl4g.infra.common.lang.Exceptions.*;
import static com.wl4g.infra.common.web.rest.RespBase.RetCode.*;

/**
 * IAM authorization error configuring.
 *
 * @author James Wong &lt;Wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0.0 2019-11-02
 * @since
 */
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class IamSmartErrorHandler extends AbstractSmartErrorHandler {

    public IamSmartErrorHandler(ErrorHandlerProperties config) {
        super(config);
    }

    @Override
    public Integer getStatus(Map<String, Object> model, Throwable th) {
        // IAM Unauthenticated?
        if ((th instanceof UnauthenticatedException) || (th instanceof com.wl4g.iam.core.exception.UnauthenticatedException)) {
            return UNAUTHC.getErrcode();
        }
        // IAM Unauthorized?
        else if ((th instanceof UnauthorizedException) || (th instanceof com.wl4g.iam.core.exception.UnauthorizedException)) {
            return UNAUTHZ.getErrcode();
        }
        // see: IamSecurityHolder
        else if (th instanceof UnknownSessionException) {
            return BAD_PARAMS.getErrcode();
        }
        // next error handler
        return null;
    }

    @Override
    public String getRootCause(Map<String, Object> model, Throwable th) {
        // IAM Unauthenticated or Unauthorized?
        if ((th instanceof UnauthenticatedException) || (th instanceof UnauthorizedException)
                || (th instanceof com.wl4g.iam.core.exception.UnauthenticatedException)
                || (th instanceof com.wl4g.iam.core.exception.UnauthorizedException)) {
            // return getRootCausesString(ex);
            return getMessage(th);
        }
        // next error handler
        return null;
    }

}