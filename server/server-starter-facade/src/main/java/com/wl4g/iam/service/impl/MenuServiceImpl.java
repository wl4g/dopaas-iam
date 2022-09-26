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

import static com.wl4g.infra.common.lang.Assert2.isTrue;
import static com.wl4g.infra.common.lang.TypeConverts.parseLongOrNull;
import static com.wl4g.infra.common.serialize.JacksonUtils.deepClone;
import static com.wl4g.infra.common.bean.BaseBean.DEFAULT_SUPER_USER;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.wl4g.infra.common.bean.BaseBean;
import com.wl4g.iam.common.bean.Menu;
import com.wl4g.iam.common.utils.RpcContextIamSecurityUtils;
import com.wl4g.iam.data.MenuDao;
import com.wl4g.iam.service.MenuService;
import com.wl4g.iam.service.OrganizationService;

/**
 * Menu service implements.
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @author vjay
 * @date 2019-10-30 15:48:00
 */
@org.springframework.stereotype.Service
// @com.alibaba.dubbo.config.annotation.Service(group = "menuService")
// @org.springframework.web.bind.annotation.RestController
public class MenuServiceImpl implements MenuService {

    @Autowired
    protected MenuDao menuDao;
    @Autowired
    protected OrganizationService groupService;

    @Override
    public Map<String, Object> findMenuTree() {
        Map<String, Object> result = new HashMap<>();
        Set<Menu> menuSet = getUserMenuSet();

        List<Menu> menuTree = new ArrayList<>(deepClone(menuSet));
        resolveMenuRoutePath(menuTree);
        menuTree = transformMenuTree(menuTree);

        result.put("data", menuTree);
        result.put("data2", menuSet);
        return result;
    }

    @Override
    public List<Menu> findMenuList() {
        String principalId = RpcContextIamSecurityUtils.currentIamPrincipalId();
        String principal = RpcContextIamSecurityUtils.currentIamPrincipalName();
        List<Menu> result;
        if (DEFAULT_SUPER_USER.equals(principal)) {
            result = menuDao.selectWithRoot();
        } else {
            result = menuDao.selectByUserId(parseLongOrNull(principalId));
        }
        // deal with route path
        resolveMenuRoutePath(result);
        return result;
    }

    @Override
    public void save(Menu menu) {
        checkRepeat(menu);
        checkMenuProperties(menu);
        if (menu.getId() != null) {
            update(menu);
        } else {
            insert(menu);
        }
    }

    @Override
    public void del(Long id) {
        Assert.notNull(id, "id is null");
        Menu menu = new Menu();
        menu.setId(id);
        menu.setDelFlag(BaseBean.DEL_FLAG_DELETED);
        menuDao.updateByPrimaryKeySelective(menu);
    }

    @Override
    public Menu detail(Long id) {
        return menuDao.selectByPrimaryKey(id);
    }

    @Override
    public List<Menu> findRoot() {
        return menuDao.selectWithRoot();
    }

    @Override
    public List<Menu> findByUserId(Long userId) {
        return menuDao.selectByUserId(userId);
    }

    private void insert(Menu menu) {
        menu.preInsert();
        Long parentId = menu.getParentId();
        // if menu type is button
        if (nonNull(menu.getType()) && menu.getType().intValue() == 3) {
            menu.setLevel(0);
        } else {
            // if has parent menu , set level = parent's level + 1
            if (nonNull(parentId) && 0 != parentId) {
                Menu parentMenu = menuDao.selectByPrimaryKey(parentId);
                Assert.notNull(parentMenu, "parentMenu is null");
                Assert.notNull(parentMenu.getLevel(), "parentMenu's level is null");
                menu.setLevel(parentMenu.getLevel() + 1);
            } else {// if is parent menu , set level = 1
                menu.setLevel(1);
            }
        }
        menuDao.insertSelective(menu);
    }

    private void update(Menu menu) {
        menu.preUpdate();
        menuDao.updateByPrimaryKeySelective(menu);
    }

    private void resolveMenuRoutePath(List<Menu> list) {
        for (Menu menu : list) {
            menu.setRoutePath(menu.getRouteNamespace());
            if (menu.getParentId() != null && menu.getParentId() > 0 && StringUtils.isNotBlank(menu.getRouteNamespace())) {
                updateMenuRoutePath(list, menu, menu.getParentId());
            }
        }
    }

    private void updateMenuRoutePath(List<Menu> list, Menu menu, long parentId) {
        if (parentId != 0) {
            for (Menu m : list) {
                if (m.getId().equals(parentId)) {
                    menu.setRoutePath(fixRouteNamespace(m.getRouteNamespace()) + fixRouteNamespace(menu.getRoutePath()));
                    if (m.getParentId() != null && m.getParentId() > 0) {
                        updateMenuRoutePath(list, menu, m.getParentId());
                    }
                    break;
                }
            }
        }
    }

    private Menu getParent(List<Menu> menus, Long parentId) {
        for (Menu menu : menus) {
            if (parentId != null && menu.getId() != null && menu.getId().longValue() == parentId.longValue()) {
                return menu;
            }
        }
        return null;
    }

    private String fixRouteNamespace(String routeNamespace) {
        if (StringUtils.equals("/", routeNamespace)) {
            return "";
        }
        if (routeNamespace != null && routeNamespace.length() > 1) {
            if (!routeNamespace.startsWith("/")) {
                routeNamespace = "/" + routeNamespace;
            }
            while (routeNamespace.endsWith("/")) {
                routeNamespace = routeNamespace.substring(0, routeNamespace.length() - 1);
            }
            routeNamespace = routeNamespace.trim();
            while (routeNamespace.contains("//")) {
                routeNamespace = routeNamespace.replaceAll("//", "/");
            }
        }
        return routeNamespace;
    }

    /**
     * <pre>
     * 父/子 动态      静态     按钮
     * 动态  1         1       0
     * 静态  1         1       1(无需设菜单文件夹的pagelocation和route_namespace为空)
     * 按钮  0         0       0
     * </pre>
     *
     * @param menu
     */
    private void checkMenuProperties(Menu menu) {
        String routeNamespace = menu.getRouteNamespace();
        final boolean isLegal = routeNamespace.startsWith("/") && routeNamespace.indexOf("/") == routeNamespace.lastIndexOf("/")
                && routeNamespace.length() > 1;
        isTrue(isLegal, "illegal routeNamespace: '%s', e.g: '/edit'", routeNamespace);

        if (menu.getParentId() == null) {
            return;
        }
        Menu parentMenu = menuDao.selectByPrimaryKey(menu.getParentId());
        if (null != parentMenu) {
            if (parentMenu.getType() == 1) {
                return;
            }
            if (parentMenu.getType() == 2 && menu.getType() == 3) {
                throw new UnsupportedOperationException("can not save because parentType=2(dynamic) and menuType=3(button)");
            }
            if (parentMenu.getType() == 3) {
                throw new UnsupportedOperationException("can not save because parentType=3(button)");
            }
        }
    }

    private void checkRepeat(Menu menu) {
        List<Menu> menus = menuDao.selectByParentId(menu.getParentId());
        for (Menu m : menus) {
            if (!m.getId().equals(menu.getId())) {
                isTrue(!equalsIgnoreCase(m.getRouteNamespace(), menu.getRouteNamespace()), "menu's route path is repeat");
                isTrue(!menu.getSort().equals(m.getSort()), "menu's sort repeat");
            }
        }
    }

    private Set<Menu> getUserMenuSet() {
        String principalId = RpcContextIamSecurityUtils.currentIamPrincipalId();
        String principal = RpcContextIamSecurityUtils.currentIamPrincipalName();

        List<Menu> menus = null;
        if (DEFAULT_SUPER_USER.equals(principal)) {
            menus = menuDao.selectWithRoot();
        } else {
            Long userId = null;
            if (isNotBlank(principalId)) {
                userId = Long.parseLong(principalId);
            }
            menus = menuDao.selectByUserId(userId);
        }
        Set<Menu> set = new LinkedHashSet<>();
        set.addAll(menus);
        // Clean unused fields
        for (Menu menu : set) {
            menu.setRoutePath(null);
            menu.setLevel(null);
            menu.setClassify(null);
            // menu.setPageLocation(null);
            // menu.setRenderTarget(null);
            // menu.setRouteNamespace(null);
            menu.setStatus(null);
            // menu.setType(null);
            menu.setDelFlag(null);
            menu.setUpdateBy(null);
            menu.setUpdateDate(null);
            menu.setCreateDate(null);
            menu.setCreateBy(null);
            menu.setOrgCode(null);
        }
        return set;
    }

    private List<Menu> transformMenuTree(List<Menu> menus) {
        List<Menu> top = new ArrayList<>();
        for (Menu menu : menus) {
            Menu parent = getParent(menus, menu.getParentId());
            if (parent == null) {
                top.add(menu);
            }
        }
        for (Menu menu : top) {
            List<Menu> children = getChildren(menus, null, menu);
            if (!CollectionUtils.isEmpty(children)) {
                menu.setChildren(children);
            }
        }
        return top;
    }

    private List<Menu> getChildren(List<Menu> menus, List<Menu> children, Menu parent) {
        if (children == null) {
            children = new ArrayList<>();
        }
        for (Menu menu : menus) {
            if (menu.getParentId() != null && parent.getId() != null
                    && menu.getParentId().longValue() == parent.getId().longValue()) {
                menu.setParentRoutePath(parent.getRoutePath());
                children.add(menu);
            }
        }
        for (Menu menu : children) {
            List<Menu> children1 = getChildren(menus, null, menu);
            if (!CollectionUtils.isEmpty(children1)) {
                menu.setChildren(children1);
            }
        }
        return children;
    }

}