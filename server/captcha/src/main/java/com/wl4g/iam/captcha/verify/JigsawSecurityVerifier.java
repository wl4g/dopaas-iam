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

import com.wl4g.infra.common.codec.CheckSums;
import com.wl4g.infra.common.crypto.asymmetric.spec.KeyPairSpec;
import com.wl4g.infra.core.framework.operator.GenericOperatorAdapter;
import com.wl4g.iam.captcha.config.CaptchaProperties;
import com.wl4g.iam.captcha.jigsaw.JigsawImageManager;
import com.wl4g.iam.captcha.jigsaw.ImageTailor.TailoredImage;
import com.wl4g.iam.captcha.jigsaw.model.JigsawApplyImgModel;
import com.wl4g.iam.captcha.jigsaw.model.JigsawVerifyImgModel;
import com.wl4g.iam.core.session.IamSession.RelationAttrKey;
import com.wl4g.iam.crypto.SecureCryptService;
import com.wl4g.iam.crypto.SecureCryptService.CryptKind;
import com.wl4g.iam.verify.BaseGraphSecurityVerifier;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static java.lang.String.format;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;

import java.util.List;
import java.util.Objects;

import static com.wl4g.infra.common.codec.Encodes.encodeBase64;
import static com.wl4g.infra.common.serialize.JacksonUtils.parseJSON;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getBindValue;

/**
 * JIGSAW slider CAPTCHA verification handler.
 * 
 * @author James Wong
 * @version v1.0 2019年8月28日
 * @since
 */
public class JigsawSecurityVerifier extends BaseGraphSecurityVerifier {

    /**
     * JIGSAW image manager.
     */
    @Autowired
    protected JigsawImageManager jigsawManager;

    /**
     * Secure asymmetric cryptographic service.
     */
    @Autowired
    protected GenericOperatorAdapter<CryptKind, SecureCryptService> cryptAdapter;

    /**
     * CAPTCHA configuration.
     */
    @Autowired
    protected CaptchaProperties capConfig;

    @Override
    public VerifyKind kind() {
        return VerifyKind.GRAPH_JIGSAW;
    }

    @Override
    protected Object postApplyGraphProperties(
            @NotNull CryptKind kind,
            String graphToken,
            VerifyCodeWrapper codeWrap,
            KeyPairSpec keySpec) {
        TailoredImage code = codeWrap.getCode();
        // Build model
        JigsawApplyImgModel model = new JigsawApplyImgModel(graphToken, kind().getAlias());
        model.setY(code.getY());
        model.setPrimaryImg(encodeBase64(code.getPrimaryImg()));
        model.setBlockImg(encodeBase64(code.getBlockImg()));
        model.setSecret(keySpec.getPubHexString());
        return model;
    }

    @Override
    protected Object generateCode() {
        // To improve performance, only image indexes are saved here
        return jigsawManager.randomBorrowIndex(); // #MARK1,@see:MARK2
    }

    @Override
    public VerifyCodeWrapper getVerifyCode(boolean assertion) {
        VerifyCodeWrapper wrap = super.getVerifyCode(assertion);
        wrap.setCode(jigsawManager.borrow(wrap.getCode())); // #MARK2,@see:MARK1
        return wrap;
    }

    @Override
    protected Object getRequestVerifyCode(@NotBlank String params, @NotNull HttpServletRequest request) {
        JigsawVerifyImgModel model = parseJSON(params, JigsawVerifyImgModel.class);
        validator.validate(model);
        return model;
    }

    @Override
    final protected boolean doMatch(@NotNull CryptKind kind, VerifyCodeWrapper storedCode, Object submitCode) {
        TailoredImage code = (TailoredImage) storedCode.getCode();
        JigsawVerifyImgModel model = (JigsawVerifyImgModel) submitCode;

        // Analyze & verification JIGSAW image.
        boolean matched = doAnalyzingJigsawGraph(kind, code, model);
        log.info("Jigsaw match result: {}, storedCode: {}, submitCode: {}", matched, code.toString(), model.toString());
        return matched;
    }

    /**
     * Analyzing & verification JIGSAW graph.
     * 
     * @param request
     * @param code
     * @param model
     * @return
     */
    final private boolean doAnalyzingJigsawGraph(@NotNull CryptKind kind, TailoredImage code, JigsawVerifyImgModel model) {
        if (Objects.isNull(model.getX())) {
            log.warn("VerifyJigsaw image x-postition is empty. - {}", model);
            return false;
        }

        SecureCryptService cryptService = cryptAdapter.forOperator(kind);
        // Gets applied verifier KeyPairSpec. #MARK21
        KeyPairSpec keyPairSpec = getBindValue(new RelationAttrKey(model.getApplyToken(), cryptService.getKeyPairSpecClass()),
                true);
        // Decryption slider block x-position.
        String plainX = cryptService.decrypt(keyPairSpec.getKeySpec(), model.getX());

        hasText(plainX, "Invalid x-position, unable to resolve.");
        // Parsing additional algorithmic salt.
        isTrue(plainX.length() > 66, format("Failed to analyze jigsaw, illegal additional ciphertext. '%s'", plainX));
        log.debug("Jigsaw analyze decrypt plain x-position: {}, cipher x-position: {}", plainX, model.getX());

        // Reduction analysis.
        final int prototypeX = parseAdditionalWithAlgorithmicSalt(plainX, model);

        // --- Offset analyzing. ---
        final boolean offsetMatched = Math.abs(prototypeX - code.getX()) <= capConfig.getJigsaw().getAllowOffsetX();

        // --- Simple trails analyzing. ---
        // X-standardDeviation
        final List<Integer> xTrails = model.getTrails().stream().map(v -> v.getX()).filter(v -> Objects.nonNull(v)).collect(
                toList());
        final double xSD = analyzingStandartDeviation(xTrails);
        log.debug("Simple AI-smart trails analyze, xSD: {}, xTrails: {}", xSD, xTrails);

        // Y-standardDeviation
        final List<Integer> yTrails = model.getTrails().stream().map(v -> v.getY()).filter(v -> Objects.nonNull(v)).collect(
                toList());
        final double ySD = analyzingStandartDeviation(yTrails);
        log.debug("Simple AI-smart trails analyze, xSD: {}, yTrails: {}", ySD, yTrails);

        // (At present, the effect is not very good.)
        // TODO => for AI CNN model verifying...
        return offsetMatched /* && xSD > 13 && xSD < 79 &&ySD>1.3&&ySD<11 */;
    }

    /**
     * Parse additionalWith algorithmic salt.
     * 
     * @param plainX
     * @param model
     * @return
     */
    final private int parseAdditionalWithAlgorithmicSalt(String plainX, JigsawVerifyImgModel model) {
        try {
            final int tmp0 = Integer.parseInt(plainX.substring(66));
            final long tmp1 = CheckSums.crc16String(model.getApplyToken());
            return (int) (tmp0 / tmp1);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't parse additional alg salt.", e);
        }
    }

    /**
     * Calculation analyzing standard deviation.
     * 
     * @param model
     * @return
     */
    final private double analyzingStandartDeviation(@NotNull List<Integer> trails) {
        final double xAvg = trails.stream().filter(t -> Objects.nonNull(t)).collect(summarizingDouble(v -> v)).getAverage();
        // Deviation
        final Double xD = trails.stream()
                .map(v -> Math.pow(Math.abs(v - xAvg), 2))
                .reduce((acc, v) -> acc += v)
                .map(v -> v / trails.size())
                .get();
        // StandardDeviation
        return Math.sqrt(xD);
    }

}