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
package com.wl4g.iam.authc;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalMap;

import com.wl4g.iam.common.subject.IamPrincipal;
import com.wl4g.iam.core.authc.IamAuthenticationInfo;

/**
 * {@link EmptyOauth2AuthenicationInfo}
 * 
 * @author James Wong &lt;jameswong1376@gmail.com&gt;
 * @version 2019年07月9日 v1.0.0
 * @see
 */
public class EmptyOauth2AuthenicationInfo implements IamAuthenticationInfo {
    private static final long serialVersionUID = -1824494219125412412L;

    /**
     * Default only instance
     */
    public static final EmptyOauth2AuthenicationInfo EMPTY = new EmptyOauth2AuthenicationInfo();

    /**
     * Empty principal collection
     */
    private static final PrincipalCollection emptyPrincipalCollection = new SimplePrincipalMap();

    @Override
    public PrincipalCollection getPrincipals() {
        return emptyPrincipalCollection;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public IamPrincipal getIamPrincipal() {
        return null;
    }

}