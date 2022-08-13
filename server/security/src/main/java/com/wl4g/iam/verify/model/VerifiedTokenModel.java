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
package com.wl4g.iam.verify.model;

/**
 * 
 * {@link VerifiedTokenModel}
 * 
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0 2019-09-04
 * @since
 */
public class VerifiedTokenModel {

    /**
     * Verify CAPTCHA verified model key-name.
     */
    final public static String KEY_VWEIFIED_RESULT = "verifiedModel";

    private boolean verified;

    private String verifiedToken;

    public VerifiedTokenModel(boolean verified, String verifiedToken) {
        this.verified = verified;
        this.verifiedToken = verifiedToken;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerifiedToken() {
        return verifiedToken;
    }

    public void setVerifiedToken(String verifiedToken) {
        this.verifiedToken = verifiedToken;
    }
}