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

import static com.wl4g.iam.common.constant.FastCasIAMConstants.BEAN_SESSION_RESOURCE_MSG_BUNDLER;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.KEY_SECRET_INFO;
import static com.wl4g.iam.core.utils.IamSecurityHolder.bind;
import static com.wl4g.iam.core.utils.IamSecurityHolder.checkSession;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getBindValue;
import static com.wl4g.iam.core.utils.IamSecurityHolder.getSessionId;
import static com.wl4g.infra.common.codec.CheckSums.crc32;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static java.security.MessageDigest.isEqual;
import static java.util.Objects.isNull;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.apache.commons.lang3.StringUtils.isBlank;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.codec.CodecSupport;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.factory.annotation.Autowired;

import com.wl4g.iam.common.i18n.SessionResourceMessageBundler;
import com.wl4g.iam.configure.SecureConfig;
import com.wl4g.iam.core.authc.IamAuthenticationInfo;
import com.wl4g.iam.core.cache.IamCacheManager;
import com.wl4g.iam.core.session.IamSession.RelationAttrKey;
import com.wl4g.iam.crypto.SecureCryptService;
import com.wl4g.iam.crypto.SecureCryptService.CryptKind;
import com.wl4g.infra.common.codec.CodecSource;
import com.wl4g.infra.common.crypto.asymmetric.spec.KeyPairSpec;
import com.wl4g.infra.common.framework.operator.GenericOperatorAdapter;
import com.wl4g.infra.common.log.SmartLogger;

/**
 * Abstract credentials securer adapter
 *
 * @author wangl.sir
 * @version v1.0 2019年3月11日
 * @see {@link org.apache.shiro.crypto.hash.DefaultHashService}
 * @since
 */
abstract class AbstractCredentialsSecurerSupport extends CodecSupport implements IamCredentialsSecurer {
    protected final SmartLogger log = getLogger(getClass());

    /**
     * Secure configuration.
     */
    protected final SecureConfig config;

    /**
     * Credential cache manager.
     */
    protected final IamCacheManager cacheManager;

    /**
     * The 'private' part of the hash salt.
     */
    protected final CodecSource privateSalt;

    /**
     * Secure asymmetric cryptic service.
     */
    protected @Autowired GenericOperatorAdapter<CryptKind, SecureCryptService> cryptAdapter;

    /**
     * I18n message source.
     */
    @Resource(name = BEAN_SESSION_RESOURCE_MSG_BUNDLER)
    protected SessionResourceMessageBundler bundle;

    /**
     * Iam delegate credentials securer. (Extension: optional)
     */
    protected @Nullable @Autowired(required = false) CredentialsSecurerAdapter delegate;

    protected AbstractCredentialsSecurerSupport(SecureConfig config, IamCacheManager cacheManager) {
        this.config = notNullOf(config, "secureConfig");
        notNullOf(config.getPrivateSalt(), "privateSalt");
        notNullOf(config.getPreCryptPoolSize(), "cryptSize");
        notNullOf(config.getCryptosExpireMs() > 0, "cryptExpireMs");
        notNullOf(config.getApplyPubkeyExpireMs() > 0, "applyPubKeyExpireMs");
        notNullOf(cacheManager, "cacheManager");
        this.privateSalt = new CodecSource(config.getPrivateSalt());
        this.cacheManager = cacheManager;
    }

    @Override
    public String signature(@NotNull CredentialsToken token, @NotNull CodecSource publicSalt) {
        notNullOf(token, "credentialsToken");

        // Delegate signature
        if (!isNull(delegate) && !token.isSolved()) {
            // Resolving request credentials token.
            return delegate.signature(resolveToken(token), publicSalt);
        }

        // When the delegate is null, it is unresolved.
        if (!token.isSolved()) {
            token = resolveToken(token); // It is necessary to resolving
        }

        // Hashing signature
        return doCredentialsHash(token, publicSalt,
                (algorithm, source, salt, hashIters) -> new SimpleHash(algorithm, source, salt, hashIters));
    }

    @Override
    public boolean validate(@NotNull CredentialsToken token, @NotNull IamAuthenticationInfo info)
            throws CredentialsException, RuntimeException {
        notNullOf(token, "credentialsToken");
        /*
         * Password is a string that may be set to empty.
         * See:xx.realm.GeneralAuthorizingRealm#doIamAuthenticationInfo
         */
        notNullOf(info, "storedAuthzInfo");
        notNullOf(info.getCredentials(), "storedCredentials");

        // Delegate validate.
        if (!isNull(delegate) && !token.isSolved()) {
            return delegate.validate(resolveToken(token), info);
        }

        // Assertion compare request credentials & storage credentials.
        return isEqual(toBytes(signature(token, info.getPublicSalt())), toBytes(info.getCredentials()));
    }

    /**
     * {@link super#applySecret(String)}
     * 
     * @param principal
     * @return
     * @see {@link com.wl4g.iam.web.login.LoginAuthenticationController#handhake()}
     */
    @Override
    public String applySecret(@NotNull CryptKind kind, @NotNull String principal) {
        // Check required sessionKey.
        checkSession();

        // Gets secretKey(publicKey) index.
        Integer index = getBindValue(KEY_SECRET_INFO);
        if (isNull(index)) {
            index = current().nextInt(0, config.getPreCryptPoolSize());
        }
        log.debug("Applied secretKey index: {}", index);

        // Gets applySecret keyPair index.
        KeyPairSpec keyPair = cryptAdapter.forOperator(kind).borrowKeyPair(index);
        log.debug("Generate asymmetric algorithms. index: {}, keyPairSpec: {}", index, keyPair);

        // Storage applied secureKey index.
        bind(new RelationAttrKey(KEY_SECRET_INFO, config.getApplyPubkeyExpireMs()), index);

        log.info("Applied secretKey of sessionId: {}, index: {}, pubKeyHexString: {}, privKeyHexString: {}", getSessionId(),
                index, keyPair.getPubHexString(), keyPair.getHexString());
        return keyPair.getPubHexString();
    }

    /**
     * Execute hashing
     *
     * @param token
     *            Resolved parameter token
     * @param info
     * @param hasher
     * @return
     */
    protected String doCredentialsHash(@NotNull CredentialsToken token, @NotNull CodecSource publicSalt, @NotNull Hasher hasher) {
        // Merge salt
        CodecSource salt = merge(privateSalt, determinePublicSalt(token, publicSalt));
        log.debug("Merge salt of principal: {}, salt: {}", token.getPrincipal(), salt);

        // Determine which hashing algorithm to use
        final String[] hashAlgorithms = config.getHashAlgorithms();
        final int size = hashAlgorithms.length;
        final long index = crc32(salt.getBytes()) % size & (size - 1);
        final String algorithm = hashAlgorithms[(int) index];
        final int hashIters = (int) (Integer.MAX_VALUE % (index + 1)) + 1;

        // Hashing signature
        return hasher.hashing(algorithm, token.getCredentials(), salt.getBytes(), hashIters).toHex();
    }

    /**
     * Combines the specified 'private' salt bytes with the specified additional
     * extra bytes to use as the total salt during hash computation.
     * {@code privateSaltBytes} will be {@code null} }if no private salt has
     * been configured.
     *
     * @param privateSalt
     *            the (possibly {@code null}) 'private' salt to combine with the
     *            specified extra bytes
     * @param publicSalt
     *            the extra bytes to use in addition to the given private salt.
     * @return a combination of the specified private salt bytes and extra bytes
     *         that will be used as the total salt during hash computation.
     * @see {@link org.apache.shiro.crypto.hash.DefaultHashService#combine()}
     */
    protected abstract CodecSource merge(CodecSource privateSalt, CodecSource publicSalt);

    /**
     * Determing and gets principal public salt.
     *
     * @param token
     *            Request principal and credentials token information.
     * @param publicSalt
     *            Database stored credentials information.
     * @return
     */
    protected abstract CodecSource determinePublicSalt(@NotNull CredentialsToken token, @NotNull CodecSource publicSalt);

    /**
     * Resolving credentials token. (corresponding to the frontend,
     * RSA1/RSA2/DSA/ECC/... encryption is used by default)
     *
     * @param cryptKind
     * @param token
     * @return
     */
    protected CredentialsToken resolveToken(@NotNull CredentialsToken token) {
        // Determine keyPairSpec
        KeyPairSpec keyPairSpec = determineSecretKeySpecPair(token.getKind(), token.getPrincipal());

        if (log.isDebugEnabled()) {
            String publicBase64String = keyPairSpec.getPubHexString();
            String pattern = "Determined keypair is principal: {}, publicKey: {}, privateKey: {}";
            String privateBase64String = "******";
            // privateBase64String = keyPairSpec.getBase64String();
            log.debug(pattern, token.getPrincipal(), publicBase64String, privateBase64String);
        }

        // Mysterious decryption them.
        final String plainCredentials = cryptAdapter.forOperator(token.getKind())
                .decrypt(keyPairSpec.getKeySpec(), token.getCredentials());
        if (isBlank(plainCredentials)) {
            throw new AuthenticationException("Invalid credentials");
        }
        return new CredentialsToken(token.getPrincipal(), plainCredentials, token.getKind(), true);
    }

    /**
     * Determine asymmetric algorithms keyPair
     *
     * @param cryptKind
     * @param principal
     * @return
     */
    private KeyPairSpec determineSecretKeySpecPair(@NotNull CryptKind kind, @NotBlank String principal) {
        // Gets the best one from the candidate keyPair.
        Integer index = getBindValue(new RelationAttrKey(KEY_SECRET_INFO, Integer.class), true);
        if (!isNull(index)) {
            KeyPairSpec keyPair = cryptAdapter.forOperator(kind).borrowKeyPair(index);
            log.debug("Determine asymmetric algorithms. index: {}, keyPairSpec: {}", index, keyPair);
            return keyPair;
        }
        log.warn("Failed to decrypt, secretKey expired of seesionId: {}, principal: {}", getSessionId(), principal);
        throw new IllegalStateException(bundle.getMessage("AbstractCredentialsSecurerSupport.secretKey.expired"));
    }

    /**
     * Hasher function.
     *
     * @author wangl.sir
     * @version v1.0 2019年1月21日
     * @since
     */
    private interface Hasher {
        Hash hashing(String algorithm, String source, byte[] salt, int hashIterations);
    }

}