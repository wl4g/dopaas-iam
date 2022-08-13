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
package com.wl4g.iam.captcha.jigsaw;

import com.wl4g.infra.common.log.SmartLogger;
import com.wl4g.infra.support.cache.locks.JedisLockManager;
import com.wl4g.iam.captcha.config.CaptchaProperties;
import com.wl4g.iam.captcha.jigsaw.ImageTailor.TailoredImage;
import com.wl4g.iam.core.cache.CacheKey;
import com.wl4g.iam.core.cache.IamCache;
import com.wl4g.iam.core.cache.IamCacheManager;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static com.wl4g.infra.common.codec.Encodes.toBytes;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.lang.Assert2.state;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.CACHE_PREFIX_IAM_VERIFY_JIGSAW_IMG;
import static io.netty.util.internal.ThreadLocalRandom.current;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.exception.ExceptionUtils.wrapAndThrow;
import static org.springframework.util.Assert.notNull;

/**
 * JIGSAW image manager.
 * 
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0 2019-09-02
 * @since
 */
public class JigsawImageManager implements ApplicationRunner, Serializable {
    private static final long serialVersionUID = -4133013721883654349L;

    final protected SmartLogger log = getLogger(getClass());

    /**
     * CAPTCHA configuration properties.
     */
    final protected CaptchaProperties config;

    /**
     * Simple lock manager.
     */
    final protected Lock lock;

    /**
     * Iam distributed cache .
     */
    final protected IamCache imgCache;

    public JigsawImageManager(CaptchaProperties config, IamCacheManager cacheManager, JedisLockManager lockManager) {
        notNullOf(cacheManager, "cacheManager");
        notNullOf(lockManager, "lockManager");
        this.config = notNullOf(config, "captchaProperties");
        this.lock = lockManager.getLock(getClass().getSimpleName(), DEFAULT_JIGSAW_INIT_TIMEOUTMS, TimeUnit.MILLISECONDS);
        this.imgCache = cacheManager.getIamCache(CACHE_PREFIX_IAM_VERIFY_JIGSAW_IMG);
    }

    @Override
    public void run(ApplicationArguments arg0) throws Exception {
        initJigsawImagesBuffer();
    }

    /**
     * Get random borrow JIGSAW image code.
     * 
     * @return
     */
    public int randomBorrowIndex() {
        return current().nextInt(config.getJigsaw().getPoolImgSize());
    }

    /**
     * Gets random borrow JIGSAW image code.
     * 
     * @return
     */
    public TailoredImage borrow() {
        return borrow(-1);
    }

    /**
     * Gets random borrow JIGSAW image code.
     * 
     * @param index
     * @return
     */
    public TailoredImage borrow(int index) {
        if (index < 0 || index >= config.getJigsaw().getPoolImgSize()) {
            int _index = randomBorrowIndex();
            log.debug("Borrow jigsaw index '{}' of out bound, used random index '{}'", index, _index);
            index = _index;
        }

        // Load JIGSAW image by index.
        TailoredImage img = imgCache.getMapField(new CacheKey(index, TailoredImage.class));
        if (isNull(img)) { // Expired?
            try {
                if (lock.tryLock(DEFAULT_JIGSAW_INIT_TIMEOUTMS / 2, TimeUnit.MILLISECONDS)) {
                    initJigsawImagesBuffer();
                }
            } catch (Exception e) {
                wrapAndThrow(e);
            } finally {
                lock.unlock();
            }
            // Retry get.
            img = imgCache.getMapField(new CacheKey(index, TailoredImage.class));
        }
        notNull(img, "Unable to borrow jigsaw image resource.");

        // UnCompression primary block image.
        return img/* .uncompress() */;
    }

    /**
     * Clear cache.
     */
    public void clearCache() {
        log.info("Cleaning jigsaw images cache ...");
        imgCache.mapRemoveAll();
    }

    /**
     * Initializing JIGSAW image buffer to cache.
     * 
     * @throws Exception
     */
    private synchronized void initJigsawImagesBuffer() throws IOException {
        log.info("Initializing jigsaw images buffer...");

        if (!isBlank(config.getJigsaw().getSourceDir())) {
            File srcDir = new File(config.getJigsaw().getSourceDir());
            state((srcDir.canRead() && srcDir.exists()),
                    format("Failed to initialize jigsaw images, please check the path: %s is correct and has read permission",
                            srcDir.getAbsolutePath()));
            // Read files.
            File[] files = srcDir.listFiles(f -> !startsWith(f.getName(), "."));
            state((files != null && files.length > 0),
                    format("Failed to initialize jigsaw images, path: %s material is empty", srcDir.getAbsolutePath()));
            makeJigsawImageBuffer(files);
        } else { // Fallback use default images material.
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(DEFAULT_JIGSAW_SOURCE_CLASSPATH);
            makeJigsawImageBuffer(resources);
        }
    }

    /**
     * Make and storage buffer images put to cache.
     * 
     * @param sources
     * @throws IOException
     */
    private void makeJigsawImageBuffer(Object[] sources) throws IOException {
        // Statistic use material.
        Set<Integer> indexs = new HashSet<>();

        // Initialize JIGSAW images.
        ImageTailor tailor = new ImageTailor();
        for (int i = 0; i < config.getJigsaw().getPoolImgSize(); i++) {
            int index = i;
            if (index >= sources.length) { // Inadequate material, random reuse.
                index = current().nextInt(sources.length);
            }
            indexs.add(index); // For statistic

            // Generate image.
            Object source = sources[index];
            log.debug("Generate jigsaw image from material: {}", source);

            if (source instanceof File) {
                String path = ((File) sources[index]).getAbsolutePath();
                doPutImage(tailor.getImageFile(path), i);
            } else if (source instanceof Resource) {
                Resource resource = (Resource) source;
                doPutImage(tailor.getImageInputStream(resource.getInputStream()), i);
            } else {
                throw new IllegalStateException(format("Unsupported jigsaw image source: %s", source));
            }
        }

        log.info("Initialized jigsaw images buffer total: {}, expend material: {}", config.getJigsaw().getPoolImgSize(),
                indexs.size());
    }

    /**
     * Put image to cache.
     * 
     * @param code
     * @param index
     */
    private void doPutImage(TailoredImage code, int index) {
        // Compression primary block image.
        // byte[] compressData = code.compress();

        // Storage to cache.
        imgCache.mapPut(new CacheKey(toBytes(valueOf(index)), config.getJigsaw().getPoolImgExpireSec()), code);
        log.debug("Puts jigsaw image, index {}, jigsawImage(x:{}, y:{})", index, code.getX(), code.getY());
    }

    /**
     * Default JIGSAW source image path.
     */
    final public static String DEFAULT_JIGSAW_SOURCE_CLASSPATH = "classpath:static/jigsaw/*.*";

    /**
     * Default JIGSAW initialize image timeoutMs
     */
    final public static long DEFAULT_JIGSAW_INIT_TIMEOUTMS = 60_000L;

}