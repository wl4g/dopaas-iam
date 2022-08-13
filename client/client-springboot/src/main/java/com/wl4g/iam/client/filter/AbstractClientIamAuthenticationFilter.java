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
package com.wl4g.iam.client.filter;

import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.lang.Exceptions.getRootCausesString;
import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;
import static com.wl4g.infra.common.web.UserAgentUtils.isBrowser;
import static com.wl4g.infra.common.web.WebUtils2.applyQueryURL;
import static com.wl4g.infra.common.web.WebUtils2.cleanURI;
import static com.wl4g.infra.common.web.WebUtils2.getRFCBaseURI;
import static com.wl4g.infra.common.web.WebUtils2.safeEncodeURL;
import static com.wl4g.infra.common.web.WebUtils2.writeJson;
import static com.wl4g.infra.common.web.WebUtils2.ResponseType.isRespJSON;
import static com.wl4g.infra.common.web.rest.RespBase.RetCode.OK;
import static com.wl4g.infra.common.web.rest.RespBase.RetCode.UNAUTHC;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.CACHE_PREFIX_IAM_TICKET_SERVER;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_ACCESSTOKEN_SIGN_NAME;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_DATA_CIPHER_NAME;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_SERVICE_ROLE;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_SERVICE_ROLE_VALUE_IAMCLIENT;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_SESSIONINFO_NAME;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_AUTHENTICATOR;
import static com.wl4g.iam.core.utils.IamAuthenticatingUtils.generateAccessToken;
import static com.wl4g.iam.core.utils.IamAuthenticatingUtils.sessionStatus;
import static com.wl4g.iam.core.utils.IamSecurityHolder.bind;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getBindValue;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getSession;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getSessionId;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getSessionRemainingTime;
import static com.wl4g.iam.core.utils.cumulate.CumulateHolder.newSessionCumulator;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.endsWithAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.apache.shiro.util.Assert.notNull;
import static org.apache.shiro.web.util.WebUtils.getCleanParam;
import static org.apache.shiro.web.util.WebUtils.issueRedirect;
import static org.apache.shiro.web.util.WebUtils.toHttp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.Cookie;

import com.wl4g.infra.common.web.WebUtils2;
import com.wl4g.infra.common.web.rest.RespBase;
import com.wl4g.infra.core.web.error.handler.AbstractSmartErrorHandler;
import com.wl4g.iam.client.authc.FastCasAuthenticationToken;
import com.wl4g.iam.client.authc.LogoutAuthenticationToken;
import com.wl4g.iam.client.config.IamClientProperties;
import com.wl4g.iam.client.configure.ClientSecurityConfigurer;
import com.wl4g.iam.client.configure.ClientSecurityCoprocessor;
import com.wl4g.iam.core.cache.CacheKey;
import com.wl4g.iam.core.cache.IamCache;
import com.wl4g.iam.core.cache.JedisIamCacheManager;
import com.wl4g.iam.core.exception.InvalidGrantTicketException;
import com.wl4g.iam.core.exception.TooManyRequestAuthentcationException;
import com.wl4g.iam.core.exception.UnauthorizedException;
import com.wl4g.iam.core.filter.BasedRiskIamAuthenticationFilter;
import com.wl4g.iam.core.utils.cumulate.Cumulator;
import com.wl4g.iam.core.web.model.SessionInfo;
import com.wl4g.iam.core.web.servlet.IamCookie;

/**
 * This filter validates the CAS service ticket to authenticate the user. It
 * must be configured on the URL recognized by the CAS server. For example, in
 * {@code shiro.ini}:
 * 
 * <pre>
 * [main]
 * casFilter = org.apache.shiro.cas.CasFilter
 * ...
 *
 * [urls]
 * /shiro-cas = casFilter
 * ...
 * </pre>
 * 
 * (example : http://host:port/mycontextpath/shiro-cas)
 *
 * @since 1.2
 */
public abstract class AbstractClientIamAuthenticationFilter<T extends AuthenticationToken>
        extends BasedRiskIamAuthenticationFilter<IamClientProperties> {

    protected final AbstractSmartErrorHandler errorConfigurer;

    /**
     * Client security context handler.
     */
    protected final ClientSecurityConfigurer configurer;

    /**
     * Client security processor.
     */
    protected final ClientSecurityCoprocessor coprocessor;

    /**
     * Client ticket distributed cache
     */
    protected final IamCache clientTicketCache;

    /**
     * Accumulator used to restrict redirection authentication.
     */
    protected final Cumulator failedCumulator;

    public AbstractClientIamAuthenticationFilter(AbstractSmartErrorHandler errorConfigurer, ClientSecurityConfigurer configurer,
            ClientSecurityCoprocessor coprocessor, JedisIamCacheManager cacheManager) {
        this.errorConfigurer = notNullOf(errorConfigurer, "errorConfigurer");
        this.configurer = notNullOf(configurer, "clientSecurityConfigurer");
        this.coprocessor = notNullOf(coprocessor, "clientSecurityCoprocessor");

        notNull(cacheManager, "'cacheManager' must not be null");
        this.clientTicketCache = cacheManager.getIamCache(CACHE_PREFIX_IAM_TICKET_SERVER);
        this.failedCumulator = newSessionCumulator(KEY_TRY_REDIRECT_AUTHC, 10_000L);
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        return doCreateToken(toHttp(request), toHttp(response));
    }

    protected abstract T doCreateToken(HttpServletRequest request, HttpServletResponse response) throws Exception;

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        return super.executeLogin(request, response);
    }

    @Override
    protected boolean onLoginSuccess(AuthenticationToken token, Subject subject, ServletRequest request, ServletResponse response)
            throws Exception {
        FastCasAuthenticationToken ftoken = (FastCasAuthenticationToken) token;
        notNull(ftoken.getCredentials(), "token.credentials(grant ticket) must not be null");

        // Grant ticket
        String grantTicket = (String) ftoken.getCredentials();

        /*
         * Binding session => grantTicket. Synchronize with
         * FastCasAuthorizingRealm#doGetAuthenticationInfo
         */
        bind(SAVE_GRANT_TICKET, grantTicket);
        log.debug("Authentication bind grantTicket[{}], sessionId[{}]", grantTicket, getSessionId(subject));

        /**
         * Binding grantTicket => sessionId. Synchronize with
         * IamClientSessionManager#getSessionId
         */
        long expiredMs = getSessionRemainingTime();
        clientTicketCache.put(new CacheKey(grantTicket, expiredMs), valueOf(getSessionId(subject)));

        // Determine success URL
        String successUrl = determineSuccessRedirectUrl(ftoken, subject, request, response);

        // JSON response
        if (isRespJSON(toHttp(request))) {
            try {
                // Make logged response JSON.
                RespBase<String> resp = makeLoggedResponse(token, request, response, subject, successUrl);

                // Call custom success handle.
                coprocessor.postAuthenticatingSuccess(ftoken, subject, toHttp(request), toHttp(response), resp.forMap());

                String logged = toJSONString(resp);
                log.info("Authenticated resp to - {}", resp);

                writeJson(toHttp(response), logged);
            } catch (IOException e) {
                log.error("Logged response json error", e);
            }
        }
        // Redirections(Native page).
        else {
            // Sets secret tokens to cookies.
            putSuccessTokensCookieIfNecessary(token, request, response);
            // Sets authorization info to cookies.
            putAuthzInfoCookiesAndSecurityIfNecessary(token, request, response);
            // Sets refresh xsrf token.
            putXsrfTokenCookieIfNecessary(token, request, response);

            // Call custom success handle.
            coprocessor.postAuthenticatingSuccess(ftoken, subject, toHttp(request), toHttp(response), null);

            log.info("Authenticated redirect to - {}", successUrl);
            issueRedirect(request, response, successUrl);
        }

        return false;
    }

    @Override
    protected boolean onLoginFailure(
            AuthenticationToken token,
            AuthenticationException ae,
            ServletRequest request,
            ServletResponse response) {
        Throwable exroot = getRootCause(ae);
        String errmsg = nonNull(exroot) ? exroot.getMessage() : null;
        String tip = format("Failed to authentication of token: %s", token);
        if (WebUtils2.isStacktraceRequest(request)) {
            log.error(tip, ae);
        } else {
            log.error("{}, caused by: {}", tip, errmsg);
        }

        // Failure redirect URL
        String failRedirectUrl = null;
        try {
            failRedirectUrl = makeFailureRedirectUrl(token, ae, toHttp(request), toHttp(response));
        } catch (TooManyRequestAuthentcationException ex) {
            exroot = ex;
        }

        // Post handle of authenticate failure.
        coprocessor.postAuthenticatingFailure(token, ae, request, response);

        // Only if the error is not authenticated, can it be redirected to the
        // IAM server login page, otherwise the client will display the error
        // page directly (to prevent unlimited redirection).
        /** See:{@link AbstractBasedIamValidator#doGetRemoteValidation()} */
        if (isNull(exroot) || (exroot instanceof InvalidGrantTicketException)) { // InvalidGrantTicketException
            if (isRespJSON(toHttp(request))) {
                try {
                    RespBase<Object> resp = makeFailedResponse(token, failRedirectUrl, exroot);
                    String failMsg = toJSONString(resp);
                    log.debug("Failure authentication response: {}", failMsg);

                    writeJson(toHttp(response), failMsg);
                } catch (IOException e) {
                    log.error("Response json error", e);
                }
            } else { // Redirects the login page direct.
                try {
                    issueRedirect(request, response, failRedirectUrl);
                } catch (IOException e) {
                    log.error("Cannot redirect to failure url - {}", failRedirectUrl, e);
                }
            }
        }
        // For example, because of interface or permission errors, there is no
        // need to carry redirection URLs, because even redirection is the same
        // error, which may lead to unlimited redirection.
        else {
            try {
                /**
                 * See:{@link com.wl4g.infra.core.web.error.com.wl4g.infra.core.web.error.ReactiveSmartErrorHandler#renderErrorResponse()}
                 * See:{@link com.wl4g.infra.core.web.error.ServletSmartErrorHandler#doAnyHandleError()}
                 */
                toHttp(response).sendError(errorConfigurer.getStatus(exroot), format("%s, %s",
                        bundle.getMessage("AbstractAuthenticationFilter.authc.failure"), getRootCausesString(exroot)));
            } catch (IOException e) {
                log.error("Failed to response error", e);
            }
        }

        // Suspend of execution. otherwise, dispatcherServlet will perform
        // redirection and eventually result in an exception:Cannot call
        // sendRedirect() after the response has been committed
        return false;
    }

    /**
     * Make failure redirect URL
     * 
     * @param token
     * @param cause
     * @param request
     * @return
     */
    protected String makeFailureRedirectUrl(
            AuthenticationToken token,
            Throwable cause,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (cause instanceof UnauthorizedException) { // Unauthorized error?
            return config.getUnauthorizedUri();
        } else { // Unauthenticated or other error.
            /**
             * Check to block multiple invalid redirects.
             * 
             * @see {@link com.wl4g.devops.iam.client.realm.FastCasAuthorizingRealm#doAuthenticationInfo(AuthenticationToken)#MARK1}
             */
            if (nonNull(token.getPrincipal())) {
                List<String> factors = singletonList(valueOf(token.getPrincipal()));
                // When the IamServer redirects the request, but the
                // grantTicket validate failed, infinite redirect needs to
                // be prevented.
                if ((cause instanceof InvalidGrantTicketException) && failedCumulator.accumulate(factors, 1) > 5) {
                    throw new TooManyRequestAuthentcationException(format("Too many redirect request authenticating"));
                }
            }

            // Redirect parameters.
            Map<String, String> clientUrlParams = new HashMap<>();
            clientUrlParams.put(config.getParam().getApplication(), config.getServiceName());

            // URL to redirect when IamServer authenticated is successful.
            String clientRedirectUrl = getRFCBaseURI(request, true) + URI_AUTHENTICATOR;
            String clientParamStr = request.getQueryString();
            if (!isBlank(clientParamStr)) {
                // When accessing client1 to client2, it may have parameters,
                // which need to be kept in the redirection parameters.
                clientRedirectUrl += "?" + clientParamStr;
            }
            clientUrlParams.put(config.getParam().getRedirectUrl(), safeEncodeURL(clientRedirectUrl));

            // Custom decorate failure parameters.
            customFailureRedirectParams(token, cause, request, clientUrlParams);

            // Determinte failure loginUrl
            String determinedLoginUrl = configurer.decorateAuthenticateFailureUrl(getLoginUrl(), token, cause, request, response);

            // Build to query URL.
            return applyQueryURL(determinedLoginUrl, clientUrlParams);
        }
    }

    /**
     * Custom authenticate failure redirection parameters.
     * 
     * @param token
     * @param cause
     * @param request
     * @param params
     */
    protected void customFailureRedirectParams(
            AuthenticationToken token,
            Throwable cause,
            HttpServletRequest request,
            Map<String, String> params) {
    }

    /**
     * determine success redirect URL
     * 
     * @param token
     * @param subject
     * @param request
     * @param response
     * @return
     */
    protected String determineSuccessRedirectUrl(
            AuthenticationToken token,
            Subject subject,
            ServletRequest request,
            ServletResponse response) {
        // Priority obtain redirectURL from request.
        String successUrl = getRedirectUrl(request);
        if (isBlank(successUrl)) {
            // Secondary get remembered redirectURL.
            successUrl = getClearSavedRememberUrl(toHttp(request));
            if (isBlank(successUrl)) {
                // Fallback get the configured redirectURL as the default.
                successUrl = config.getSuccessUri();
            }
        }

        // Determine successUrl.
        successUrl = configurer.decorateAuthenticateSuccessUrl(successUrl, token, subject, request, response);
        notNull(successUrl, "'successUrl' must not be null");
        return cleanURI(successUrl); // Check & cleanup.
    }

    /**
     * Gets the URL from the redirectUrl from the authentication
     * request(flexible API).
     * 
     * @return
     */
    protected String getRedirectUrl(ServletRequest request) {
        return getCleanParam(request, config.getParam().getRedirectUrl());
    }

    /**
     * Gets remember last request URL
     * 
     * @param request
     * @return
     */
    protected String getClearSavedRememberUrl(HttpServletRequest request) {
        // Use remember redirection.
        if (config.isUseRememberRedirect()) {
            String rememberUrl = getBindValue(KEY_REMEMBER_URL, true);
            if (isNotBlank(rememberUrl)) {
                // URL excluding redirection remember
                if (!endsWithAny(rememberUrl, EXCLOUDE_SAVED_REDIRECT_URLS)) {
                    return rememberUrl;
                }
            }
        }

        return null;
    }

    /**
     * Make logged-in response message.
     * 
     * @see {@link com.wl4g.devops.iam.filter.AbstractIamAuthenticationFilter#makeLoggedResponse()}
     * @param request
     *            Servlet request
     * @param response
     * @param redirectUri
     *            login success redirect URL
     * @return
     */
    protected RespBase<String> makeLoggedResponse(
            AuthenticationToken token,
            ServletRequest request,
            ServletResponse response,
            Subject subject,
            String redirectUri) {
        hasTextOf(redirectUri, "redirectUri");

        // Make successful message
        RespBase<String> resp = RespBase.create(sessionStatus());
        resp.withCode(OK).withMessage("Authentication successful");
        resp.forMap().put(config.getParam().getRedirectUrl(), redirectUri);
        resp.forMap().put(config.getParam().getApplication(), config.getServiceName());
        resp.forMap().put(KEY_SERVICE_ROLE, KEY_SERVICE_ROLE_VALUE_IAMCLIENT);
        // Iam-client session info.
        resp.forMap().put(KEY_SESSIONINFO_NAME, new SessionInfo(config.getParam().getSid(), valueOf(getSessionId(subject))));

        // Sets secret tokens to cookies.
        String[] tokens = putSuccessTokensCookieIfNecessary(token, request, response);

        // Sets child dataCipherKey. (if necessary)
        resp.forMap().put(config.getParam().getDataCipherKeyName(), tokens[0]);
        // Sets child accessToken. (if necessary)
        resp.forMap().put(config.getParam().getAccessTokenName(), tokens[1]);
        // Sets authorization info.
        resp.forMap().putAll(putAuthzInfoCookiesAndSecurityIfNecessary(token, request, response));
        // Sets refresh xsrf token.
        // resp.forMap().putAll(putXsrfTokenCookieIfNecessary(token, request,
        // response));
        putXsrfTokenCookieIfNecessary(token, request, response);

        return resp;
    }

    /**
     * Make login failed response message.
     * 
     * @param token
     *            e.g, {@link LogoutAuthenticationToken}
     * @param loginRedirectUrl
     *            Login redirect URL
     * @param err
     *            Exception object
     * @return
     * @see {@link com.wl4g.devops.iam.filter.AbstractIamAuthenticationFilter#makeFailedResponse()}
     */
    protected RespBase<Object> makeFailedResponse(AuthenticationToken token, String loginRedirectUrl, Throwable err) {
        String errmsg = !isNull(err) ? err.getMessage() : "Authentication failure";

        // Make failed message
        RespBase<Object> resp = RespBase.create(sessionStatus());
        resp.withCode(UNAUTHC).withMessage(errmsg);
        resp.forMap().put(config.getParam().getRedirectUrl(), loginRedirectUrl);
        resp.forMap().put(config.getParam().getApplication(), config.getServiceName());
        resp.forMap().put(KEY_SERVICE_ROLE, KEY_SERVICE_ROLE_VALUE_IAMCLIENT);
        return resp;
    }

    /**
     * Puts secret and tokens/signature to cookies handle.
     * 
     * @param token
     * @param request
     * @param response
     * @return
     */
    protected String[] putSuccessTokensCookieIfNecessary(
            AuthenticationToken token,
            ServletRequest request,
            ServletResponse response) {
        // Sets child dataCipherKeys to cookie.
        String childDataCipherKey = getBindValue(KEY_DATA_CIPHER_NAME);
        if (!isBlank(childDataCipherKey) && isBrowser(toHttp(request))) {
            Cookie c = new IamCookie(config.getCookie());
            c.setName(config.getParam().getDataCipherKeyName());
            c.setValue(childDataCipherKey);
            c.saveTo(toHttp(request), toHttp(response));
        }

        // Sets child accessToken to cookie.
        String childAccessToken = generateChildAccessTokenIfNecessary();
        if (!isBlank(childAccessToken) && isBrowser(toHttp(request))) {
            Cookie c = new IamCookie(config.getCookie());
            c.setName(config.getParam().getAccessTokenName());
            c.setValue(childAccessToken);
            c.saveTo(toHttp(request), toHttp(response));
        }

        return new String[] { childDataCipherKey, childAccessToken };
    }

    /**
     * Generation child accessToken.
     * 
     * @return
     */
    protected final String generateChildAccessTokenIfNecessary() {
        // Gets child accessTokenSign key.
        String childAccessTokenSignKey = getBindValue(KEY_ACCESSTOKEN_SIGN_NAME);
        // Generate child accessToken
        return isBlank(childAccessTokenSignKey) ? null : generateAccessToken(getSession(), childAccessTokenSignKey);
    }

    public abstract String getName();

    public static final String SAVE_GRANT_TICKET = AbstractClientIamAuthenticationFilter.class.getSimpleName() + ".GRANT_TICKET";

    /**
     * What kind of URL request does not need to be remembered (i.e. using the
     * default successUrl) when using the function of recording login successful
     * callback URLs ? <br/>
     * (For example, jump to IAM/login.html after executing logout)
     */
    public static final String[] EXCLOUDE_SAVED_REDIRECT_URLS = { ("/" + LogoutAuthenticationFilter.NAME) };

    /**
     * Remember last request URL.
     */
    public static final String KEY_REMEMBER_URL = AbstractClientIamAuthenticationFilter.class.getSimpleName() + ".IamRememberUrl";

    /**
     * Redirection authentication failure retry upper limit key.
     */
    public static final String KEY_TRY_REDIRECT_AUTHC = "TryRedirectAuthenticating";

}