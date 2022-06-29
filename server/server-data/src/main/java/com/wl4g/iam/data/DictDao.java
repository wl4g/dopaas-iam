/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
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

import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.wl4g.infra.integration.feign.core.annotation.FeignConsumer;
import com.wl4g.iam.common.bean.Dict;

import java.util.List;

/**
 * {@link DictDao}
 * 
 * @author James Wong &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2020-0520
 * @sine v1.0
 * @see
 */
@FeignConsumer("${provider.serviceId.iam-data:iam-data}")
@RequestMapping("/dict-dao")
public interface DictDao {
    @RequestMapping(value = "/deleteByPrimaryKey", method = { POST })
    int deleteByPrimaryKey(@RequestParam("key") String key);

    @RequestMapping(method = POST, value = "/insert")
    int insert(@RequestBody Dict record);

    @RequestMapping(method = POST, value = "/insertSelective")
    int insertSelective(@RequestBody Dict record);

    @RequestMapping(method = GET, value = "/selectByPrimaryKey")
    Dict selectByPrimaryKey(@RequestParam("key") String key);

    @RequestMapping(method = POST, value = "/updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(@RequestBody Dict record);

    @RequestMapping(method = POST, value = "/updateByPrimaryKey")
    int updateByPrimaryKey(@RequestBody Dict record);

    @RequestMapping(method = GET, value = "/selectByType")
    List<Dict> selectByType(@RequestParam("type") String type);

    @RequestMapping(method = GET, value = "/allType")
    List<String> allType();

    @RequestMapping(method = GET, value = "/getByKey")
    Dict getByKey(@RequestParam("key") String key);

    @RequestMapping(method = GET, value = "/list")
    List<Dict> list(
            @RequestParam(value = "key", required = false) @Param("key") String key,
            @RequestParam(value = "label", required = false) @Param("label") String label,
            @RequestParam(value = "type", required = false) @Param("type") String type,
            @Deprecated @RequestParam(value = "description", required = false) @Param("description") String description,
            @RequestParam(value = "orderBySort", required = false) @Param("orderBySort") String orderBySort);

}