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
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.d3x.core.util.Crypto;
import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import com.d3x.core.util.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The filter that ensures requests to the Kronos server have been authenticated
 *
 * @link https://paragonie.com/blog/2015/04/secure-authentication-php-with-long-term-persistence#title.2
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class HttpAuthFilter2 implements Filter {

    private HttpAuthenticator authenticator;
    private HttpAuthToken.Manager authTokenManager;
    private Set<String> openPathSet = new HashSet<>();
    private Set<String> closePathSet = new HashSet<>();
    private List<Pattern> openPatternList = new ArrayList<>();
    private Map<String,HttpAuthToken> authTokenMap = new HashMap<>();


    /**
     * Constructor
     * @param openPaths         the list of servlet paths that do not require authentication
     * @param authenticator     the authenticator for this filter
     * @param authTokenManager  the auth token manager
     */
    public HttpAuthFilter2(List<String> openPaths, HttpAuthenticator authenticator, HttpAuthToken.Manager authTokenManager) {
        Objects.requireNonNull(authenticator, "The authenticator cannot be null");
        Objects.requireNonNull(authTokenManager, "The auth token manager cannot be null");
        this.openPathSet.addAll(openPaths != null ? openPaths : Collections.emptyList());
        this.authenticator = authenticator;
        this.authTokenManager = authTokenManager;
        this.openPathSet.forEach(path -> {
            this.openPatternList.add(Pattern.compile(path));
        });
    }


    @Override
    public void init(FilterConfig config) {
        try {
            this.authTokenManager.getTokens().forEach(token -> {
                this.authTokenMap.put(token.getTokenKey(), token);
            });
            /*
            this.database.registerMapper(AuthToken.class, new AuthTokenMapper());
            this.database.registerInsertBinder(AuthToken.class, new AuthTokenInserter());
            this.database.select(AuthToken.class).stream().forEach(token -> {
                this.authTokenMap.put(token.tokenKey, token);
            });
            */
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize authentication filter", ex);
        }
    }


    @Override
    public void destroy() {
        this.authTokenMap.clear();
    }


    @Override
    public final void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest)req;
        final HttpServletResponse response = (HttpServletResponse)res;
        final String requestPath = HttpUtils.getRequestPath(request);
        final String method = request.getMethod() != null ? request.getMethod() : "GET";
        try {
            User.reset();
            if (requestPath.equals("/account/login") && method.equalsIgnoreCase("POST")) {
                login(request, response);
            } else if (requestPath.equals("/account/logout")) {
                logout(request, response);
            } else if (isOpenPath(requestPath)) {
                chain.doFilter(req, res);
            } else {
                final HttpSession session = request.getSession(false);
                final String username = session != null ? (String)session.getAttribute("username") : null;
                if (session != null && username != null) {
                    User.setCurrentUser(username.toLowerCase());
                    chain.doFilter(req, res);
                } else if (isRememberMe(request, response)) {
                    chain.doFilter(req, res);
                } else {
                    final String baseUrl = HttpUtils.getBaseUrl(request);
                    final String nextUrl = HttpUtils.getRequestUrl(request, true);
                    final String redirectUrl = baseUrl + "/account/login?next=" + URLEncoder.encode(nextUrl, StandardCharsets.UTF_8);
                    response.sendRedirect(redirectUrl);
                }
            }
        } finally {
            User.reset();
        }
    }


    /**
     * Returns true if the request path does not require authentication
     * @param pathInfo  the request path info
     * @return          true if path is open
     */
    private boolean isOpenPath(String pathInfo) {
        if (closePathSet.contains(pathInfo)) {
            return false;
        } else if (openPathSet.contains(pathInfo)) {
            return true;
        } else {
            for (Pattern pattern : openPatternList) {
                final Matcher matcher = pattern.matcher(pathInfo);
                if (matcher.matches()) {
                    this.openPathSet.add(pathInfo);
                    return true;
                }
            }
            this.closePathSet.add(pathInfo);
            return false;
        }
    }


    /**
     * Attempts to login a user based on the JSON request
     * @param request       the http request
     * @param response      the http response
     */
    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Reader reader = null;
        LoginRequest loginRequest = null;
        try {
            reader = new InputStreamReader(request.getInputStream());
            final Gson gson = new GsonBuilder().create();
            loginRequest = gson.fromJson(reader, LoginRequest.class);
            final String username = loginRequest.username.toLowerCase();
            final char[] password = loginRequest.password != null ? loginRequest.password.toCharArray() : new char[0];
            this.authenticator.verify(username, password);
            if (!loginRequest.rememberMe) {
                onLoginSucceeded(username, request, response);
            } else {
                final String key = UUID.randomUUID().toString();
                final String remoteIp = request.getRemoteAddr();
                createAuthToken(key, username, remoteIp, response);
                onLoginSucceeded(username, request, response);
            }
        } catch (SecurityException ex) {
            final String username = loginRequest != null ? loginRequest.username : "null";
            log.error("Failed login for user " + username , ex);
            response.sendError(401, ex.getMessage());
        } catch (Exception ex) {
            final String username = loginRequest != null ? loginRequest.username : "null";
            log.error("Unexpected failure to login user  " + username, ex);
            response.sendError(401, "Invalid username and/or password");
        } finally {
            IO.close(reader);
        }
    }


    /**
     * Logs out the user associated with the request
     * @param request       the http request
     * @param response      the http response
     */
    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Option.of(request.getSession(false)).ifPresent(HttpSession::invalidate);
            final HttpAuthToken token = getAuthToken(request).orNull();
            if (token != null) {
                final Cookie cookie = cookie(token.getTokenKey(), "", 0, "/");
                response.addCookie(cookie);
            }
        } finally {
            final String baseUrl = HttpUtils.getBaseUrl(request);
            final String redirectUrl = baseUrl + "/account/login?next=" + URLEncoder.encode(baseUrl, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        }
    }


    /**
     * Returns true if the request carries a Cookie that includes a login token based on remember me
     * @param request       the http request
     * @param response      the http response
     * @return              true if remember me token found and is valid
     */
    private boolean isRememberMe(HttpServletRequest request, HttpServletResponse response) {
        try {
            final Option<HttpAuthToken> authToken = getAuthToken(request);
            if (authToken.isEmpty()) {
                return false;
            } else {
                final HttpAuthToken token = authToken.get();
                createAuthToken(token.getTokenKey(), token.getUsername(), token.getRemoteIP(), response);
                onLoginSucceeded(token.getUsername(), request, null);
                return true;
            }
        } catch (Exception ex) {
            log.error("Failed to process remember me cookies", ex);
            return false;
        }
    }


    /**
     * Returns the AuthToken bound to the specified request
     * @param request   the http request
     * @return          the option on AuthToken
     */
    private Option<HttpAuthToken> getAuthToken(HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                final String name = cookie.getName();
                final HttpAuthToken authToken = authTokenMap.get(name);
                if (authToken != null) {
                    final String value = cookie.getValue();
                    final String valueSha256 = value != null ? Crypto.sha256(value) : null;
                    if (authToken.getTokenValue().equals(valueSha256)) {
                        return Option.of(authToken);
                    }
                }
            }
        }
        return Option.empty();
    }


    /**
     * Called after a user has successfully logged in
     * @param username  the username of user
     * @param request   the http request
     */
    private void onLoginSucceeded(String username, HttpServletRequest request, HttpServletResponse response) throws IOException {
        final HttpSession session = request.getSession(true);
        session.setAttribute("username", username);
        session.setMaxInactiveInterval(0);
        User.setCurrentUser(username);
        if (response != null) {
            PrintWriter writer = null;
            try {
                final String nextUrl = request.getParameter("request");
                if (nextUrl != null) {
                    response.setContentType("text/plain");
                    response.setStatus(200);
                    writer = response.getWriter();
                    writer.print(nextUrl);
                } else {
                    final String path = HttpUtils.getRequestPath(request);
                    final String url = request.getRequestURL().toString();
                    final String next = url.replace(path, "");
                    response.setContentType("text/plain");
                    response.setStatus(200);
                    writer = response.getWriter();
                    writer.print(next);
                }
            } finally {
                IO.close(writer);
            }
        }
    }


    /**
     * Creates a new AuthToken with associated Cookie based on the args provided
     * @param tokenKey      the key for auth token
     * @param username      the username
     * @param remoteIp      the remote IP address
     * @param response      the http response
     */
    private void createAuthToken(String tokenKey, String username, String remoteIp, HttpServletResponse response) {
        try {
            final String value = new BigInteger(256, new Random()).toString();
            final String valueSha256 = Crypto.sha256(value);
            final HttpAuthToken authToken = new HttpAuthToken(username, tokenKey, valueSha256, remoteIp);
            response.addCookie(cookie(tokenKey, value, 0, "/"));
            this.authTokenMap.put(tokenKey, authToken);
            this.authTokenManager.add(authToken);
        } catch (Exception ex) {
            log.error("Failed to create auth token", ex);
        }
    }


    /**
     * Returns a newly created cookie with the args specified
     * @param name      the name for cookie
     * @param value     the value for cookie
     * @param maxAge    the max age in seconds
     * @param path      the path
     * @return          the cookie
     */
    private Cookie cookie(String name, String value, int maxAge, String path) {
        final Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        return cookie;
    }




    /**
     * A class to capture the username/password for a login request
     */
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    static class LoginRequest {
        private String username;
        private String password;
        private boolean rememberMe;
    }

}
