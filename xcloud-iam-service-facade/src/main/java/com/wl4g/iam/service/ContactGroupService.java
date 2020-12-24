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

import com.wl4g.component.core.bean.model.PageHolder;
import com.wl4g.component.rpc.springboot.feign.annotation.SpringBootFeignClient;
import com.wl4g.iam.common.bean.ContactGroup;

import static org.springframework.web.bind.annotation.RequestMethod.*;


import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * {@link ContactGroupService}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @author vjay
 * @date 2019-08-05
 * @sine v1.0
 * @see
 */
@SpringBootFeignClient("contactGroupService")
@RequestMapping("/contactGroup")
public interface ContactGroupService {

	@RequestMapping(value = "/save", method = { POST })
	void save(ContactGroup contactGroup);

	@RequestMapping(value = "/del", method = { DELETE })
	void del(Long id);

	@RequestMapping(value = "/findContactGroups", method = { GET })
	List<ContactGroup> findContactGroups(String name);

	@RequestMapping(value = "/list", method = { GET })
	PageHolder<ContactGroup> list(PageHolder<ContactGroup> pm, String name);

}