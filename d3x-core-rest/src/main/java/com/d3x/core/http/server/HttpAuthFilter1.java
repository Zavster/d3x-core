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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.d3x.core.util.User;

/**
 * A Servlet filter that checks to see if a username has been set in the request header.
 *
 * @author Xavier Witdouck
 */
public class HttpAuthFilter1 implements Filter {

    private String userParam;
    private Function<String,Boolean> userVerifier;
    private Set<String> openPathSet = new HashSet<>();


    /**
     * Constructor
     * @param authHeader    the name of header parameter with authenticated username
     * @param openPaths     the list of servlet paths that do not require authentication
     * @param userVerifier  the function that verifies if a username is valid
     */
    public HttpAuthFilter1(String authHeader, List<String> openPaths, Function<String,Boolean> userVerifier) {
        Objects.requireNonNull(authHeader, "The user header param cannot be null");
        Objects.requireNonNull(userVerifier, "The user verifier function cannot be null");
        this.userParam = authHeader;
        this.openPathSet.addAll(openPaths);
        this.userVerifier = userVerifier;
    }

    @Override
    public void destroy() {}
    @Override
    public void init(FilterConfig filterConfig) {}
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest)req;
        final HttpServletResponse response = (HttpServletResponse)res;
        final String servletPath = request.getServletPath() != null ? request.getServletPath() : "";
        User.reset();
        if (openPathSet.contains(servletPath)) {
            chain.doFilter(req, res);
        } else {
            final String username = request.getHeader(userParam);
            if (username == null) {
                response.sendError(401, "No username parameter in request header");
            } else {
                try {
                    final Boolean valid = userVerifier.apply(username.toLowerCase());
                    if (valid == null || !valid) {
                        response.sendError(401, "No user exists for " + username);
                    } else {
                        User.setCurrentUser(username.toLowerCase());
                        chain.doFilter(req, res);
                    }
                } finally {
                    User.reset();
                }
            }
        }
    }
}
