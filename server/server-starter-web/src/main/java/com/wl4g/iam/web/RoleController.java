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
package com.wl4g.iam.web;

import com.wl4g.infra.common.web.rest.RespBase;
import com.wl4g.infra.common.bean.page.PageHolder;
import com.wl4g.iam.common.bean.Role;
import com.wl4g.iam.service.RoleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author vjay
 * @date 2019-10-29 16:07:00
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    // @com.alibaba.dubbo.config.annotation.Reference
    @Autowired
    private RoleService roleService;

    @RequestMapping(value = "/getRolesByUserGroups")
    @RequiresPermissions(value = { "iam:role" })
    public RespBase<?> getRolesByUserGroups() {
        RespBase<Object> resp = RespBase.create();

        List<Role> roles = roleService.getLoginRoles();

        resp.forMap().put("data", roles);
        return resp;
    }

    @RequestMapping(value = "/list")
    @RequiresPermissions(value = { "iam:role" })
    public RespBase<?> list(PageHolder<Role> pm, String organizationId, String roleCode, String displayName) {
        RespBase<Object> resp = RespBase.create();

        resp.setData(roleService.list(pm, organizationId, roleCode, displayName));

        return resp;
    }

    @RequestMapping(value = "/save")
    @RequiresPermissions(value = { "iam:role" })
    public RespBase<?> save(@RequestBody Role role) {
        RespBase<Object> resp = RespBase.create();
        roleService.save(role);
        return resp;
    }

    @RequestMapping(value = "/del")
    @RequiresPermissions(value = { "iam:role" })
    public RespBase<?> del(Long id) {
        RespBase<Object> resp = RespBase.create();
        roleService.del(id);
        return resp;
    }

    @RequestMapping(value = "/detail")
    @RequiresPermissions(value = { "iam:role" })
    public RespBase<?> detail(Long id) {
        RespBase<Object> resp = RespBase.create();
        Role role = roleService.detail(id);
        resp.forMap().put("data", role);
        return resp;
    }

}