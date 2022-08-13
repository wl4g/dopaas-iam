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
import com.wl4g.infra.core.web.BaseController;
import com.wl4g.infra.core.page.PageHolder;
import com.wl4g.iam.common.bean.ContactGroup;
import com.wl4g.iam.service.ContactGroupService;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author vjay
 * @date 2019-08-05 11:44:00
 */
@RestController
@RequestMapping("/contactGroup")
public class ContactGroupController extends BaseController {

    // @com.alibaba.dubbo.config.annotation.Reference
    @Autowired
    private ContactGroupService contactGroupService;

    @RequestMapping(value = "/list")
    @RequiresPermissions(value = { "iam:contact" })
    public RespBase<?> list(String name, PageHolder<ContactGroup> pm) {
        log.info("into ContactGroupController.list prarms::" + "name = {} , pm = {} ", name, pm);
        RespBase<Object> resp = RespBase.create();
        resp.setData(contactGroupService.list(pm, name));
        return resp;
    }

    @RequestMapping(value = "/save")
    @RequiresPermissions(value = { "iam:contact" })
    public RespBase<?> save(ContactGroup contactGroup) {
        log.info("into ContactGroupController.save prarms::" + "contactGroup = {} ", contactGroup);
        Assert.notNull(contactGroup, "group is null");
        Assert.hasText(contactGroup.getName(), "groupName is null");
        RespBase<Object> resp = RespBase.create();
        contactGroupService.save(contactGroup);
        return resp;
    }

    @RequestMapping(value = "/del")
    @RequiresPermissions(value = { "iam:contact" })
    public RespBase<?> del(Long id) {
        log.info("into ContactController.del prarms::" + "id = {} ", id);
        RespBase<Object> resp = RespBase.create();
        contactGroupService.del(id);
        return resp;
    }

    @RequestMapping(value = "/groupList")
    @RequiresPermissions(value = { "iam:contact" })
    public RespBase<?> groupList() {
        RespBase<Object> resp = RespBase.create();
        List<ContactGroup> contactGroups = contactGroupService.findContactGroups(null);
        resp.setData(contactGroups);
        return resp;
    }

}