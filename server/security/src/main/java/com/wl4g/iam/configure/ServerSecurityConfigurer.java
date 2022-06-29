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
package com.wl4g.iam.configure;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

import com.wl4g.iam.authc.ServerIamAuthenticationToken.RedirectInfo;
import com.wl4g.iam.common.bean.FastCasClientInfo;
import com.wl4g.iam.common.bean.SocialConnectInfo;
import com.wl4g.iam.common.bean.OidcClient;
import com.wl4g.iam.common.bean.RealmBean;
import com.wl4g.iam.common.subject.IamPrincipal;
import com.wl4g.iam.common.subject.IamPrincipal.Parameter;
import com.wl4g.iam.core.authc.IamAuthenticationToken;
import com.wl4g.iam.core.configure.SecurityConfigurer;
import com.wl4g.iam.core.exception.BindingConstraintsException;

/**
 * IAM server security context handler
 *
 * @author James Wong <983708408@qq.com>
 * @version v1.0
 * @date 2018年12月21日
 * @since
 */
public interface ServerSecurityConfigurer extends SecurityConfigurer {

    //
    // B A S E D _ M E T H O D
    //

    /**
     * Get application information by name
     *
     * @param appName
     *            application name
     * @return aplication information
     */
    FastCasClientInfo getFastCasClientInfo(String appName);

    /**
     * Find application information list by names
     *
     * @param appNames
     *            application names
     * @return aplication information
     */
    List<FastCasClientInfo> findFastCasClientInfo(String... appNames);

    /**
     * Gets authenticating user account details information based on loginId
     *
     * @param parameter
     *            query parameter
     * @return account information
     */
    IamPrincipal getIamUserDetail(Parameter parameter);

    /**
     * When the authentication succeeds, but there is no access to the Iam
     * client application, this method will be called fallback to gets the
     * redirection URL
     *
     * @param token
     *            Authentication token.
     * @param defaultRedirect
     *            Default redirection information for configuration.
     * @return
     */
    default RedirectInfo getFallbackRedirectInfo(IamAuthenticationToken token, RedirectInfo defaultRedirect) {
        return defaultRedirect;
    }

    //
    // A U T H O R I Z I N G _ M E T H O D
    //

    /**
     * Check whether the principal has access to an application.<br/>
     * In fact, it's application-level privilege control.<br/>
     * For example, User1 can access App1 and App2, but User2 can only access
     * App1
     *
     * @param principal
     *            principal
     * @param application
     *            application name
     * @return If principal is allowed to access the application, TRUE is
     *         returned
     */
    boolean isApplicationAccessAuthorized(String principal, String application);

    //
    // S N S _ M E T H O D
    //

    /**
     * Query social connections list.
     *
     * @param principal
     *            login principal
     * @param provider
     *            social platform provider
     * @return
     */
    <T extends SocialConnectInfo> List<T> findSocialConnections(String principal, String provider);

    /**
     * Save(bind) social connection information
     *
     * @param social
     * @throws BindingConstraintsException
     */
    void bindSocialConnection(SocialConnectInfo social) throws BindingConstraintsException;

    /**
     * Delete(UnBind) social connection
     *
     * @param social
     * @throws BindingConstraintsException
     */
    void unbindSocialConnection(SocialConnectInfo social) throws BindingConstraintsException;

    //
    // R E A L M / O I D C _ M E T H O D
    //

    RealmBean loadRealm(@Nullable Long realmId, @Nullable String realmName);

    OidcClient loadOidcClient(@NotBlank String clientId);

}