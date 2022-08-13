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

import com.wl4g.iam.common.bean.SocialAuthorizeInfo;

/**
 * Wechat authentication token
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0
 * @date 2018年11月19日
 * @since
 */
public class WechatAuthenticationToken extends Oauth2SnsAuthenticationToken {
    private static final long serialVersionUID = 8587329689973009598L;

    public WechatAuthenticationToken(final String remoteHost, final RedirectInfo redirectInfo, SocialAuthorizeInfo social) {
        super(remoteHost, redirectInfo, social);
    }

}