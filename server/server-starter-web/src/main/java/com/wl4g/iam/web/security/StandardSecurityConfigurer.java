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
package com.wl4g.iam.web.security;

import static com.wl4g.infra.common.collection.CollectionUtils2.isEmptyArray;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeSet;
import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.bean.BaseBean.DEFAULT_SUPER_USER;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wl4g.iam.common.bean.FastCasClientInfo;
import com.wl4g.iam.common.bean.Menu;
import com.wl4g.iam.common.bean.OidcClient;
import com.wl4g.iam.common.bean.Organization;
import com.wl4g.iam.common.bean.RealmBean;
import com.wl4g.iam.common.bean.Role;
import com.wl4g.iam.common.bean.SocialConnectInfo;
import com.wl4g.iam.common.bean.User;
import com.wl4g.iam.common.subject.IamPrincipal;
import com.wl4g.iam.common.subject.IamPrincipal.OrganizationInfo;
import com.wl4g.iam.common.subject.IamPrincipal.Parameter;
import com.wl4g.iam.common.subject.IamPrincipal.PrincipalOrganization;
import com.wl4g.iam.common.subject.IamPrincipal.SimpleParameter;
import com.wl4g.iam.common.subject.IamPrincipal.SnsParameter;
import com.wl4g.iam.common.subject.SimpleIamPrincipal;
import com.wl4g.iam.configure.ServerSecurityConfigurer;
import com.wl4g.iam.core.exception.OidcException;
import com.wl4g.iam.service.FastCasClientService;
import com.wl4g.iam.service.MenuService;
import com.wl4g.iam.service.OidcClientService;
import com.wl4g.iam.service.OidcMapperService;
import com.wl4g.iam.service.OrganizationService;
import com.wl4g.iam.service.RealmService;
import com.wl4g.iam.service.RoleService;
import com.wl4g.iam.service.UserService;

/**
 * Standard IAM Security context handler
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0
 * @date 2018年12月29日
 * @since
 */
@Service
public class StandardSecurityConfigurer implements ServerSecurityConfigurer {

    /**
     * To solve the problem that protobuff cannot be serialization, refer see:
     * {@link StandardSecurityConfigurer#getIamUserDetail()}
     */
    private @Autowired transient UserService userService;
    private @Autowired transient RoleService roleService;
    private @Autowired transient MenuService menuService;
    private @Autowired transient OrganizationService organService;
    private @Autowired transient FastCasClientService fastCasClientService;
    private @Autowired transient RealmService realmService;
    private @Autowired transient OidcClientService oidcClientService;
    private @Autowired transient OidcMapperService oidcMapperService;
    private @Autowired transient IamHelper iamHelper;

    @Override
    public String decorateAuthenticateSuccessUrl(
            String successUrl,
            AuthenticationToken token,
            Subject subject,
            ServletRequest request,
            ServletResponse response) {
        return successUrl;
    }

    @Override
    public String decorateAuthenticateFailureUrl(
            String loginUrl,
            AuthenticationToken token,
            Throwable ae,
            ServletRequest request,
            ServletResponse response) {
        return loginUrl;
    }

    @Override
    public FastCasClientInfo getFastCasClientInfo(String appName) {
        List<FastCasClientInfo> apps = safeList(findFastCasClientInfo(appName));
        return !isEmpty(apps) ? apps.get(0) : null;
    }

    @Override
    public List<FastCasClientInfo> findFastCasClientInfo(String... appNames) {
        List<FastCasClientInfo> appClients = new ArrayList<>();
        if (isEmptyArray(appNames)) {
            return emptyList();
        }

        // is't IAM demo
        if (equalsAny("iam-example", appNames)) {
            FastCasClientInfo app = new FastCasClientInfo("iam-example", "http://localhost:18083");
            app.setIntranetBaseUri("http://localhost:18083/iam-example");
            appClients.add(app);
        } else { // formal environment
            List<FastCasClientInfo> fccs = fastCasClientService.findByAppNames(appNames,
                    iamHelper.getApplicationActiveEnvironmentType(), null);
            for (FastCasClientInfo cc : fccs) {
                FastCasClientInfo app = new FastCasClientInfo();
                app.setAppName(cc.getAppName());
                app.setExtranetBaseUri(cc.getExtranetBaseUri());
                app.setIntranetBaseUri(cc.getIntranetBaseUri());
                app.setViewExtranetBaseUri(cc.getViewExtranetBaseUri());
                app.setRemark(cc.getRemark());
                appClients.add(app);
            }
        }

        return appClients;
    }

    @Override
    public IamPrincipal getIamUserDetail(Parameter parameter) {
        User user = null;

        // By SNS authorizing
        if (parameter instanceof SnsParameter) {
            SnsParameter sns = (SnsParameter) parameter;
            User find = User.builder().wechatOpenId(sns.getOpenId()).wechatUnionId(sns.getUnionId()).build();
            user = userService.findBySelective(find);
        }
        // By general account
        else if (parameter instanceof SimpleParameter) {
            SimpleParameter simpleParameter = (SimpleParameter) parameter;
            user = userService.findBySubject(simpleParameter.getPrincipal());
        }
        if (nonNull(user)) {
            // User organization sets.
            Set<Organization> orgSet = organService.getUserOrganizations(user);
            List<OrganizationInfo> orgs = safeSet(orgSet).stream()
                    .map(o -> new OrganizationInfo().withOrgCode(o.getOrgCode())
                            .withParent(o.getParentCode())
                            .withType(o.getType())
                            .withName(o.getNameEn())
                            .withAreaId(o.getAreaId()))
                    .collect(toList());

            IamPrincipal principal = new SimpleIamPrincipal().withPrincipalId(valueOf(user.getId()))
                    .withPrincipal(user.getSubject())
                    .withStoredCredentials(user.getPassword())
                    .withPublicSalt(user.getPubSalt())
                    .withRoles(findRoles(user.getSubject()))
                    .withPermissions(findPermissions(user.getSubject()))
                    .withOrganization(new PrincipalOrganization(orgs));
            principal.attributes().setUser(user);
            return principal;
        }
        return null;
    }

    @Override
    public boolean isApplicationAccessAuthorized(String principal, String application) {
        return true;
    }

    @Override
    public void bindSocialConnection(SocialConnectInfo social) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SocialConnectInfo> findSocialConnections(String principal, String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unbindSocialConnection(SocialConnectInfo social) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets current authenticate principal role codes.
     *
     * @param subject
     * @return
     */
    private String findRoles(String subject) {
        User user = userService.findBySubject(subject);
        // TODO cache
        List<Role> list;
        if (DEFAULT_SUPER_USER.equals(subject)) {
            list = roleService.findRoot(null, null, null);
        } else {
            list = roleService.findByUserId(user.getId());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Role role = list.get(i);
            if (i == list.size() - 1) {
                sb.append(role.getRoleCode());
            } else {
                sb.append(role.getRoleCode()).append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Gets current authenticate principal permissions.
     *
     * @param subject
     * @return
     */
    private String findPermissions(String subject) {
        User user = userService.findBySubject(subject);
        // TODO cache
        List<Menu> list;
        if (DEFAULT_SUPER_USER.equals(subject)) {
            list = menuService.findRoot();
        } else {
            list = menuService.findByUserId(user.getId());
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Menu menu = list.get(i);
            if (i == list.size() - 1) {
                sb.append(menu.getPermission());
            } else {
                sb.append(menu.getPermission()).append(",");
            }
        }
        return sb.toString();
    }

    @Override
    public RealmBean loadRealm(Long realmId, String realmName) {
        List<RealmBean> realms = realmService.findList(RealmBean.builder().id(realmId).name(realmName).build());
        if (isEmpty(realms)) {
            throw new OidcException(format("No found realm configuration by realmId='%s' or realmName='%s'", realmId, realmName));
        } else if (realms.size() > 1) {
            throw new IllegalStateException(
                    format("Data check error, too many realm configurations was got by realmId='%s' or realmName='%s'", realmId,
                            realmName));
        }
        return realms.get(0);
    }

    @Override
    public OidcClient loadOidcClient(String clientId) {
        hasTextOf(clientId, "clientId");

        List<OidcClient> clients = oidcClientService.findList(
                OidcClient.builder().clientId(clientId).envType(iamHelper.getApplicationActiveEnvironmentType()).build());
        if (isEmpty(clients)) {
            throw new OidcException(format("No found OIDC client configuration by clientId='%s'", clientId));
        } else if (clients.size() > 1) {
            throw new IllegalStateException(
                    format("Data check error, too many OIDC client configurations was got by clientId='%s'", clientId));
        }
        OidcClient client = clients.get(0);
        client.setRealm(loadRealm(client.getRealmId(), null));
        client.setMappers(oidcMapperService.findByClientId(clientId));
        return client;
    }

}