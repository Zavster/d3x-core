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
package com.d3x.core.http.rest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.d3x.core.http.server.HttpAuthFilter1;
import com.d3x.core.http.server.HttpCorsFilter;
import com.d3x.core.http.server.HttpIpFilter;
import com.d3x.core.http.server.HttpServer;
import com.d3x.core.util.Modules;

/**
 * Unit tests for the REST api framework
 *
 * @author Xavier Witdouck
 */
public class RestServers {


    /**
     * Convenience factory method to start a single context HttpServer with rest modules
     * @param modules   the modules containing JAX-RS annotated components
     * @param username  the username in which requests will be made that are valid
     * @return          the newly created server
     */
    static HttpServer start(String username, Modules modules) throws IOException {
        Objects.requireNonNull(modules, "The modules cannot be null");
        final List<String> openPaths = List.of("/open");
        final String loopback = InetAddress.getLoopbackAddress().getHostAddress();
        final Function<String,Boolean> verifier = user -> user.equalsIgnoreCase(username);
        final RestContext context = new RestContext(modules, "/", Arrays.asList(
            new HttpIpFilter(loopback),
            new HttpAuthFilter1("X-Remote-User", openPaths, verifier),
            new HttpCorsFilter()
        ));
        final ServerSocket socket = new ServerSocket(0);
        final int port = socket.getLocalPort();
        final HttpServer server = new HttpServer(port, false, context);
        socket.close();
        server.start();
        return server;
    }

}
