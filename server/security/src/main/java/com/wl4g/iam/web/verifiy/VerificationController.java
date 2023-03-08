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
package com.wl4g.iam.web.verifiy;

import com.wl4g.infra.common.web.rest.RespBase;
import com.wl4g.infra.common.framework.operator.NoSuchOperatorException;
import com.wl4g.iam.annotation.VerifyAuthController;
import com.wl4g.iam.core.exception.AccessRejectedException;
import com.wl4g.iam.verify.CompositeSecurityVerifierAdapter;
import com.wl4g.iam.verify.SecurityVerifier.VerifyCodeWrapper;
import com.wl4g.iam.verify.SmsSecurityVerifier.MobileNumber;
import com.wl4g.iam.verify.model.VerifiedTokenModel;
import com.wl4g.iam.web.BaseIamController;
import com.wl4g.iam.web.login.model.SmsCheckModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import static com.wl4g.infra.common.web.WebUtils2.getHttpRemoteAddr;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.*;
import static com.wl4g.iam.core.utils.IamAuthenticatingUtils.sessionStatus;
import static com.wl4g.iam.core.utils.RiskSecurityUtils.*;
import static com.wl4g.iam.verify.SecurityVerifier.VerifyKind.TEXT_SMS;
import static com.wl4g.iam.verify.SmsSecurityVerifier.MobileNumber.parse;
import static com.wl4g.iam.verify.model.VerifiedTokenModel.*;
import static com.wl4g.iam.web.login.model.SmsCheckModel.KEY_SMS_CHECK;
import static org.apache.shiro.web.util.WebUtils.getCleanParam;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * IAM verifier authenticator controller.
 *
 * @author wangl.sir
 * @version v1.0 2019年8月22日
 * @since
 */
@VerifyAuthController
public class VerificationController extends BaseIamController {

    /**
     * Verify CAPTCHA apply model key-name.
     */
    public static final String KEY_APPLY_RESULT = "applyModel";

    /**
     * Composite verifier handler.
     */
    @Autowired
    protected CompositeSecurityVerifierAdapter verifier;

    /**
     * Apply CAPTCHA.
     *
     * @param param
     *            CAPTCHA parameter, required
     * @param request
     * @param response
     */
    @RequestMapping(value = URI_IAM_SERVER_VERIFY_APPLY_CAPTCHA, method = { POST })
    @ResponseBody
    public RespBase<?> applyCaptcha(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("called:applyCaptcha '{}' from '{}'", URI_IAM_SERVER_VERIFY_APPLY_CAPTCHA, request.getRemoteHost());

        RespBase<Object> resp = RespBase.create(sessionStatus());
        if (!coprocessor.preApplyCapcha(request, response)) {
            throw new AccessRejectedException(bundle.getMessage("AbstractAttemptsMatcher.accessReject"));
        }

        // LoginId number or mobileNum(Optional)
        String principal = getCleanParam(request, config.getParam().getPrincipalName());
        // Limit factors
        List<String> factors = getV1Factors(getHttpRemoteAddr(request), principal);

        // Apply CAPTCHA
        if (verifier.forOperator(request).isEnabled(factors)) { // Enabled?
            resp.forMap().put(KEY_APPLY_RESULT, verifier.forOperator(request).apply(principal, factors, request));
        } else { // Invalid requestVERIFIED_TOKEN_EXPIREDMS
            log.warn("Invalid request, no captcha enabled, factors: {}", factors);
        }

        return resp;
    }

    /**
     * Verify CAPTCHA code.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = URI_IAM_SERVER_VERIFY_ANALYSIS_CAPTCHA, method = { POST })
    @ResponseBody
    public RespBase<?> verifyAnalysis(String verifyData, HttpServletRequest request) throws Exception {
        log.debug("called:applyCaptcha '{}' from '{}', verifyData: {}", URI_IAM_SERVER_VERIFY_ANALYSIS_CAPTCHA,
                request.getRemoteHost(), verifyData);
        RespBase<Object> resp = RespBase.create(sessionStatus());

        // LoginId number or mobileNum(Optional)
        String principal = getCleanParam(request, config.getParam().getPrincipalName());
        // Limit factors
        List<String> factors = getV1Factors(getHttpRemoteAddr(request), principal);
        // Verifying
        String verifiedToken = verifier.forOperator(request).verify(verifyData, request, factors);
        resp.forMap().put(KEY_VWEIFIED_RESULT, new VerifiedTokenModel(true, verifiedToken));

        return resp;
    }

    /**
     * Apply verification code
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws NoSuchOperatorException
     */
    @RequestMapping(value = URI_IAM_SERVER_VERIFY_SMS_APPLY, method = { POST })
    @ResponseBody
    public RespBase<?> applySmsCode(HttpServletRequest request, HttpServletResponse response)
            throws NoSuchOperatorException, IOException {
        log.debug("called:applySmsCode '{}' from '{}'", URI_IAM_SERVER_VERIFY_SMS_APPLY, request.getRemoteHost());
        RespBase<Object> resp = RespBase.create(sessionStatus());

        if (!coprocessor.preApplySmsCode(request, response)) {
            throw new AccessRejectedException(bundle.getMessage("AbstractAttemptsMatcher.accessReject"));
        }

        // Login account number or mobile number(Required)
        MobileNumber mn = parse(getCleanParam(request, config.getParam().getPrincipalName()));
        // Lock factors
        List<String> factors = getV1Factors(getHttpRemoteAddr(request), mn.asNumberText());

        // Graph validation
        verifier.forOperator(request).validate(factors, getCleanParam(request, config.getParam().getVerifiedTokenName()), false);

        // Apply SMS verify code.
        resp.forMap().put(KEY_APPLY_RESULT, verifier.forOperator(TEXT_SMS).apply(mn.asNumberText(), factors, request));

        // The creation time of the currently created SMS authentication
        // code (must exist).
        VerifyCodeWrapper code = verifier.forOperator(TEXT_SMS).getVerifyCode(true);
        resp.forMap().put(KEY_SMS_CHECK,
                new SmsCheckModel(mn.getNumber(), code.getRemainDelay(config.getMatcher().getFailFastSmsDelay())));

        return resp;
    }

}