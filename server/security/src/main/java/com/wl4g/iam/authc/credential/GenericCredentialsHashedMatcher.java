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
package com.wl4g.iam.authc.credential;

import java.util.List;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationToken;

import com.wl4g.iam.authc.GenericAuthenticationToken;
import com.wl4g.iam.authc.VerifyAuthenticationToken;
import com.wl4g.iam.authc.credential.secure.CredentialsToken;
import com.wl4g.iam.core.authc.IamAuthenticationInfo;
import com.wl4g.iam.core.authc.IamAuthenticationToken;

/**
 * General UserName Password credential matcher
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0
 * @date 2018年11月29日
 * @since
 */
public class GenericCredentialsHashedMatcher extends BasedAttemptsLockedMatcher {

    @Override
    public boolean doMatching(IamAuthenticationToken token, IamAuthenticationInfo info, List<String> factors) {
        GenericAuthenticationToken tk = (GenericAuthenticationToken) token;
        // preCheck.
        if (!coprocessor.preAuthenticatingAllowed(tk, info)) {
            throw new AccountException(bundle.getMessage("ServerSecurityCoprocessor.accessDenied", tk.getPrincipal()));
        }

        // Matching credentials.
        CredentialsToken crToken = new CredentialsToken((String) tk.getPrincipal(), (String) tk.getCredentials(),
                tk.getCryptKind());
        return securer.validate(crToken, info);
    }

    @Override
    protected void assertRequestVerify(AuthenticationToken token, String principal, List<String> factors) {
        if (token instanceof VerifyAuthenticationToken) {
            VerifyAuthenticationToken tk = ((VerifyAuthenticationToken) token);
            verifier.forOperator(tk.getVerifyType()).validate(factors, tk.getVerifiedToken(), false);
        }
    }

}