/*
 * Copyright 2018, D3X Systems - All Rights Reserved
 *
 * Licensed under a proprietary end-user agreement issued by D3X Systems.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.d3xsystems.com/terms/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.http.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collections;

import com.d3x.core.util.Option;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests that the IP filter behaves as expected
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpIpFilterTest {

    private HttpServer server;


    @BeforeClass()
    public void startup() throws IOException {
        this.server = HttpTestUtils.start("/", Collections.singletonList(new HttpIpFilter("234.234.234.235")));
    }


    @AfterClass()
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }


    @Test()
    public void reject() throws Exception {
        final int port = server.getPort();
        Assert.assertTrue(server.isStarted());
        final HttpURLConnection conn = HttpTestUtils.doGet(port, "/hello", Option.empty());
        Assert.assertEquals(403, conn.getResponseCode());
    }

}
