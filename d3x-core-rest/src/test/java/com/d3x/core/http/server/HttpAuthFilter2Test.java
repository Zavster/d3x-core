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
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import com.google.gson.GsonBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the auth filter 2 implementation
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpAuthFilter2Test {

    private HttpServer server;
    private String sessionCookie;
    private HttpAuthenticator authenticator = (username, password) -> {
        if (!username.equalsIgnoreCase("doej")) {
            throw new SecurityException("Unknown user specified");
        } else if (!new String(password).equals("password")) {
            throw new SecurityException("Invalid password");
        }
    };


    @BeforeClass()
    public void startup() throws IOException {
        log.info("STARTING THE HTTP SERVER");
        this.server = HttpTestUtils.start("/", Arrays.asList(
            new HttpAuthFilter2(List.of("/open"), authenticator, createAuthTokenManager()),
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


    /**
     * Returns the token manager for this test
     * @return  the token manager
     */
    private HttpAuthToken.Manager createAuthTokenManager() {
        return new HttpAuthToken.Manager() {
            private Map<String, HttpAuthToken> tokenMap = new HashMap<>();
            @Override
            public void add(HttpAuthToken token) {
                tokenMap.put(token.getUsername(), token);
            }
            @Override
            public Stream<HttpAuthToken> getTokens() {
                return tokenMap.values().stream();
            }
        };
    }


    @Test()
    public void openPath() throws Exception {
        final int port = server.getPort();
        Assert.assertTrue(server.isStarted());
        final HttpURLConnection conn = HttpTestUtils.doGet(port, "/open", Option.empty());
        Assert.assertEquals(200, conn.getResponseCode());
        Assert.assertEquals(IO.readText(conn.getInputStream()), "It Worked!");
    }


    @Test(dependsOnMethods={"openPath"})
    public void redirect() throws Exception {
        final int port = server.getPort();
        Assert.assertTrue(server.isStarted());
        final HttpURLConnection conn = HttpTestUtils.doGet(port, "/hello", Option.empty());
        final String location = conn.getHeaderField( "Location" );
        Assert.assertEquals(302, conn.getResponseCode());
        Assert.assertEquals(location, "http://localhost:" + port + "/account/login?next=http%3A%2F%2Flocalhost%3A" + port + "%2Fhello");
    }


    @Test(dependsOnMethods={"redirect"})
    public void loginUnknownUser() throws IOException {
        final int port = server.getPort();
        final HttpAuthFilter2.LoginRequest request = new HttpAuthFilter2.LoginRequest("unknown", "password", false);
        final String jsonString = new GsonBuilder().create().toJson(request);
        final HttpURLConnection conn = HttpTestUtils.doPost(port, "/account/login", Option.empty(), "application/json", jsonString);
        Assert.assertEquals(401, conn.getResponseCode());
        Assert.assertEquals(conn.getResponseMessage(), "Unknown user specified");
    }


    @Test(dependsOnMethods={"redirect"})
    public void loginWrongPassword() throws IOException {
        final int port = server.getPort();
        final HttpAuthFilter2.LoginRequest request = new HttpAuthFilter2.LoginRequest("doej", "wrong", false);
        final String jsonString = new GsonBuilder().create().toJson(request);
        final HttpURLConnection conn = HttpTestUtils.doPost(port, "/account/login", Option.empty(), "application/json", jsonString);
        Assert.assertEquals(401, conn.getResponseCode());
        Assert.assertEquals(conn.getResponseMessage(), "Invalid password");
    }


    @Test(dependsOnMethods={"redirect"})
    public void loginSuccess() throws IOException {
        final int port = server.getPort();
        final HttpAuthFilter2.LoginRequest request = new HttpAuthFilter2.LoginRequest("doej", "password", false);
        final String jsonString = new GsonBuilder().create().toJson(request);
        final HttpURLConnection conn = HttpTestUtils.doPost(port, "/account/login", Option.empty(), "application/json", jsonString);
        this.sessionCookie = conn.getHeaderField("Set-Cookie");
        Assert.assertEquals(200, conn.getResponseCode());
        Assert.assertNotNull(sessionCookie);
        Assert.assertTrue(sessionCookie.contains("JSESSIONID"));
    }


    @Test(dependsOnMethods={"loginSuccess"})
    public void request() throws Exception {
        Assert.assertNotNull(sessionCookie);
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port, "/download", Option.empty(), Option.of(sessionCookie));
        Assert.assertEquals(200, conn.getResponseCode());
        Assert.assertEquals(IO.readText(conn.getInputStream()), "It Worked!");
    }


    @Test(dependsOnMethods={"request"})
    public void logout() throws Exception {
        Assert.assertNotNull(sessionCookie);
        final int port = server.getPort();
        final HttpURLConnection conn = HttpTestUtils.doGet(port, "/account/logout", Option.empty(), Option.of(sessionCookie));
        final String location = conn.getHeaderField( "Location" );
        Assert.assertEquals(302, conn.getResponseCode());
        Assert.assertEquals(location, "http://localhost:" + port + "/account/login?next=http%3A%2F%2Flocalhost%3A" + port);
    }


    @Test(dependsOnMethods={"logout"})
    public void redirectAgain() throws Exception {
        redirect();
    }


    private class HttpTestFilter implements Filter {
        @Override
        public void init(FilterConfig config) {}
        @Override
        public void destroy() {}
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException {
            OutputStream os = null;
            try {
                final HttpServletResponse response = (HttpServletResponse)res;
                response.setStatus(200);
                os = new BufferedOutputStream(response.getOutputStream());
                os.write("It Worked!".getBytes());
            } finally {
                IO.close(os);
            }
        }
    }

}
