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
import com.wl4g.iam.service.AreaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author vjay
 * @date 2019-10-29 16:19:00
 */
@RestController
@RequestMapping("/area")
public class AreaController {

    // @com.alibaba.dubbo.config.annotation.Reference
    @Autowired
    private AreaService areaService;

    @RequestMapping(value = "/getAreaTree")
    public RespBase<?> getAreaTree() {
        RespBase<Object> resp = RespBase.create();
        resp.setData(areaService.getAreaTree());
        return resp;
    }

}