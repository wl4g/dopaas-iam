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
import org.apache.shiro.util.ByteSource;

import com.wl4g.iam.common.subject.IamPrincipal;
import com.wl4g.iam.core.authc.AbstractIamAuthenticationInfo;

public class SmsAuthenticationInfo extends AbstractIamAuthenticationInfo {
    private static final long serialVersionUID = 1558934819432102687L;

    public SmsAuthenticationInfo(IamPrincipal accountInfo, PrincipalCollection principals, String realmName) {
        this(accountInfo, principals, null, realmName);
    }

    public SmsAuthenticationInfo(IamPrincipal accountInfo, PrincipalCollection principals, ByteSource credentialsSalt,
            String realmName) {
        super(accountInfo, principals, credentialsSalt, realmName);
    }

}