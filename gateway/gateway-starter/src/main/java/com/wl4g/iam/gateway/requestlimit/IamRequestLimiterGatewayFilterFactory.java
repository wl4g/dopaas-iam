/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wl4g.iam.gateway.requestlimit;

import static com.wl4g.infra.common.serialize.JacksonUtils.parseJSON;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;

import com.wl4g.iam.gateway.requestlimit.config.IamRequestLimiterProperties;
import com.wl4g.iam.gateway.requestlimit.key.IamKeyResolver;
import com.wl4g.iam.gateway.requestlimit.key.IamKeyResolver.KeyResolverProvider;
import com.wl4g.iam.gateway.requestlimit.key.IamKeyResolver.KeyResolverStrategy;
import com.wl4g.iam.gateway.requestlimit.limiter.IamRequestLimiter;
import com.wl4g.iam.gateway.requestlimit.limiter.IamRequestLimiter.RequestLimiterStrategy;
import com.wl4g.iam.gateway.requestlimit.limiter.IamRequestLimiter.RequestLimiterPrivoder;
import com.wl4g.infra.core.framework.operator.GenericOperatorAdapter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import reactor.core.publisher.Mono;

/**
 * {@link IamRequestLimiterGatewayFilterFactory}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-04-19 v3.0.0
 * @since v3.0.0
 * @see {@link org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory}
 */
@Getter
@Setter
@ToString
public class IamRequestLimiterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<IamRequestLimiterGatewayFilterFactory.Config> {
    private static final String EMPTY_KEY = "____EMPTY_KEY__";

    private @Autowired IamRequestLimiterProperties requsetLimiterConfig;
    private @Autowired GenericOperatorAdapter<KeyResolverProvider, IamKeyResolver<? extends KeyResolverStrategy>> keyResolverAdapter;
    private @Autowired GenericOperatorAdapter<RequestLimiterPrivoder, IamRequestLimiter> requestLimiterAdapter;

    public IamRequestLimiterGatewayFilterFactory() {
        super(IamRequestLimiterGatewayFilterFactory.Config.class);
    }

    @Override
    public String name() {
        return BEAN_FILTER_NAME;
    }

    @Override
    public GatewayFilter apply(IamRequestLimiterGatewayFilterFactory.Config config) {
        // TODO use based yaml tags parse?
        KeyResolverStrategy keyStrategy = config.parse().getKeyStrategy();
        applyDefaultToConfig(config, keyStrategy);
        return new IamRequestLimiterGatewayFilter(config, keyStrategy, getKeyResolver(config), getRequestLimiter(config));
    }

    private void applyDefaultToConfig(Config config, KeyResolverStrategy keyStrategy) {
        if (isNull(config.getDenyEmptyKey())) {
            config.setDenyEmptyKey(requsetLimiterConfig.isDenyEmptyKey());
        }
        if (isBlank(config.getEmptyKeyStatusCode())) {
            config.setEmptyKeyStatusCode(requsetLimiterConfig.getEmptyKeyStatusCode());
        }
        if (isBlank(config.getStatusCode())) {
            config.setStatusCode(requsetLimiterConfig.getStatusCode());
        }
        keyStrategy.applyDefaultIfNecessary(requsetLimiterConfig);
    }

    @SuppressWarnings("unchecked")
    private IamKeyResolver<KeyResolverStrategy> getKeyResolver(IamRequestLimiterGatewayFilterFactory.Config config) {
        return (IamKeyResolver<KeyResolverStrategy>) keyResolverAdapter
                .forOperator(config.parse().getKeyStrategy().getProvider());
    }

    private IamRequestLimiter getRequestLimiter(IamRequestLimiterGatewayFilterFactory.Config config) {
        return requestLimiterAdapter.forOperator(config.parse().getLimiterStrategy().getProvider());
    }

    @Getter
    @Setter
    @ToString
    public static class Config {

        /**
         * Switch to deny requests if the Key Resolver returns an empty key,
         * defaults to true.
         */
        private Boolean denyEmptyKey;

        /**
         * HttpStatus to return when denyEmptyKey is true, defaults to
         * FORBIDDEN.
         */
        private String emptyKeyStatusCode;

        /**
         * HttpStatus to return when limiter is true, defaults to
         * TOO_MANY_REQUESTS.
         */
        private String statusCode;

        /**
         * The key resolver strategy JSON configuration.
         */
        private String keyResolverJson;

        /**
         * The request limiter strategy JSON configuration.
         */
        private String limiterJson;

        //
        // Temporary fields.
        //

        @Setter(lombok.AccessLevel.NONE)
        private KeyResolverStrategy keyStrategy;

        @Setter(lombok.AccessLevel.NONE)
        private RequestLimiterStrategy limiterStrategy;

        public Config parse() {
            if (isNull(keyStrategy)) {
                this.keyStrategy = parseJSON(getKeyResolverJson(), KeyResolverStrategy.class);
            }
            if (isNull(limiterStrategy)) {
                this.limiterStrategy = parseJSON(getLimiterJson(), RequestLimiterStrategy.class);
            }
            return this;
        }

    }

    @AllArgsConstructor
    public static class IamRequestLimiterGatewayFilter implements GatewayFilter {
        private final Config config;
        private final KeyResolverStrategy keyStrategy;
        private final IamKeyResolver<KeyResolverStrategy> keyResolver;
        private final IamRequestLimiter requestLimiter;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            return keyResolver.resolve(keyStrategy, exchange).defaultIfEmpty(EMPTY_KEY).flatMap(key -> {
                if (EMPTY_KEY.equals(key)) {
                    if (config.getDenyEmptyKey()) {
                        ServerWebExchangeUtils.setResponseStatus(exchange,
                                HttpStatusHolder.parse(config.getEmptyKeyStatusCode()));
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                }
                Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                return requestLimiter.isAllowed(config, route.getId(), key).flatMap(result -> {
                    // Add limited headers.
                    for (Map.Entry<String, String> header : result.getHeaders().entrySet()) {
                        exchange.getResponse().getHeaders().add(header.getKey(), header.getValue());
                    }
                    if (result.isAllowed()) {
                        return chain.filter(exchange);
                    }
                    ServerWebExchangeUtils.setResponseStatus(exchange, HttpStatusHolder.parse(config.getStatusCode()));
                    return exchange.getResponse().setComplete();
                });
            });
        }
    }

    public static final String BEAN_FILTER_NAME = "IamRequestLimiter";
}
