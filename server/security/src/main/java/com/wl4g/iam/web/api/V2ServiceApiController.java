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
package com.wl4g.iam.web.api;

import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_AUTHC_TOKEN;
import static com.wl4g.iam.handler.fastcas.FastCasServerAuthenticatingHandler.getGrantCredentials;
import static java.util.Objects.nonNull;

import org.springframework.web.bind.annotation.ResponseBody;

import com.wl4g.iam.authc.GenericAuthenticationToken;
import com.wl4g.iam.authc.Oauth2SnsAuthenticationToken;
import com.wl4g.iam.authc.WechatMpAuthenticationToken;
import com.wl4g.iam.core.annotation.V2APIController;
import com.wl4g.iam.core.authc.ClientRef;
import com.wl4g.iam.core.session.GrantCredentialsInfo;
import com.wl4g.iam.core.session.IamSession;
import com.wl4g.iam.core.session.IamSession.RelationAttrKey;
import com.wl4g.iam.core.web.BaseApiController;
import com.wl4g.iam.core.web.model.SessionAttributeModel;
import com.wl4g.iam.core.web.model.SessionAttributeModel.IamSessionInfo;

/**
 * IAM server API controller.
 *
 * @author James Wong <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2019年10月31日
 * @since
 */
@V2APIController
@ResponseBody
public class V2ServiceApiController extends BaseApiController {

    /**
     * Convert wrap {@link IamSession} to {@link SessionAttributeModel}. </br>
     * </br>
     *
     * <b>Origin {@link IamSession} json string example:</b>
     *
     * <pre>
     *    {
     * 	  "code": 200,
     * 	  "status": "Normal",
     * 	  "requestId": null,
     * 	  "message": "ok",
     * 	  "data": {
     * 	    "sessions": [
     *          {
     * 	        "id": "sid4c034ff4e95741dcb3b20f687c952cd4",
     * 	        "startTimestamp": 1572593959441,
     * 	        "stopTimestamp": null,
     * 	        "lastAccessTime": 1572593993963,
     * 	        "timeout": 1800000,
     * 	        "expired": false,
     * 	        "host": "0:0:0:0:0:0:0:1",
     * 	        "attributes": {
     * 	          "org.apache.shiro.subject.support.DefaultSubjectContext_AUTHENTICATED_SESSION_KEY": true,
     * 	          "authcTokenAttributeKey": {
     * 	            "principals": {
     * 	              "empty": false,
     * 	              "primaryPrincipal": "root",
     * 	              "realmNames": [
     * 	                "com.wl4g.devops.iam.realm.GeneralAuthorizingRealm_0"
     * 	              ]
     *                },
     * 	            "credentials": "911ef082b5de81151ba25d8442efb6e77bb380fd36ac349ee737ee5461ae6d3e8a13e4366a20e6dd71f95e8939fe375e203577568297cdbc34d598dd47475a7c",
     * 	            "credentialsSalt": null,
     * 	            "accountInfo": {
     * 	              "principal": "root",
     * 	              "storedCredentials": "911ef082b5de81151ba25d8442efb6e77bb380fd36ac349ee737ee5461ae6d3e8a13e4366a20e6dd71f95e8939fe375e203577568297cdbc34d598dd47475a7c"
     *                }
     *              },
     * 	          "CentralAuthenticationHandler.GRANT_TICKET": {
     * 	            "applications": {
     * 	              "umc-manager": "stzgotzYWGdweoBGgEOtDKpXwJsxyEaqCrttfMSgFMYkZuIWrDWNpzPYWFa"
     *                }
     *              },
     * 	          "org.apache.shiro.subject.support.DefaultSubjectContext_PRINCIPALS_SESSION_KEY": {
     * 	            "empty": false,
     * 	            "primaryPrincipal": "root",
     * 	            "realmNames": [
     * 	              "com.wl4g.devops.iam.realm.GeneralAuthorizingRealm_0"
     * 	            ]
     *              }
     *            }
     *          }
     * 	    ]
     *      }
     *    }
     * </pre>
     *
     * @param session
     * @return
     */
    @Override
    protected IamSessionInfo convertIamSessionInfo(IamSession session) {
        IamSessionInfo sa = super.convertIamSessionInfo(session);

        // Authentication grant applications.
        GrantCredentialsInfo info = (GrantCredentialsInfo) getGrantCredentials(session);
        if (nonNull(info) && info.hasEmpty()) {
            sa.setGrants(info.getGrantApps().keySet());
        }

        // Authentication client type.
        Object token = session.getAttribute(new RelationAttrKey(KEY_AUTHC_TOKEN, IamSession.class));
        if (nonNull(token)) {
            if (token instanceof GenericAuthenticationToken) {
                sa.setClientRef(((GenericAuthenticationToken) token).getClientRef());
            } else if ((token instanceof WechatMpAuthenticationToken)) {
                sa.setClientRef(ClientRef.WeChatMp);
            }
            // Only when oauth2 authorizes authentication
            if ((token instanceof Oauth2SnsAuthenticationToken)) {
                sa.setOauth2Provider(((Oauth2SnsAuthenticationToken) token).getSocial().getProvider());
            }
        }

        return sa;
    }

}