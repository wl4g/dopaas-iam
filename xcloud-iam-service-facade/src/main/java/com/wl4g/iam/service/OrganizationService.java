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
package com.wl4g.iam.service;

import com.wl4g.component.rpc.springboot.feign.annotation.SpringBootFeignClient;
import com.wl4g.iam.common.bean.Organization;
import com.wl4g.iam.common.bean.User;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * {@link OrganizationService}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @author vjay
 * @date 2019-10-29
 * @sine v1.0
 * @see
 */
@SpringBootFeignClient("${spring.cloud.provider-service:organization-service}")
@RequestMapping("/organization")
public interface OrganizationService {

	@RequestMapping(value = "/save", method = POST)
	void save(Organization org);

	@RequestMapping(value = "/del", method = POST)
	void del(Long id);

	@RequestMapping(value = "/detail", method = GET)
	Organization detail(Long id);

	@RequestMapping(value = "/getLoginOrganizationTree", method = POST)
	List<Organization> getLoginOrganizationTree();

	@RequestMapping(value = "/getGroupsSet", method = POST)
	Set<Organization> getUserOrganizations(@RequestBody User user);

}