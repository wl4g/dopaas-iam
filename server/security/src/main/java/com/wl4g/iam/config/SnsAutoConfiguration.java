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
package com.wl4g.iam.config;

import static com.wl4g.iam.common.constant.FastCasIAMConstants.URI_IAM_SERVER_SNS_BASE;
import static com.wl4g.iam.common.constant.IAMConstants.CONF_PREFIX_IAM_SECURITY_SNS;

import java.util.List;

import org.apache.shiro.cache.CacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.wl4g.iam.annotation.SnsController;
import com.wl4g.iam.config.properties.IamProperties;
import com.wl4g.iam.config.properties.SnsProperties;
import com.wl4g.iam.configure.ServerSecurityConfigurer;
import com.wl4g.iam.configure.ServerSecurityCoprocessor;
import com.wl4g.iam.core.cache.JedisIamCacheManager;
import com.wl4g.iam.core.config.AbstractIamConfiguration;
import com.wl4g.iam.sns.DefaultOAuth2ApiBindingFactory;
import com.wl4g.iam.sns.OAuth2ApiBinding;
import com.wl4g.iam.sns.OAuth2ApiBindingFactory;
import com.wl4g.iam.sns.github.GithubOauth2Template;
import com.wl4g.iam.sns.handler.BindingSnsHandler;
import com.wl4g.iam.sns.handler.ClientAuthcSnsHandler;
import com.wl4g.iam.sns.handler.DelegateSnsHandler;
import com.wl4g.iam.sns.handler.LoginSnsHandler;
import com.wl4g.iam.sns.handler.SecondaryAuthcSnsHandler;
import com.wl4g.iam.sns.handler.SnsHandler;
import com.wl4g.iam.sns.handler.UnBindingSnsHandler;
import com.wl4g.iam.sns.qq.QQOauth2Template;
import com.wl4g.iam.sns.web.GenericOauth2SnsController;
import com.wl4g.iam.sns.wechat.WechatMpOauth2Template;
import com.wl4g.iam.sns.wechat.WechatOauth2Template;
import com.wl4g.iam.sns.wechat.api.WechatMpApiOperator;

/**
 * SNS resource configuration
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0 2019年1月8日
 * @since
 */
@ConditionalOnBean(IamServerMarkerConfiguration.Marker.class)
@AutoConfigureAfter({ IamAutoConfiguration.class })
public class SnsAutoConfiguration extends AbstractIamConfiguration {

    @Bean
    @ConfigurationProperties(prefix = CONF_PREFIX_IAM_SECURITY_SNS)
    public SnsProperties snsProperties() {
        return new SnsProperties();
    }

    //
    // Social provider oauth2 template's
    //

    @Bean
    public GithubOauth2Template githubOauth2Template(
            SnsProperties config,
            @Qualifier(BEAN_IAM_OKHTTP3_REST_TEMPLATE) RestTemplate restTemplate,
            CacheManager cacheManager) {
        return new GithubOauth2Template(config.getGithub(), restTemplate, cacheManager);
    }

    @Bean
    public QQOauth2Template qqOauth2Template(
            SnsProperties config,
            @Qualifier(BEAN_IAM_OKHTTP3_REST_TEMPLATE) RestTemplate restTemplate,
            CacheManager cacheManager) {
        return new QQOauth2Template(config.getQq(), restTemplate, cacheManager);
    }

    @Bean
    public WechatOauth2Template wechatOauth2Template(
            SnsProperties config,
            @Qualifier(BEAN_IAM_OKHTTP3_REST_TEMPLATE) RestTemplate restTemplate,
            CacheManager cacheManager) {
        return new WechatOauth2Template(config.getWechat(), restTemplate, cacheManager);
    }

    @Bean
    public WechatMpOauth2Template wechatMpOauth2Template(
            SnsProperties config,
            @Qualifier(BEAN_IAM_OKHTTP3_REST_TEMPLATE) RestTemplate restTemplate,
            CacheManager cacheManager) {
        return new WechatMpOauth2Template(config.getWechatMp(), restTemplate, cacheManager);
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public OAuth2ApiBindingFactory oAuth2ApiBindingFactory(List<OAuth2ApiBinding> apis) {
        return new DefaultOAuth2ApiBindingFactory(apis);
    }

    //
    // SNS security handler's
    //

    @Bean
    public DelegateSnsHandler delegateSnsHandler(IamProperties config, List<SnsHandler> handlers) {
        return new DelegateSnsHandler(config, handlers);
    }

    @Bean
    public LoginSnsHandler loginSnsHandler(
            IamProperties config,
            SnsProperties snsConfig,
            OAuth2ApiBindingFactory connectFactory,
            ServerSecurityConfigurer context,
            ServerSecurityCoprocessor coprocessor,
            JedisIamCacheManager cacheManager) {
        return new LoginSnsHandler(config, snsConfig, connectFactory, context);
    }

    @Bean
    public ClientAuthcSnsHandler clientAuthcSnsHandler(
            IamProperties config,
            SnsProperties snsConfig,
            OAuth2ApiBindingFactory connectFactory,
            ServerSecurityConfigurer context,
            ServerSecurityCoprocessor coprocessor) {
        return new ClientAuthcSnsHandler(config, snsConfig, connectFactory, context);
    }

    @Bean
    public BindingSnsHandler bindingSnsHandler(
            IamProperties config,
            SnsProperties snsConfig,
            OAuth2ApiBindingFactory connectFactory,
            ServerSecurityConfigurer context,
            ServerSecurityCoprocessor coprocessor) {
        return new BindingSnsHandler(config, snsConfig, connectFactory, context);
    }

    @Bean
    public UnBindingSnsHandler unBindingSnsHandler(
            IamProperties config,
            SnsProperties snsConfig,
            OAuth2ApiBindingFactory connectFactory,
            ServerSecurityConfigurer context,
            ServerSecurityCoprocessor coprocessor) {
        return new UnBindingSnsHandler(config, snsConfig, connectFactory, context);
    }

    @Bean
    public SecondaryAuthcSnsHandler secondAuthcSnsHandler(
            IamProperties config,
            SnsProperties snsConfig,
            OAuth2ApiBindingFactory connectFactory,
            ServerSecurityConfigurer context,
            ServerSecurityCoprocessor coprocessor) {
        return new SecondaryAuthcSnsHandler(config, snsConfig, connectFactory, context);
    }

    //
    // SNS controller's
    //

    @Bean
    public GenericOauth2SnsController genericOauth2SnsController(
            IamProperties config,
            SnsProperties snsConfig,
            DelegateSnsHandler delegate) {
        return new GenericOauth2SnsController(config, snsConfig, delegate);
    }

    @Bean
    public Object snsControllerPrefixHandlerMapping() {
        return super.newPrefixHandlerMapping(URI_IAM_SERVER_SNS_BASE, SnsController.class);
    }

    //
    // SNS api operator's
    //

    @Bean
    public WechatMpApiOperator wechatMpApiOperator(IamProperties config, SnsProperties snsConfig) {
        return new WechatMpApiOperator(config, snsConfig);
    }

}