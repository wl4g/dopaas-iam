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
package com.wl4g.iam.data;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.wl4g.infra.integration.feign.core.annotation.FeignConsumer;
import com.wl4g.iam.common.bean.NotificationContact;

@FeignConsumer("${provider.serviceId.iam-data:iam-data}")
@RequestMapping("/notificationContact-dao")
public interface NotificationContactDao {

    @RequestMapping(value = "/deleteByPrimaryKey", method = { POST })
    int deleteByPrimaryKey(@RequestParam("id") Long id);

    @RequestMapping(method = POST, value = "/insert")
    int insert(@RequestBody NotificationContact record);

    @RequestMapping(method = POST, value = "/insertSelective")
    int insertSelective(@RequestBody NotificationContact record);

    @RequestMapping(method = GET, value = "/selectByPrimaryKey")
    NotificationContact selectByPrimaryKey(@RequestParam("id") Long id);

    @RequestMapping(method = POST, value = "/updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(@RequestBody NotificationContact record);

    @RequestMapping(method = POST, value = "/updateByPrimaryKey")
    int updateByPrimaryKey(@RequestBody NotificationContact record);

    @RequestMapping(method = GET, value = "/getByRecordId")
    List<NotificationContact> getByRecordId(@RequestParam("id") Long id);

}