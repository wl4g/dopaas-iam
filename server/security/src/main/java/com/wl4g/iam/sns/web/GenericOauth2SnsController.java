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
package com.wl4g.iam.sns.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.wl4g.infra.common.web.rest.RespBase;
import static com.wl4g.infra.common.web.rest.RespBase.RetCode.OK;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_IAM_SERVER_AFTER_CALLBACK_AGENT;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_IAM_SERVER_SNS_CALLBACK;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_IAM_SERVER_SNS_CONNECT;

import com.wl4g.iam.annotation.SnsController;
import com.wl4g.iam.config.properties.IamProperties;
import com.wl4g.iam.config.properties.SnsProperties;
import com.wl4g.iam.core.config.AbstractIamProperties.Which;
import com.wl4g.iam.sns.CallbackResult;
import com.wl4g.iam.sns.handler.DelegateSnsHandler;

import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.apache.shiro.web.util.WebUtils.getCleanParam;
import static org.apache.shiro.web.util.WebUtils.issueRedirect;
import static java.lang.String.format;
import static com.google.common.base.Charsets.UTF_8;
import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;
import static com.wl4g.infra.common.web.WebUtils2.getFullRequestURI;
import static com.wl4g.infra.common.web.WebUtils2.safeDecodeURL;
import static com.wl4g.infra.common.web.WebUtils2.toQueryParams;
import static com.wl4g.infra.common.web.WebUtils2.ResponseType.isRespJSON;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;

/**
 * Default oauth2 social networking services controller
 *
 * @author James Wong <983708408@qq.com>
 * @version v1.0
 * @date 2019年1月2日
 * @since
 */
@SnsController
public class GenericOauth2SnsController extends AbstractSnsController {

    final public static String DEFAULT_AUTHC_READY_STATUS = "CertificateReady";
    final public static String DEFAULT_SECOND_AUTHC_STATUS = "SecondCertifies";

    public GenericOauth2SnsController(IamProperties config, SnsProperties snsConfig, DelegateSnsHandler delegate) {
        super(config, snsConfig, delegate);
    }

    /**
     * Request to connect to social service provider preprocessing.</br>
     * 
     * <pre>
     * 示例1：(获取配置微信公众号view类型菜单的URL)
     * 请求此接口(工具接口)：http://iam.example.com/iam-web/sns/connect/wechatmp?which=client_auth&service=wechatMp
     * 
     * 返回：
     * https://open.weixin.qq.com/connect/oauth2/authorize?state=a7d2e06d3c05483a8feeaa0bdf37455a&scope=snsapi_userinfo&redirect_uri=http%3A%2F%2Fiam.example.com%2Fiam-web%2Fsns%2Fwechatmp%2Fcallback%3Fwhich%3Dclient_auth%26service%3DwechatMp&response_type=code&appid=wxec3f74a4062d650f#wechat_redirect
     * </pre>
     * 
     * @param provider
     *            social networking provider id
     * @param which
     *            action
     * @param state
     *            Oauth2 protocol state (which can be imported when the WeChat
     *            public platform is used for manual configuration)
     * @param request
     *            HttpServletRequest
     * @param response
     *            HttpServletResponse
     * @return
     * @throws IOException
     */
    @GetMapping("/" + URI_IAM_SERVER_SNS_CONNECT + "/{" + PARAM_SNS_PRIVIDER + "}")
    public void connect(
            @PathVariable(PARAM_SNS_PRIVIDER) String provider,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        log.info("called:connect from '{}', url='{}'", request.getRemoteHost(), getFullRequestURI(request));

        // Basic parameters
        String which = getCleanParam(request, config.getParam().getWhich());
        String state = getCleanParam(request, config.getParam().getState());

        // Extra parameters all.(Note: Form submission parameters will be
        // ignored)
        Map<String, String> connectParams = toQueryParams(request.getQueryString());

        // Getting SNS authorizingUrl
        String authorizingUrl = delegate.doGetAuthorizingUrl(Which.of(which), provider, state, connectParams);

        // Response type
        if (isRespJSON(request)) {
            RespBase<String> resp = RespBase.create();
            resp.withCode(OK).withStatus(DEFAULT_AUTHC_READY_STATUS).setMessage("Obtain the SNS authorization code is ready.");
            writeJson(response, toJSONString(resp));
        } else {
            // Some handler have carried the 'redirect:' prefix
            if (startsWithIgnoreCase(authorizingUrl, REDIRECT_PREFIX)) {
                String redirect_uri = authorizingUrl.substring(REDIRECT_PREFIX.length());
                log.info("redirect:connect provider='{}', state='{}', redirect_uri='{}'", provider, state, redirect_uri);
                issueRedirect(request, response, redirect_uri, null, false);
            } else {
                // Return the URL string directly without redirect
                String html = format(
                        "<p style='text-align:center'>The following is the OAuth2 configuration address, Please configure this URL to the platform.</p><hr/>"
                                + "<p><b>Social Service Provider:</b>&nbsp;%s</p>"
                                + "<p><b>Oauth2 URL:</b>&nbsp;<a style='word-break:break-all;' href='%s' target='_blank'>%s</a></p>",
                        provider, authorizingUrl, authorizingUrl);
                log.info("rendering:connect provider='{}', state='{}', html='{}'", provider, state, html);
                write(response, HttpServletResponse.SC_OK, MediaType.TEXT_HTML_VALUE, html.getBytes(UTF_8));
            }
        }
    }

    /**
     * Used to process callback request after social service provider completes
     * oauth2 authorization.</br>
     * 
     * <pre>
     * 示例1：
     * 
     * step1: 设置微信公众号view类型菜单的URL，如：
     * APPID=yours appid
     * REDIRECT_URL=https://iam.example.com/sso/sns/wechatmp/callback?which=client_auth&state=1
     * https://open.weixin.qq.com/connect/oauth2/authorize?appid={APPID}&redirect_uri={REDIRECT_URL}&response_type=code&scope=snsapi_base#wechat_redirect
     * 
     * step2: 点击它，此时微信将发起回调请求，如：
     * https://iam.example.com/iam-web/sns/wechatmp/callback?which=client_auth&state=1&code=011Z9B7G1SkEh60IE38G1jpG7G1Z9B71
     * 
     * </pre>
     * 
     * @param provider
     *            social networking provider id
     * @param code
     *            oauth2 callback authorization code
     * @param request
     * @param response
     * @throws Exception
     */
    @GetMapping("/{" + PARAM_SNS_PRIVIDER + "}/" + URI_IAM_SERVER_SNS_CALLBACK)
    public void callback(
            @PathVariable(PARAM_SNS_PRIVIDER) String provider,
            @NotBlank @RequestParam(PARAM_SNS_CODE) String code,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        log.info("called:callback from '{}', url='{}'", request.getRemoteHost(), getFullRequestURI(request));

        // Get required parameters
        Which which = Which.of(getCleanParam(request, config.getParam().getWhich()));
        String state = getCleanParam(request, config.getParam().getState());

        CallbackResult ret = delegate.doCallback(which, provider, state, code, request);

        // When refresh redirectUrl is blank, indicating that no redirect is
        // required for this operation.
        if (ret.hasRefreshUri()) {
            log.info("redirect:callback provider='{}', state='{}', redirect_uri='{}'", provider, state, ret.getRefreshUri());
            issueRedirect(request, response, safeDecodeURL(ret.getRefreshUri()), null, false);
        } else {
            RespBase<String> resp = RespBase.create(DEFAULT_SECOND_AUTHC_STATUS);
            resp.withCode(OK).withMessage("Second authenticate successfully.");
            // resp.setData(singletonMap(config.getParam().getRefreshUri(),redirectRefreshUrl));
            log.info("resp:callback provider='{}', state='{}', resp='{}'", ret.getRefreshUri(), provider, state, resp);
            writeJson(response, toJSONString(resp));
        }
    }

    /**
     * After the SNS callback, it is used to jump to the middle page of the home
     * page.
     * <p>
     * {@link com.wl4g.iam.sns.web.GenericOauth2SnsController#callback()}
     *
     * @param response
     * @param refreshUrl
     *            Actual after callback refresh URL
     * @throws IOException
     */
    @GetMapping(URI_IAM_SERVER_AFTER_CALLBACK_AGENT)
    public void afterCallbackAgent(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> callbackParams = toQueryParams(request.getQueryString());
        String attributeJsonstr = toJSONString(callbackParams).replaceAll("\\\"", "\\\\\"");
        // Readering agent page
        byte[] agentPageHtml = format(TEMPLATE_CALLBACK_AGENT, attributeJsonstr).getBytes(UTF_8);
        write(response, HttpStatus.OK.value(), MediaType.TEXT_HTML_VALUE, agentPageHtml);
    }

}