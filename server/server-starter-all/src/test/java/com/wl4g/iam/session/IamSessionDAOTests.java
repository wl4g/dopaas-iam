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
package com.wl4g.iam.session;

import static com.google.common.base.Charsets.UTF_8;
import static com.wl4g.iam.common.constant.FastCasIAMConstants.CACHE_PREFIX_IAM_SESSION;

import java.io.IOException;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.wl4g.StandaloneIam;
import com.wl4g.infra.common.jedis.JedisService;
import com.wl4g.infra.common.jedis.cursor.ScanCursor;
import com.wl4g.infra.common.jedis.cursor.ScanCursor.ClusterScanParams;
import com.wl4g.iam.core.session.IamSession;
import com.wl4g.iam.core.session.mgt.IamSessionDAO;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneIam.class)
@FixMethodOrder(MethodSorters.JVM)
public class IamSessionDAOTests {

    @Autowired
    private JedisService jedisService;

    @Autowired
    private IamSessionDAO sessionDAO;

    @Test
    public void setSerializeTest() {
        IamSession s = new IamSession();
        s.setId("abcd123");
        String res = jedisService.setObjectT("iam:session:1c315080e64b4731b011a14551a54c92", s, 0);
        System.out.println("setSerializeTest	result: " + res);
    }

    @Test
    public void getDeserializeTest() {
        IamSession s = jedisService.getObjectT("iam:session:1c315080e64b4731b011a14551a54c92", IamSession.class);
        System.out.println("getDeserializeTest	IamSession: " + s);
    }

    @Test
    public void scanCursorTest() throws IOException {
        byte[] pattern = ("iam_" + CACHE_PREFIX_IAM_SESSION + "*").getBytes(UTF_8);
        ClusterScanParams params = new ClusterScanParams(200, pattern);

        ScanCursor<IamSession> sc = new ScanCursor<IamSession>(jedisService.getJedisClient(), null, params) {
        }.open();
        System.out.println("ScanResult: " + sc);
        while (sc.hasNext()) {
            System.out.println("IamSession: " + sc.next());
        }

    }

    @Test
    public void getAccessSessionsTests() {
        ScanCursor<IamSession> ss = sessionDAO.getAccessSessions(200);
        while (ss.hasNext()) {
            IamSession s = ss.next();
            System.out.println(s);
        }

    }

}