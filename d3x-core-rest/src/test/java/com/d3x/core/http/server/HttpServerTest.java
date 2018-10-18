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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Unit test for the HttpServer class
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpServerTest {

    private HttpServer server;
    private String username = "doej";


    @BeforeClass()
    public void startup() throws IOException {
        log.info("STARTING THE HTTP SERVER");
        final List<String> openPaths = List.of("/open");
        final String loopback = InetAddress.getLoopbackAddress().getHostAddress();
        final Function<String,Boolean> verifier = user -> user.equalsIgnoreCase(username);
        this.server = HttpTestUtils.start("/", Arrays.asList(
            new HttpIpFilter(loopback),
            new HttpAuthFilter1("X-Remote-User", openPaths, verifier),
            new HttpCorsFilter(),
            new HttpTestFilter()
        ));
    }


    @AfterClass()
    public void shutdown() {
        log.info("STOPPING THE HTTP SERVER");
        if (server != null) {
            server.stop();
        }
    }


    @Test()
    public void openPath() throws Exception {
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port,"/open", Option.empty());
        final int statusCode = conn.getResponseCode();
        final String responseText = IO.readText(conn.getInputStream());
        Assert.assertEquals(statusCode, 200);
        Assert.assertEquals(responseText, "It Worked!");
    }


    @Test()
    public void securePathWithUser() throws Exception {
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port,"/secure", Option.of("doej"));
        final int statusCode = conn.getResponseCode();
        final String responseText = IO.readText(conn.getInputStream());
        Assert.assertEquals(statusCode, 200);
        Assert.assertEquals(responseText, "It Worked!");
    }


    @Test()
    public void securePathWithNoUser() throws Exception {
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port,"/secure", Option.empty());
        final int statusCode = conn.getResponseCode();
        Assert.assertEquals(statusCode, 401);
    }


    @Test()
    public void securePathWithWrongUser() throws Exception {
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port,"/secure", Option.of("xxxxxxx"));
        final int statusCode = conn.getResponseCode();
        Assert.assertEquals(statusCode, 401);
    }


    @Test()
    public void cors() throws Exception {
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port,"/open", Option.empty());
        final int statusCode = conn.getResponseCode();
        final String responseText = IO.readText(conn.getInputStream());
        Assert.assertEquals(statusCode, 200);
        Assert.assertEquals(responseText, "It Worked!");
        Assert.assertEquals(conn.getHeaderField("Access-Control-Allow-Origin"), "*");
        Assert.assertEquals(conn.getHeaderField("Access-Control-Allow-Methods"), "GET, OPTIONS, HEAD, PUT, POST");
        Assert.assertEquals(conn.getHeaderField("Access-Control-Allow-Headers"), "X-Requested-With, Content-Type");
    }


    @Test()
    public void query() throws Exception {
        final int port = server.getPort();
        final String query = "param1=value1&param2=value2";
        final String expected = "http://localhost:" + server.getPort() + "/open?" + query;
        final HttpURLConnection conn = HttpTestUtils.doGet(port,"/open?" + query, Option.empty());
        final int statusCode = conn.getResponseCode();
        final String responseText = IO.readText(conn.getInputStream());
        Assert.assertEquals(statusCode, 200);
        Assert.assertEquals(responseText, "It Worked!");
        Assert.assertEquals(expected, conn.getHeaderField("requestUrl"));
    }


    @Test()
    public void paths() {
        Assert.assertEquals(HttpUtils.fixPath("/"), "");
        Assert.assertEquals(HttpUtils.fixPath("/hello/world/"), "/hello/world");
        Assert.assertEquals(HttpUtils.fixPath("hello/world/"), "/hello/world");
    }


    /**
     * A simple test filter to make some assertions
     */
    private class HttpTestFilter implements Filter {
        @Override
        public void init(FilterConfig config) {}
        @Override
        public void destroy() {}
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException {
            OutputStream os = null;
            try {
                final HttpServletRequest request = (HttpServletRequest)req;
                final HttpServletResponse response = (HttpServletResponse)res;
                final String baseUrl = HttpUtils.getBaseUrl(request);
                final String requestUrl = HttpUtils.getRequestUrl(request, true);
                Assert.assertEquals(baseUrl, "http://localhost:" + server.getPort());
                response.addHeader("requestUrl", requestUrl);
                response.setStatus(200);
                os = new BufferedOutputStream(response.getOutputStream());
                os.write("It Worked!".getBytes());
            } finally {
                IO.close(os);
            }
        }
    }

}
