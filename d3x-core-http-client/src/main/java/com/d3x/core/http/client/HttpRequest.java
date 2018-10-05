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

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.d3x.core.util.Option;

/**
 * A request descriptor used to make http requests of various kinds via the Morpheus http client api.
 *
 * @param <T>   the type produced by this request
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public class HttpRequest<T> implements java.io.Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private Map<String,String> headers;
    private Map<String,String> cookies;

    /** The URL for this request */
    @lombok.Getter @lombok.Setter private URL url;
    /** The http method for this request */
    @lombok.Getter private HttpMethod method;
    /** The retry count in case of an network exception */
    @lombok.Getter @lombok.Setter private int retryCount;
    /** The read time out in milliseconds */
    @lombok.Getter @lombok.Setter private int readTimeout;
    /** The connect timeout in seconds */
    @lombok.Getter @lombok.Setter private int connectTimeout;
    /** True if request is set to verbose logging */
    @lombok.Getter @lombok.Setter private boolean verbose;
    /** The response handler for this request */
    @lombok.Setter private HttpClient.ResponseHandler<T> responseHandler;


    /**
     * Constructor
     * @param method    the HTTP request method
     * @param url       the url for request
     */
    HttpRequest(HttpMethod method, URL url) {
        this.method = method;
        this.headers = new HashMap<>();
        this.cookies = new HashMap<>();
        this.verbose = true;
        this.url = url;
    }

    /**
     * Applies all state from the argument to this request
     * @param request   the request reference
     * @return          this request
     */
    HttpRequest<T> apply(HttpRequest<T> request) {
        this.url = request.url;
        this.method = request.method;
        this.retryCount = request.retryCount;
        this.readTimeout = request.readTimeout;
        this.connectTimeout = request.connectTimeout;
        this.headers.putAll(request.headers);
        this.cookies.putAll(request.cookies);
        this.responseHandler = request.responseHandler;
        return this;
    }

    /**
     * Sets the value of the Accept Http header
     * @param accept    the Accept value
     */
    public void setAccept(String accept) {
        if (accept == null) {
            this.headers.remove("Accept");
        } else {
            this.headers.put("Accept", accept);
        }
    }

    /**
     * Sets the value of the Content-Type http header
     * @param contentType   the content type
     */
    public void setContentType(String contentType) {
        if (contentType == null) {
            this.headers.remove("Content-Type");
        } else {
            this.headers.put("Content-Type", contentType);
        }
    }

    /**
     * Sets the value of the Content-Length http header value
     * @param contentLength the content length
     */
    public void setContentLength(Integer contentLength) {
        if (contentLength == null) {
            this.headers.remove("Content-Length");
        } else {
            this.headers.put("Content-Length", String.valueOf(contentLength));
        }
    }

    /**
     * Returns the response handler for this request
     * @return      the response handler for this request
     */
    public Option<HttpClient.ResponseHandler<T>> getResponseHandler() {
        return Option.of(responseHandler);
    }

    /**
     * Returns the request header parameters for this request
     * @return  the map of headers
     */
    public Map<String,String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns an map of cookies for this request
     * @return  the map of cookies
     */
    public Map<String,String> getCookies() {
        return Collections.unmodifiableMap(cookies);
    }

    /**
     * Returns the optional content for this request
     * @return      the optional content for request
     */
    public Option<byte[]> getContent() {
        return Option.empty();
    }

    /**
     * Adds a request header to this request
     * @param key       the header key
     * @param value     the header value
     */
    public void addHeader(String key, String value) {
        Objects.requireNonNull(key, "The header key cannot be null");
        Objects.requireNonNull(value, "The header value cannot be null");
        this.headers.put(key, value);
    }

    /**
     * Creates a copy of this request replacing the URL
     * @param url   the URL to replace
     * @return      the copy of this request
     */
    @SuppressWarnings("unchecked")
    public HttpRequest<T> copy(String url) {
        try {
            final HttpRequest<T> clone = (HttpRequest<T>)super.clone();
            clone.url = new URL(url);
            return clone;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to clone HttpRequest", ex);
        }
    }
}
