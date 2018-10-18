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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContextEvent;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;

import com.d3x.core.util.Modules;

/**
 * A Supplier that creates a ContextHandler for a Jetty Server which initializes a RestApp using REST Easy
 *
 * @author Xavier Witdouck
 */
public class RestContext implements Supplier<ContextHandler> {

    private Modules modules;
    private String contextPath;
    private List<Filter> filters = new ArrayList<>();

    /**
     * Constructor
     * @param modules       the runtime modules
     * @param contextPath   the context path
     */
    public RestContext(Modules modules, String contextPath) {
        this(modules, contextPath, Collections.emptyList());
    }

    /**
     * Constructor
     * @param modules       the runtime modules
     * @param contextPath   the context path
     * @param filters       the filters for this context
     */
    public RestContext(Modules modules, String contextPath, List<Filter> filters) {
        this.modules = modules;
        this.contextPath = contextPath;
        this.filters.addAll(filters);
        this.filters.add(new FilterDispatcher());
    }


    @Override
    public ContextHandler get() {
        final ServletContextHandler context = new ServletContextHandler();
        this.addFilters(context);
        context.setContextPath(contextPath);
        context.addServlet(new ServletHolder(new DefaultServlet()), "/*");
        context.setBaseResource(Resource.newClassPathResource("/static"));
        context.setSessionHandler(new SessionHandler());
        context.addEventListener(createBootstrap());
        return context;
    }


    /**
     * Returns the REST easy initializer that injects runtime modules for RestApp
     * @return      the REST easy initializer
     */
    private ResteasyBootstrap createBootstrap() {
        return new ResteasyBootstrap() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                super.contextInitialized(event);
                deployment.getDispatcher().getDefaultContextObjects().put(Modules.class, modules);
            }
        };
    }


    /**
     * Adds all required filters to this context
     * @param context   the context reference
     */
    private void addFilters(ServletContextHandler context) {
        final EnumSet<DispatcherType> dispatches = EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC);
        this.filters.forEach(filter -> {
            final FilterHolder filterHolder = new FilterHolder(filter);
            context.addFilter(filterHolder, "/*", dispatches);
            if (filter instanceof FilterDispatcher) {
                filterHolder.setInitParameter("javax.ws.rs.Application", RestApp.class.getName());
            }
        });
    }
}
