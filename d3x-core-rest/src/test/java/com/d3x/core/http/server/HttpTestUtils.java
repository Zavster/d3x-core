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

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.testng.Assert;

/**
 * Some useful utilities for testing the http server stack
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpTestUtils {

    /**
     * Convenience factory method to start a single context HttpServer with a bunch of filters
     * @param contextPath   the context path
     * @param filters       the filters for context
     * @return              the newly created server
     */
    static HttpServer start(String contextPath, List<Filter> filters) throws IOException {
        Objects.requireNonNull(contextPath, "The content path cannot be null");
        Objects.requireNonNull(filters, "The filters cannot be null");
        final EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC);
        final ServletContextHandler context = new ServletContextHandler(null, contextPath);
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


    /**
     * Convenience HTTP GET implementation for performing localhost tests
     * @param port      the http listen port
     * @param path      the path to add to http://localhost
     * @return          the resulting connection
     */
    public static HttpURLConnection doGet(int port, String path) {
        return doGet(port, path, Option.empty(), Option.empty());
    }


    /**
     * Convenience HTTP GET implementation for performing localhost tests
     * @param port      the http listen port
     * @param path      the path to add to http://localhost
     * @param user      the optional user in header
     * @return          the resulting connection
     */
    public static HttpURLConnection doGet(int port, String path, Option<String> user) {
        return doGet(port, path, user, Option.empty());
    }


    /**
     * Convenience HTTP GET implementation for performing localhost tests
     * @param port      the http listen port
     * @param path      the path to add to http://localhost
     * @param user      the optional user in header
     * @return          the resulting connection
     */
    public static HttpURLConnection doGet(int port, String path, Option<String> user, Option<String> cookie) {
        URL url = null;
        try {
            url = new URL("http://localhost:" + port + path);
            Assert.assertEquals(port, HttpUtils.getPort(url));
            log.info("Calling " + url);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            user.ifPresent(v -> conn.setRequestProperty("X-Remote-User", v));
            cookie.ifPresent(v -> conn.setRequestProperty("Cookie", v));
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            return conn;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get " + url, ex);
        }
    }



    /**
     * Convenience HTTP POST implementation for performing localhost tests
     * @param port          the http listen port
     * @param path          the path to add to http://localhost
     * @param user          the optional user in header
     * @param contentType   the content type for post
     * @param content       the content to post
     * @return              the resulting connection
     */
    public static HttpURLConnection doPost(int port, String path, Option<String> user, String contentType, String content) {
        URL url = null;
        OutputStream os = null;
        try {
            url = new URL("http://localhost:" + port + path);
            Assert.assertEquals(port, HttpUtils.getPort(url));
            final byte[] bytes = content.getBytes();
            final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            user.ifPresent(v -> conn.setRequestProperty("X-Remote-User", v));
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            conn.setInstanceFollowRedirects(false);
            conn.setDoOutput(true);
            os = new BufferedOutputStream(conn.getOutputStream());
            os.write(bytes);
            return conn;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get " + url, ex);
        } finally {
            IO.close(os);
        }
    }

}
