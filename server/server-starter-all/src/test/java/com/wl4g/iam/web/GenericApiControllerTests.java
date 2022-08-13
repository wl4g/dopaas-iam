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

import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

import com.wl4g.StandaloneIam;
import com.wl4g.iam.core.web.BaseApiController;
import com.wl4g.iam.core.web.model.SessionQueryModel;
import com.wl4g.infra.common.web.rest.RespBase;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneIam.class)
@FixMethodOrder(MethodSorters.JVM)
public class GenericApiControllerTests {

    @Autowired
    private BaseApiController endpoint;

    @Test
    public void scanSessionsTest1() throws Exception {
        RespBase<?> resp = endpoint.getSessions(new SessionQueryModel(), new MockHttpServletRequest());
        System.out.println(toJSONString(resp));
    }

}