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
package com.wl4g.iam.service;

import com.wl4g.infra.integration.feign.core.annotation.FeignConsumer;
import com.wl4g.iam.common.bean.FastCasClientInfo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * {@link FastCasClientService}
 * 
 * @author James Wong &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @author vjay
 * @date 2019-11-14
 * @sine v1.0.0
 * @see
 */
@FeignConsumer("${provider.serviceId.iam-facade:clientConfig-service}")
@RequestMapping("/fastCasClient-service")
public interface FastCasClientService {

    @RequestMapping(value = "/loadInit", method = GET)
    Map<String, Object> loadInit(@RequestParam(value = "envType", required = false) String envType);

    @RequestMapping(value = "/getFastCasClientInfo", method = GET)
    FastCasClientInfo getFastCasClientInfo(@RequestParam("clusterConfigId") Long clusterConfigId);

    @RequestMapping(value = "/getByAppNames", method = POST)
    List<FastCasClientInfo> findByAppNames(
            @RequestParam(value = "appNames", required = false) String[] appNames,
            @RequestParam(value = "envType", required = false) String envType,
            @RequestParam(value = "type", required = false) String type);

    @RequestMapping(value = "/findOfIamServers", method = GET)
    List<FastCasClientInfo> findOfIamServers();

}