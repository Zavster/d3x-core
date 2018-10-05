/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.http.client;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.d3x.core.http.server.HttpServer;
import com.d3x.core.http.server.HttpUtils;
import com.d3x.core.json.Json;
import com.d3x.core.util.Generic;
import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import com.google.gson.Gson;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * A test of the HttpClient adapter
 *
 * @author Xavier Witdouck
 */
public class HttpClientTest {

    private HttpServer server;

    /**
     * Convenience factory method to start a single context HttpServer with a bunch of filters
     * @param filters       the filters for context
     * @return              the newly created server
     */
    static HttpServer start(List<Filter> filters) throws IOException {
        Objects.requireNonNull(filters, "The filters cannot be null");
        final EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC);
        final ServletContextHandler context = new ServletContextHandler(null, "/");
        context.setSessionHandler(new SessionHandler());
        filters.forEach(v -> context.addFilter(new FilterHolder(v), "/*", dispatches));
        final ServerSocket socket = new ServerSocket(0);
        final int port = socket.getLocalPort();
        final HttpServer server = new HttpServer(port, false);
        socket.close();
        server.addContext(context);
        server.start();
        return server;
    }


    @BeforeClass()
    public void startup() throws IOException {
        this.server = start(List.of(new HttpTestFilter()));
    }


    @AfterClass()
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }


    /**
     * Returns a local URL to connect to embedded server
     * @param path      the URL path
     * @return          the new URL
     */
    private String localUrl(String path) {
        return "http://localhost:" + server.getPort() + path;
    }


    @DataProvider(name="clients")
    public Object[][] clients() {
        return new Object[][] { { new HttpClientJdk() }};
    }


    @Test(dataProvider="clients")
    public void setDefault(HttpClient client) {
        HttpClient.setDefault(client);
        Assert.assertSame(HttpClient.getDefault(), client);
    }


    @Test(dataProvider="clients", expectedExceptions={ HttpException.class })
    public void malformedUrl(HttpClient client) {
        client.doGet("http://lskdfjlsafdjl", null);
    }


    @Test(dataProvider="clients")
    public void doGet(HttpClient client) {
        final AtomicBoolean responded = new AtomicBoolean();
        final String result = client.doGet(localUrl("/hello/world"), req -> {
            req.setContentType("text/test");
            req.addHeader("Test-Header-1", "Test-Cookie-Value-1");
            req.addHeader("Test-Header-2", "Test-Cookie-Value-2");
            req.addHeader("Test-Cookie-1", "Test-Cookie-Value-1");
            req.addHeader("Test-Cookie-2", "Test-Cookie-Value-2");
            req.setResponseHandler(response -> {
                responded.set(true);
                Assert.assertEquals(response.getStatus().getCode(), 200);
                Assert.assertTrue(response.getHeader("Received-Method").isPresent());
                Assert.assertTrue(response.getHeader("Received-Content-Type").isPresent());
                Assert.assertTrue(response.getHeader("Received-Content-Length").isPresent());
                Assert.assertTrue(response.getHeader("Received-Test-Header-1").isPresent());
                Assert.assertTrue(response.getHeader("Received-Test-Header-2").isPresent());
                Assert.assertTrue(response.getHeader("Received-Test-Cookie-1").isPresent());
                Assert.assertTrue(response.getHeader("Received-Test-Cookie-2").isPresent());
                Assert.assertEquals(response.getHeaders().size(), 16);
                Assert.assertEquals(response.getHeaders("Received-Method").size(), 1);
                Assert.assertEquals(response.getHeader("Received-Method").get().getValue(), "GET");
                Assert.assertEquals(response.getHeader("Received-Content-Type").get().getValue(), "text/test");
                Assert.assertEquals(response.getHeader("Received-Content-Length").get().getValue(), "-1");
                Assert.assertEquals(response.getHeader("Received-Test-Header-1").get().getValue(), "Test-Cookie-Value-1");
                Assert.assertEquals(response.getHeader("Received-Test-Header-2").get().getValue(), "Test-Cookie-Value-2");
                Assert.assertEquals(response.getHeader("Received-Test-Cookie-1").get().getValue(), "Test-Cookie-Value-1");
                Assert.assertEquals(response.getHeader("Received-Test-Cookie-2").get().getValue(), "Test-Cookie-Value-2");
                return IO.readText(response.getStream());
            });
        });
        Assert.assertEquals(result, "Hello there!");
        Assert.assertTrue(responded.get());
    }


    @Test(dataProvider="clients")
    public void doPost(HttpClient client) {
        final AtomicBoolean responded = new AtomicBoolean();
        final Gson gson = Json.createGsonBuilder(Option.empty()).setPrettyPrinting().create();
        final List<LocalDate> dates = IntStream.range(0, 10).mapToObj(i -> LocalDate.now().plusDays(i)).collect(Collectors.toList());
        final String jsonString = gson.toJson(dates);
        final List<LocalDate> result = client.doPost(localUrl("/post/json"), req -> {
            req.setContentLength(0);
            req.setContentType("application/json");
            req.setContent(jsonString.getBytes());
            req.setResponseHandler(response -> {
                responded.set(true);
                Assert.assertEquals(response.getStatus().getCode(), 200);
                Assert.assertTrue(response.getHeader("Received-Method").isPresent());
                Assert.assertTrue(response.getHeader("Content-Type").isPresent());
                Assert.assertEquals(response.getHeader("Content-Type").get().getValue(), "application/json");
                final String jsonResponse = IO.readText(response.getStream());
                return gson.fromJson(jsonResponse, Generic.of(List.class, LocalDate.class));
            });
        });
        Assert.assertTrue(responded.get());
        Assert.assertEquals(result, dates);
    }


    @Test(dataProvider="clients", expectedExceptions={ SocketTimeoutException.class })
    public void readTimeOut(HttpClient client) throws Throwable {
        try {
            client.doGet(localUrl("/readTimeout"), request -> {
                request.setReadTimeout(1000);
                request.setResponseHandler(response -> {
                    Assert.assertNotEquals(200, response.getStatus().getCode());
                    return null;
                });
            });
        } catch (HttpException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw ex.getCause();
            }
        }
    }


    @Test(dataProvider="clients")
    public void retry(HttpClient client) {
        final AtomicBoolean responded = new AtomicBoolean();
        final String result = client.doGet(localUrl("/retry"), request -> {
            request.setReadTimeout(1000);
            request.setRetryCount(3);
            request.setResponseHandler(response -> {
                responded.set(true);
                Assert.assertEquals(200, response.getStatus().getCode());
                return IO.readText(response.getStream());
            });
        });
        Assert.assertTrue(responded.get());
        Assert.assertEquals(result, "It worked after 3 retries!");
    }



    @Test(dataProvider="clients")
    public void notFound(HttpClient client) {
        final AtomicBoolean responded = new AtomicBoolean();
        client.doGet(localUrl("/notFound"), request -> {
            request.setReadTimeout(1000);
            request.setRetryCount(3);
            request.setResponseHandler(response -> {
                responded.set(true);
                Assert.assertEquals(404, response.getStatus().getCode());
                return null;
            });
        });
        Assert.assertTrue(responded.get());
    }






    /**
     * A simple test filter to make some assertions
     */
    private class HttpTestFilter implements Filter {
        private AtomicInteger retryCount = new AtomicInteger(0);
        @Override
        public void init(FilterConfig config) {}
        @Override
        public void destroy() {}
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException {
            OutputStream os = null;
            try {
                final HttpServletRequest request = (HttpServletRequest) req;
                final HttpServletResponse response = (HttpServletResponse) res;
                final String baseUrl = HttpUtils.getBaseUrl(request);
                final String requestUrl = HttpUtils.getRequestUrl(request, true);
                final String requestPath = HttpUtils.getRequestPath(request);
                final Enumeration<String> headers = request.getHeaderNames();
                response.addHeader("Received-Method", request.getMethod());
                response.addHeader("Received-Content-Type", request.getContentType());
                response.addHeader("Received-Content-Length", String.valueOf(request.getContentLength()));
                while (headers.hasMoreElements()) {
                    final String name = headers.nextElement();
                    final String value = request.getHeader(name);
                    response.addHeader("Received-" + name, value);
                }
                final Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : request.getCookies()) {
                        response.addHeader("Received-" + cookie.getName(), cookie.getValue());
                    }
                }
                Assert.assertEquals(baseUrl, "http://localhost:" + server.getPort());
                response.addHeader("requestUrl", requestUrl);
                if (requestPath.equals("/hello/world") && request.getMethod().equals("GET")) {
                    response.setStatus(200);
                    os = new BufferedOutputStream(response.getOutputStream());
                    os.write("Hello there!".getBytes());
                } else if (requestPath.equals("/readTimeout")) {
                    Thread.sleep(2000);
                } else if (requestPath.equals("/retry") && request.getMethod().equals("GET")) {
                    final int retry = retryCount.incrementAndGet();
                    if (retry < 3) Thread.sleep(2000);
                    response.setStatus(200);
                    os = new BufferedOutputStream(response.getOutputStream());
                    os.write(String.format("It worked after %s retries!", retryCount.get()).getBytes());
                } else if (requestPath.equals("/post/json") || request.getMethod().equals("POST")) {
                    final String jsonRequest = IO.readText(request.getInputStream());
                    response.addHeader("Content-Type", "application/json");
                    response.setStatus(200);
                    os = new BufferedOutputStream(response.getOutputStream());
                    os.write(jsonRequest.getBytes());
                } else {
                    response.setStatus(404);
                    os = new BufferedOutputStream(response.getOutputStream());
                    os.write("Resource not found".getBytes());
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();;
            } finally {
                IO.close(os);
            }
        }
    }

}
