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

import java.net.URL;
import javax.servlet.http.HttpServletRequest;

/**
 * A utility class with some useful Http Server related functions
 *
 * @author Xavier Witdouck
 */
public class HttpUtils {


    /**
     * Returns the protocol string inferred from request
     * @param req   the servlet request descriptor
     * @return      the protocol, either http or https
     */
    public static String getProtocol(HttpServletRequest req) {
        final String proto = req.getHeader("X-Forwarded-Proto");
        return proto != null ? proto : "http";

    }

    /**
     * Returns the server base url inferred from the request descriptor
     * @param req   the servlet request descriptor
     * @return      the server base url
     */
    public static String getBaseUrl(HttpServletRequest req) {
        try {
            final String proto = getProtocol(req);
            final String urlString = req.getRequestURL().toString().replace("http", proto);
            final URL url = new URL(urlString);
            final int port = url.getPort();
            if (port == url.getDefaultPort()) {
                return String.format("%s://%s", proto, url.getHost());
            } else {
                return String.format("%s://%s:%s", proto, url.getHost(), port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to resolve base url from http request:" + req.getRequestURI(), ex);
        }
    }


    /**
     * Returns the full request path by combining servlet path and path info from request
     * @param req   the http servlet request
     * @return      the full path for request
     */
    public static String getRequestPath(HttpServletRequest req) {
        final StringBuilder path = new StringBuilder();
        path.append(fixPath(req.getServletPath()));
        path.append(fixPath(req.getPathInfo()));
        return path.toString();
    }


    /**
     * Removes a trailing front slash from path if it exists
     * @param path  the path to trim trailing front slash
     * @return      the trimmed path
     */
    static String fixPath(String path) {
        if (path == null || path.trim().length() == 0) {
            return "";
        } else {
            String result = path.trim();
            result = result.startsWith("/") ? result : "/" + result;
            final int length = result.length();
            return result.charAt(length-1) == '/' ? result.substring(0, length-1) : result;
        }
    }


    /**
     * Returns the http port for URL specified
     * @param url   the http port
     * @return      the port, -1 if cannot resolve a port
     */
    public static int getPort(URL url) {
        final int port = url.getPort();
        if (port > 0) {
            return port;
        } else {
            final String protocol = url.getProtocol();
            if (protocol.equalsIgnoreCase("https")) {
                return 443;
            } else if (protocol.equalsIgnoreCase("http")) {
                return 80;
            } else {
                return -1;
            }
        }
    }


    /**
     * Returns the full request url, optional including the query string appended
     * @param req       the servlet request descriptor
     * @param query     the query string
     * @return          the request url
     */
    public static String getRequestUrl(HttpServletRequest req, boolean query) {
        if (!query) {
            final String proto = getProtocol(req);
            return req.getRequestURL().toString().replace("http", proto);
        } else {
            final String proto = getProtocol(req);
            final String urlString = req.getRequestURL().toString().replace("http", proto);
            final String queryString = req.getQueryString();
            return queryString != null ? urlString + "?" + queryString : urlString;
        }
    }
}
