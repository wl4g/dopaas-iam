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
package com.wl4g.iam.core.cache;

import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.jedis.util.RedisSpecUtil.safeFormat;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;

import com.wl4g.infra.common.jedis.JedisClient;

/**
 * RedisCache Manager implements let Shiro use Redis caching
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0
 * @time 2017年4月13日
 * @since
 */
public class JedisIamCacheManager implements IamCacheManager {
    private final Map<String, IamCache> caching = new ConcurrentHashMap<>(16);

    private String prefix;
    private JedisClient jedisClient;

    public JedisIamCacheManager(String prefix, JedisClient jedisClient) {
        notNullOf(prefix, "prefix");
        this.prefix = safeFormat(prefix, '_'); // e.g. iam-web => iam_server
        this.jedisClient = notNullOf(jedisClient, "jedisClient");
    }

    public JedisClient getJedisClient() {
        return jedisClient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Cache<CacheKey, Object> getCache(String name) throws CacheException {
        return getIamCache(name);
    }

    /**
     * Getting enhanced cache instance
     *
     * @param name
     * @return
     * @throws CacheException
     */
    @Override
    public IamCache getIamCache(String name) throws CacheException {
        String cacheName = getCacheName(name);
        IamCache cache = caching.get(cacheName);
        if (Objects.isNull(cache)) {
            caching.put(cacheName, (cache = new JedisIamCache(cacheName, jedisClient)));
        }
        return cache;
    }

    private final String getCacheName(String name) {
        return prefix + name;
    }

}