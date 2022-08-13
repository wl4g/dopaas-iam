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
package com.wl4g.iam.common.session.mgt.support;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.wl4g.StandaloneIam;
import com.wl4g.iam.core.session.NoOpSession;
import com.wl4g.iam.core.session.mgt.support.IamUidSessionIdGenerator;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneIam.class)
@FixMethodOrder(MethodSorters.JVM)
public class IamUidSessionIdGeneratorTests {

	@Test
	public void test1() {
		IamUidSessionIdGenerator g = new IamUidSessionIdGenerator();
		System.out.println(g.generateId(new NoOpSession()));
	}

}