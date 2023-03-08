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
package com.wl4g.iam.core.subject;

import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;
import static java.lang.System.out;

import com.wl4g.infra.context.utils.bean.BeanCopierUtils;
import com.wl4g.iam.common.subject.SimpleIamPrincipal;

/**
 * {@link SimplePrincipalInfoTests}
 *
 * @author James Wong<jamewong1376@gmail.com>
 * @version v1.0 2020年7月16日
 * @since
 */
public class SimplePrincipalInfoTests {

	public static void main(String[] args) {
		serializationTest1();
	}

	static void serializationTest1() {
		//
		// 注：bean的setter方法必须是标准的，如：setter方法有返回值也会导致无法复制
		//
		SimpleIamPrincipal p1 = new SimpleIamPrincipal();
		p1.setPrincipalId("001");
		p1.setPrincipal("zs");
		p1.attributes().put("aa", "11");
		out.println("source p1 object: " + toJSONString(p1) + ", hashCode: " + p1.hashCode());

		SimpleIamPrincipal p2 = BeanCopierUtils.clone(p1);
		out.println("clone p2 object: " + toJSONString(p2) + ", hashCode: " + p2.hashCode());
	}

}