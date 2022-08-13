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
package com.wl4g.iam.captcha.verify;

import com.wl4g.infra.common.crypto.asymmetric.spec.KeyPairSpec;
import com.wl4g.iam.captcha.gif.Captcha;
import com.wl4g.iam.captcha.gif.GifCaptcha;
import com.wl4g.iam.captcha.gif.model.GifApplyImgModel;
import com.wl4g.iam.captcha.gif.model.GifVerifyImgModel;
import com.wl4g.iam.crypto.SecureCryptService.CryptKind;
import com.wl4g.iam.verify.BaseGraphSecurityVerifier;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static com.wl4g.infra.common.serialize.JacksonUtils.parseJSON;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * GIF CAPTCHA verification handler.
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0
 * @date 2018年12月28日
 * @since
 */
public class GifSecurityVerifier extends BaseGraphSecurityVerifier {

    @Override
    public VerifyKind kind() {
        return VerifyKind.GRAPH_GIF;
    }

    @Override
    protected Object postApplyGraphProperties(
            @NotNull CryptKind kind,
            String applyToken,
            VerifyCodeWrapper codeWrap,
            KeyPairSpec keySpec) throws IOException {
        // Generate image & to base64 string.
        Captcha captcha = new GifCaptcha(codeWrap.getCode());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        captcha.out(out);

        // Build model
        GifApplyImgModel model = new GifApplyImgModel(applyToken, kind().getAlias());
        model.setPrimaryImg(convertToBase64(out.toByteArray()));
        return model;
    }

    @Override
    protected Object getRequestVerifyCode(@NotBlank String params, @NotNull HttpServletRequest request) {
        GifVerifyImgModel model = parseJSON(params, GifVerifyImgModel.class);
        validator.validate(model);
        return model;
    }

}