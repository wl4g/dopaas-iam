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
package com.wl4g.iam.service.impl;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.lang.TypeConverts.parseLongOrNull;
import static com.wl4g.infra.core.bean.BaseBean.DEFAULT_SUPER_USER;
import static java.util.Objects.isNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.wl4g.infra.common.id.SnowflakeIdGenerator;
import com.wl4g.infra.core.bean.BaseBean;
import com.wl4g.infra.core.page.PageHolder;
import com.wl4g.iam.common.bean.Menu;
import com.wl4g.iam.common.bean.Organization;
import com.wl4g.iam.common.bean.Role;
import com.wl4g.iam.common.bean.RoleUser;
import com.wl4g.iam.common.bean.User;
import com.wl4g.iam.common.utils.RpcContextIamSecurityUtils;
import com.wl4g.iam.data.MenuDao;
import com.wl4g.iam.data.OrganizationDao;
import com.wl4g.iam.data.RoleDao;
import com.wl4g.iam.data.RoleUserDao;
import com.wl4g.iam.data.UserDao;
import com.wl4g.iam.service.UserService;

/**
 * User service implements.
 * 
 * @author James Wong<jamewong1376@gmail.com>
 * @author vjay
 * @date 2019-10-28 16:38:00
 */
@org.springframework.stereotype.Service
// @com.alibaba.dubbo.config.annotation.Service(group = "areaService")
// @org.springframework.web.bind.annotation.RestController
public class UserServiceImpl implements UserService {

    private @Autowired UserDao userDao;
    private @Autowired MenuDao menuDao;
    private @Autowired RoleDao roleDao;
    private @Autowired OrganizationDao groupDao;
    private @Autowired RoleUserDao roleUserDao;

    @Override
    public PageHolder<User> list(PageHolder<User> pm, String userName, String displayName, Long roleId) {
        pm.useCount().bind();

        // Current login principal.
        String principalId = RpcContextIamSecurityUtils.currentIamPrincipalId();
        String principalName = RpcContextIamSecurityUtils.currentIamPrincipalName();

        List<User> list = null;
        if (DEFAULT_SUPER_USER.equals(principalName)) {
            list = userDao.list(null, userName, displayName, displayName, roleId);
        } else {
            list = userDao.list(parseLongOrNull(principalId), userName, displayName, displayName, roleId);
        }
        for (User user : list) {
            List<Organization> groups = groupDao.selectByUserId(user.getId());
            user.setGroupNameStrs(groups2Str(groups));
            List<Role> roles = roleDao.selectByUserId(user.getId());
            user.setRoleStrs(roles2Str(roles));
        }
        pm.setRecords(list);
        return pm;
    }

    @Override
    public void save(User user) {
        if (!isNull(user.getId())) {
            update(user);
        } else {
            insert(user);
        }
    }

    private void insert(User user) {
        User user1 = userDao.selectBySubject(user.getSubject());
        Assert.isTrue(user1 == null, user.getSubject() + " is exist");
        user.preInsert();
        user.setId(SnowflakeIdGenerator.getDefault().nextId());
        userDao.insertSelective(user);
        List<Long> roleIds = user.getRoleIds();
        for (Long roleId : roleIds) {
            RoleUser roleUser = new RoleUser();
            roleUser.preInsert();
            roleUser.setUserId(user.getId());
            roleUser.setRoleId(roleId);
            roleUserDao.insertSelective(roleUser);
        }
    }

    private void update(User user) {
        user.preUpdate();
        userDao.updateByPrimaryKeySelective(user);
        roleUserDao.deleteByUserId(user.getId());
        for (Long roleId : safeList(user.getRoleIds())) {
            RoleUser roleUser = new RoleUser();
            roleUser.preInsert();
            roleUser.setUserId(user.getId());
            roleUser.setRoleId(roleId);
            roleUserDao.insertSelective(roleUser);
        }
    }

    @Override
    public void del(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setDelFlag(BaseBean.DEL_FLAG_DELETE);
        userDao.updateByPrimaryKeySelective(user);
    }

    @Override
    public User detail(Long userId) {
        User user = userDao.selectByPrimaryKey(userId);
        if (user == null) {
            return null;
        }
        List<Long> roleIds = roleUserDao.selectRoleIdByUserId(userId);
        user.setRoleIds(roleIds);
        return user;
    }

    @Override
    public User findSimpleUser(Long id) {
        return userDao.selectByPrimaryKey(id);
    }

    @Override
    public Set<Menu> getMenusByUser(Long userId) {
        List<Menu> menus = menuDao.selectByUserId(userId);
        Set<Menu> set = new HashSet<>(menus);
        for (Menu menu : menus) {
            getMenusByParentId(menu.getId(), set);
        }
        return set;
    }

    @Override
    public User findBySubject(String userName) {
        return userDao.selectBySubject(userName);
    }

    @Override
    public User findBySelective(User user) {
        return userDao.selectBySelective(user);
    }

    private void getMenusByParentId(Long parentId, Set<Menu> menuSet) {
        // TODO use cache
        List<Menu> menus = menuDao.selectByParentId(parentId);
        if (!CollectionUtils.isEmpty(menus)) {
            if (menuSet != null) {
                menuSet.addAll(menus);
            }
            for (Menu menu : menus) {
                getMenusByParentId(menu.getId(), menuSet);
            }
        }
    }

    private String roles2Str(List<Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < roles.size(); i++) {
            String displayName = roles.get(i).getNameZh();
            if (i == roles.size() - 1) {
                stringBuilder.append(displayName);
            } else {
                stringBuilder.append(displayName).append(",");
            }
        }
        return stringBuilder.toString();
    }

    private String groups2Str(List<Organization> groups) {
        if (CollectionUtils.isEmpty(groups)) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < groups.size(); i++) {
            String displayName = groups.get(i).getNameZh();
            if (i == groups.size() - 1) {
                stringBuilder.append(displayName);
            } else {
                stringBuilder.append(displayName).append(",");
            }
        }
        return stringBuilder.toString();
    }

}