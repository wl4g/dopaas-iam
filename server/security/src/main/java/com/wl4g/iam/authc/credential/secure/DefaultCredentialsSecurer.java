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
package com.wl4g.iam.authc.credential.secure;

import static com.wl4g.infra.common.codec.CheckSums.crc32;
import static com.wl4g.infra.common.lang.Assert2.*;

import javax.validation.constraints.NotNull;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource.Util;

import com.wl4g.infra.common.codec.CodecSource;
import com.wl4g.iam.configure.SecureConfig;
import com.wl4g.iam.core.cache.IamCacheManager;

/**
 * Default credentials securer
 *
 * @author wangl.sir
 * @version v1.0 2019年1月16日
 * @since
 */
public class DefaultCredentialsSecurer extends AbstractCredentialsSecurerSupport {

    public DefaultCredentialsSecurer(SecureConfig config, IamCacheManager cacheManager) {
        super(config, cacheManager);
    }

    @Override
    protected CodecSource merge(CodecSource privateSalt, CodecSource publicSalt) {
        return new CodecSource(crossCombined(privateSalt.getBytes(), publicSalt.getBytes()));
    }

    @Override
    protected CodecSource determinePublicSalt(@NotNull CredentialsToken token, @NotNull CodecSource publicSalt) {
        // return new CodecSource(token.getPrincipal()); // for simple
        // from DB
        return notNull(publicSalt,
                "Salt is required for current credentials verification, Please check whether the salt field of the stored account credentials info is complete");
    }

    /**
     * Cross combined of bytes
     *
     * @param privateSalt
     * @param publicSalt
     * @return
     */
    private static byte[] crossCombined(byte[] privateSalt, byte[] publicSalt) {
        notNull(privateSalt, "'privateSalt' must not be null");
        notNull(publicSalt, "'publicSalt' must not be null");
        int privateSaltLength = privateSalt != null ? privateSalt.length : 0;
        int publicSaltLength = publicSalt != null ? publicSalt.length : 0;

        int length = privateSaltLength + publicSaltLength;
        if (length <= 0) {
            return null;
        }

        byte[] combined = new byte[length];
        int i = 0;
        for (int j = 0, k = 0; j < privateSaltLength || k < publicSaltLength; j++, k++) {
            if (j < privateSaltLength) {
                combined[i++] = privateSalt[j];
            }
            if (k < publicSaltLength) {
                combined[i++] = publicSalt[k];
            }
        }
        return combined;
    }

    public static void main(String[] args) {
        // CodecSource privateSalt = new CodecSource("IAM");
        // CodecSource privateSalt = new CodecSource("iam-webdev");
        CodecSource privateSalt = new CodecSource("iam-webfat");
        // CodecSource privateSalt = new CodecSource("iam-webuat");
        // CodecSource privateSalt = new CodecSource("iam-webpro");
        // CodecSource privateSalt = new CodecSource("safecloud");

        CodecSource publicSalt = CodecSource.fromHex("a3e0b320c73020aa81ebf87bd8611bf1");
        CodecSource salt = new CodecSource(crossCombined(privateSalt.getBytes(), publicSalt.getBytes()));

        String[] hashAlgorithms = new String[] { "MD5", "SHA-256", "SHA-384", "SHA-512" };
        int size = hashAlgorithms.length;
        long index = crc32(salt.getBytes()) % size & (size - 1);
        String algorithm = hashAlgorithms[(int) index];
        int hashIterations = (int) (Integer.MAX_VALUE % (index + 1)) + 1;

        System.out.println(">>>>>>>>>>");
        System.out.print(new SimpleHash(algorithm, Util.bytes("123456"), salt.getBytes(), hashIterations).toHex());
        System.out.print("\n<<<<<<<<<<");
    }

}