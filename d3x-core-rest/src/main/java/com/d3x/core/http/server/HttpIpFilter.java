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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet filter that rejects requests that do not come from IP addresses in a whitelist
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpIpFilter implements Filter {

    private Set<String> ipSet = new HashSet<>();

    /**
     * Constructor
     * @param ipAddress the one IP address to accept
     */
    public HttpIpFilter(String ipAddress) {
        this(Collections.singleton(ipAddress));
    }

    /**
     * Constructor
     * @param ipSet  the set of allowed IP addresses
     */
    public HttpIpFilter(Collection<String> ipSet) {
        this.ipSet.addAll(ipSet);
    }

    @Override
    public void destroy() { }
    @Override
    public void init(FilterConfig config)  {}
    @Override
    public final void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (ipSet.isEmpty()) {
            chain.doFilter(req, res);
        } else {
            final String remoteIp = req.getRemoteAddr();
            if (!ipSet.contains(remoteIp)) {
                log.warn("Request from rogue IP address: " + remoteIp);
                final String message = "Your IP address (" + remoteIp + ") is not allowed to access this server";
                final HttpServletResponse response = (HttpServletResponse)res;
                response.sendError(403, message);
            } else {
                chain.doFilter(req, res);
            }
        }
    }
}