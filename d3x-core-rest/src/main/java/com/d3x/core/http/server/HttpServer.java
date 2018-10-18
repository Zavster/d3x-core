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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.d3x.core.util.LifeCycle;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * A simple Http Server class that uses embedded Jetty as the underlying engine
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpServer extends LifeCycle.Base {

    private int port;
    private Server httpServer;
    private boolean blockOnStart;
    private List<Supplier<ContextHandler>> contextList = new ArrayList<>();

    /**
     * Constructor
     * @param port          the http listen port
     * @param blockOnStart  true to block when calling start
     */
    public HttpServer(int port, boolean blockOnStart) {
        this(port, blockOnStart, Collections.emptyList());
    }

    /**
     * Constructor
     * @param port          the http listen port
     * @param blockOnStart  true to block when calling start
     * @param context       the context for server
     */
    public HttpServer(int port, boolean blockOnStart, Supplier<ContextHandler> context) {
        this(port, blockOnStart, Collections.singletonList(context));
    }

    /**
     * Constructor
     * @param port          the http listen port
     * @param blockOnStart  true to block when calling start
     * @param contexts      the list of context suppliers
     */
    public HttpServer(int port, boolean blockOnStart, List<Supplier<ContextHandler>> contexts) {
        this.port = port;
        this.blockOnStart = blockOnStart;
        this.contextList.addAll(contexts);
    }


    /**
     * Returns the listen port for server
     * @return  the listen port for server
     */
    public int getPort() {
        return port;
    }

    /**
     * Adds a context to this server, which must be done prior to start
     * @param context   the context handler to add
     */
    public void addContext(ContextHandler context) {
        if (isStarted()) {
            throw new IllegalStateException("The HttpServer has already been started");
        } else {
            this.contextList.add(() -> context);
        }
    }


    /**
     * Adds a context supplier to this server, which must be done prior to start
     * @param context   the context handler to add
     */
    public void addContext(Supplier<ContextHandler> context) {
        if (isStarted()) {
            throw new IllegalStateException("The HttpServer has already been started");
        } else {
            this.contextList.add(context);
        }
    }


    /**
     * Adds a context to expose static content given the resource path
     * @param contextPath       the context path for handler
     * @param resourceBase      the resource base path to expose on context path
     * @throws IllegalStateException    if the server has already been started
     */
    public void addContext(String contextPath, String resourceBase) {
        if (isStarted()) {
            throw new IllegalStateException("The HttpServer has already been started");
        } else {
            final ResourceHandler resource = new ResourceHandler();
            final ContextHandler context = new ContextHandler(contextPath);
            context.setResourceBase(resourceBase);
            context.setHandler(new ResourceHandler());
            resource.setDirAllowed(false);
            this.contextList.add(() -> context);
        }
    }


    /**
     * Returns the newly created contexts for this server
     * @return  the newly created contexts
     */
    private Stream<ContextHandler> createContexts() {
        if (contextList.isEmpty()) {
            throw new IllegalStateException("No contexts configured for Http server");
        } else {
            return contextList.stream().map(supplier -> {
                final ContextHandler context = supplier.get();
                log.info("Created http context for path: " + context.getContextPath());
                return context;
            });
        }
    }

    @Override
    protected void doStart() throws RuntimeException {
        try {
            final ContextHandler[] contexts = createContexts().toArray(ContextHandler[]::new);
            log.info("Starting Http Server on port: " + port + " with " + contexts.length + " context(s)");
            final ContextHandlerCollection handler = new ContextHandlerCollection(contexts);
            this.httpServer = new Server(port);
            this.httpServer.setSessionIdManager(new DefaultSessionIdManager(httpServer));
            this.httpServer.setHandler(handler);
            this.httpServer.start();
            if (this.blockOnStart) {
                this.httpServer.join();
            }
        } catch (Exception ex) {
            this.stop();
            throw new RuntimeException("Failed to start Jetty Http server", ex);
        }
    }


    @Override
    protected void doStop() throws RuntimeException {
        try {
            log.info("Stopping Http Server on port " + port);
            this.httpServer.stop();
        } catch (Exception ex) {
            log.error("Failed to gracefully stop the Jetty Server", ex);
        }
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer(8002, true);
        server.addContext("/javadocs", "/Users/witdxav/Dropbox/dev/docs/jdk1.8/api");
        server.start();
    }
}
