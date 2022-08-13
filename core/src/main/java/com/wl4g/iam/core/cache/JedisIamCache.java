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

import static com.google.common.base.Charsets.UTF_8;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.lang.Assert2.notNull;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static com.wl4g.infra.support.cache.jedis.util.RedisSpecUtil.isSuccess;
import static com.wl4g.iam.core.cache.CacheKey.toKeyBytes;
import static java.util.Collections.singletonMap;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.cache.CacheException;

import com.google.common.base.Charsets;
import com.wl4g.infra.common.log.SmartLogger;
import com.wl4g.infra.support.cache.jedis.JedisClient;
import com.wl4g.iam.core.cache.CacheKey.Serializer;

import redis.clients.jedis.params.SetParams;

/**
 * REDIS enhanced implement cache
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0
 * @date 2018年11月30日
 * @since
 */
public class JedisIamCache implements IamCache {
    protected final SmartLogger log = getLogger(getClass());

    private final String name;
    private final JedisClient jedisClient;

    public JedisIamCache(String name, JedisClient jedisClient) {
        this.name = notNullOf(name, "cacheName");
        this.jedisClient = notNullOf(jedisClient, "jedisClient");
    }

    @Override
    public String getCacheName() {
        return name;
    }

    @Override
    public Object get(final CacheKey key) throws CacheException {
        notNullOf(key, "key");
        notNullOf(key.getValueClass(), "valueClass");
        log.debug("Get key={}", key);

        byte[] data = jedisClient.get(key.getKey(name));
        return key.getSerializer().deserialize(data, key.getValueClass());
    }

    @Override
    public Object put(final CacheKey key, final Object value) throws CacheException {
        notNullOf(key, "key");
        notNullOf(value, "value");
        log.debug("Put key={}, value={}", key, value);

        // Serialization
        byte[] data = key.getSerializer().serialize(value);

        String ret = null;
        if (key.hasExpire()) {
            ret = jedisClient.setex(key.getKey(name), key.getExpire(), data);
        } else {
            ret = jedisClient.set(key.getKey(name), data);
        }
        return String.valueOf(ret).equalsIgnoreCase("nil") ? null : ret;
    }

    @Override
    public Object remove(final CacheKey key) throws CacheException {
        notNull(key, "'key' must not be null");
        log.debug("Remove key={}", key);
        return jedisClient.del(key.getKey(name));
    }

    @Override
    public void clear() throws CacheException {
        if (log.isDebugEnabled()) {
            log.debug("Clear name={}", name);
        }
        jedisClient.hdel(name);
    }

    @Override
    public int size() {
        if (log.isDebugEnabled()) {
            log.debug("Size name={}", name);
        }
        return jedisClient.hlen(name).intValue();
    }

    @Deprecated
    @Override
    public Set<CacheKey> keys() {
        // if (log.isDebugEnabled()) {
        // log.debug("Keys name={}", name);
        // }
        // Set<byte[]> keys = jedisAdapter.hkeys(name);
        // if (keys != null && !keys.isEmpty()) {
        // return keys.stream().map(key -> new
        // EnhancedKey(key)).collect(Collectors.toSet());
        // }
        // return Collections.emptySet();
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Collection<Object> values() {
        // if (log.isDebugEnabled()) {
        // log.debug("Values name={}", name);
        // }
        // Collection<byte[]> vals = jedisAdapter.hvals(name);
        // if (vals != null && !vals.isEmpty()) {
        // return vals.stream().collect(Collectors.toList());
        // }
        // return Collections.emptyList();
        throw new UnsupportedOperationException();
    }

    @Override
    public Long timeToLive(CacheKey key, Object value) throws CacheException {
        notNull(value, "TTL key is null, please check configure");
        notNull(value, "TTL value is null, please check configure");

        byte[] realKey = key.getKey(name);
        // New create.
        if (!jedisClient.exists(realKey)) {
            // key -> createTime
            jedisClient.set(realKey, String.valueOf(value).getBytes(Charsets.UTF_8));
        }

        // Get last TTL expire
        Long lastTTL = jedisClient.ttl(realKey);
        // Less than or equal to 0 means immediate expiration
        if (key.hasExpire()) {
            jedisClient.expire(realKey, key.getExpire());
        }

        return lastTTL;
    }

    @Override
    public Long incrementGet(CacheKey key) throws CacheException {
        return incrementGet(key, 1);
    }

    @Override
    public Long incrementGet(CacheKey key, long incrBy) throws CacheException {
        byte[] realKey = key.getKey(name);
        // Increment
        Long res = jedisClient.incrBy(key.getKey(name), incrBy);
        // Less than or equal to 0 means immediate expiration
        if (key.hasExpire()) {
            jedisClient.expire(realKey, key.getExpire());
        }
        return res;
    }

    @Override
    public Long decrementGet(CacheKey key) throws CacheException {
        return decrementGet(key, 1);
    }

    @Override
    public Long decrementGet(CacheKey key, long decrBy) throws CacheException {
        byte[] realKey = key.getKey(name);
        // Decrement
        Long res = jedisClient.decr(realKey);
        // Less than or equal to 0 means immediate expiration
        if (key.hasExpire()) {
            jedisClient.expire(realKey, key.getExpire());
        }
        return res;
    }

    @Override
    public boolean putIfAbsent(final CacheKey key, final Object value) {
        notNull(key, "'key' must not be null");
        notNull(value, "'value' must not be null");
        log.debug("Put key={}, value={}", key, value);

        // Serialization
        byte[] data = key.getSerializer().serialize(value);

        if (key.hasExpire()) {
            SetParams params = SetParams.setParams().nx().px(key.getExpireMs());
            String res = jedisClient.set(key.getKey(name), data, params);
            return isSuccess(res);
        }
        Long res = jedisClient.setnx(key.getKey(name), data);
        return isSuccess(res);
    }

    // --- Enhanced API. ---

    @Override
    public String mapPut(CacheKey fieldKey, Object fieldValue) {
        notNull(fieldKey, "fieldKey");
        notNull(fieldValue, "fieldValue");
        log.debug("mapPut key={}, value={}", fieldKey, fieldValue);
        return mapPutAll(singletonMap(fieldKey, fieldValue), fieldKey.getExpire(), fieldKey.getSerializer());
    }

    @Override
    public String mapPutAll(Map<Object, Object> map, Serializer serializer) {
        return mapPutAll(map, 0, serializer);
    }

    @Override
    public String mapPutAll(Map<Object, Object> map, int expireSec, Serializer serializer) {
        if (isEmpty(map)) {
            return null;
        }
        log.debug("mapPut map={}", map);

        // Convert to fields map.
        Map<byte[], byte[]> dataMap = map.entrySet().stream().collect(toMap(e -> {
            notNull(e.getKey(), "fieldKey");
            if (e.getKey() instanceof CacheKey) {
                return ((CacheKey) e.getKey()).getKey();
            }
            return serializer.serialize(e.getKey());
        }, e -> {
            notNull(e.getValue(), "fieldValue");
            return serializer.serialize(e.getValue());
        }));
        // Hash map sets
        byte[] mapKey = toKeyBytes(name);
        String res = jedisClient.hmset(mapKey, dataMap);
        if (expireSec > 0) {
            jedisClient.expire(mapKey, expireSec);
        }

        return res;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMapField(CacheKey fieldKey) {
        notNullOf(fieldKey, "fieldKey");
        notNullOf(fieldKey.getValueClass(), "valueClass");

        // Load bytes
        byte[] data = jedisClient.hget(toKeyBytes(name), fieldKey.getKey());
        if (isNull(data)) {
            return null;
        }

        // Deserialization
        return (T) fieldKey.getSerializer().deserialize(data, fieldKey.getValueClass());
    }

    @Override
    public <T> Map<String, T> getMapAll(Class<T> valueClass, Serializer serializer) {
        return safeMap(jedisClient.hgetAll(toKeyBytes(name))).entrySet().stream().collect(
                toMap(e -> new String(e.getKey(), UTF_8), e -> {
                    if (isNull(e.getValue()))
                        return null;
                    return serializer.deserialize(e.getValue(), valueClass);
                }));
    }

    @Override
    public Map<byte[], byte[]> getMapAll() {
        return jedisClient.hgetAll(toKeyBytes(name));
    }

    @Override
    public Long mapRemove(String fieldKey) {
        hasTextOf(fieldKey, "fieldKey");
        return jedisClient.hdel(toKeyBytes(name), toKeyBytes(fieldKey));
    }

    @Override
    public void mapRemoveAll() {
        jedisClient.del(toKeyBytes(name));
    }

    @Override
    public boolean expireMap(int expireSec) {
        Long res = jedisClient.expire(toKeyBytes(name), expireSec);
        return !isNull(res) ? res > 0 : false;
    }

}