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

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;

/**
 * The default implementation of the HttpClient that uses the JDK http api
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
class HttpClientJdk extends HttpClient {


    @Override
    public <T> T doGet(String url, Consumer<HttpRequest<T>> configurator) {
        try {
            return doGet(new URL(url), configurator);
        } catch (MalformedURLException ex) {
            throw new HttpException("Malformed URL: " + url, ex);
        }
    }


    @Override
    public <T> T doPost(String url, Consumer<HttpPost<T>> configurator) {
        try {
            return doPost(new URL(url), configurator);
        } catch (MalformedURLException ex) {
            throw new HttpException("Malformed URL: " + url, ex);
        }
    }


    @Override
    public <T> T doGet(URL url, Consumer<HttpRequest<T>> configurator) {
        final HttpRequest<T> request = new HttpRequest<>(HttpMethod.GET, url);
        if (configurator != null) configurator.accept(request);
        return execute(request);
    }


    @Override
    public <T> T doPost(URL url, Consumer<HttpPost<T>> configurator) {
        final HttpPost<T> request = new HttpPost<>(url);
        if (configurator != null) configurator.accept(request);
        return execute(request);
    }

    /**
     * Executes the Http request, returning the result produced by the response handler
     * @param request   the request descriptor
     * @param <T>       the type of response object
     * @return          the result produced by response handler
     */
    private <T> T execute(HttpRequest<T> request) {
        final URL url = request.getUrl();
        final int retryCount = request.getRetryCount();
        for (int i = 0; i <= retryCount; ++i) {
            try {
                final long t1 = System.currentTimeMillis();
                final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod(request.getMethod().name());
                conn.setConnectTimeout(request.getConnectTimeout());
                conn.setReadTimeout(request.getReadTimeout());
                conn.setDoOutput(request.getContent().isPresent());
                request.getHeaders().forEach(conn::setRequestProperty);
                request.getCookies().forEach((key, value) -> conn.addRequestProperty("Cookie", String.format("%s=%s", key, value)));
                request.getContent().ifPresent(bytes -> write(bytes, conn));
                final int statusCode = conn.getResponseCode();
                if (hasMoved(statusCode)) {
                    final String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null) return execute(request.copy(newUrl));
                    final String message = "Received re-direct response but no Location in header";
                    throw new HttpException(message, null, request);
                }
                final String message = conn.getResponseMessage();
                final HttpStatus status = new HttpStatus(statusCode, message);
                final HttpResponse response = new DefaultResponse(status, conn);
                return request.getResponseHandler().map(handler -> {
                    try {
                        final T value = handler.onResponse(response);
                        final long t2 = System.currentTimeMillis();
                        if (request.isVerbose()) log.info("Completed HTTP request in " + (t2 - t1) + " millis for " + url);
                        return value;
                    } catch (Exception ex) {
                        throw new HttpException("Failed to handle Http response for " + url, ex);
                    } finally {
                        IO.close(response);
                    }
                }).orNull();
            } catch (Exception ex) {
                if (!isRetryAllowed(ex) || i == retryCount) {
                    throw new HttpException(ex.getMessage(), ex, request);
                } else {
                    log.info("Retrying http request: " + url);
                }
            }
        }
        return null;
    }


    /**
     * Returns true if we can retry the request for the exception generated
     * @param ex    the exception associated with failed request
     * @return      true if retry allowed
     */
    private boolean isRetryAllowed(Exception ex) {
        return ex instanceof ConnectException ||
                ex instanceof SocketException ||
                ex instanceof SocketTimeoutException ||
                ex instanceof EOFException;
    }


    /**
     * Writes bytes to the output stream for the connection provided
     * @param bytes the bytes to write
     * @param conn  the connection to write bytes to
     */
    private void write(byte[] bytes, HttpURLConnection conn) {
        try (OutputStream os = new BufferedOutputStream(conn.getOutputStream())) {
            os.write(bytes);
            os.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write byes to URL: " + conn.getURL(), ex);
        }
    }


    /**
     * Returns true if the status code implies the resource has moved
     * @param statusCode    the HTTP status code
     * @return              true if resource has moved
     */
    private boolean hasMoved(int statusCode) {
        switch (statusCode) {
            case HttpURLConnection.HTTP_MOVED_TEMP: return true;
            case HttpURLConnection.HTTP_MOVED_PERM: return true;
            case HttpURLConnection.HTTP_SEE_OTHER:  return true;
            default:                                return false;
        }
    }


    /**
     * The HttpResponse object for the default HttpClient.
     */
    private static class DefaultResponse implements HttpResponse, Closeable {

        private HttpStatus status;
        private InputStream stream;
        private HttpURLConnection conn;
        private List<HttpHeader> headers;
        private Map<String,List<HttpHeader>> headerMap;

        /**
         * Constructor
         * @param conn  the http connection object
         */
        DefaultResponse(HttpStatus status, HttpURLConnection conn) {
            this.conn = conn;
            this.status = status;
            this.headers = new ArrayList<>();
            this.headerMap = new HashMap<>();
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null && values != null && !values.isEmpty()) {
                    final List<HttpHeader> headerList = new ArrayList<>();
                    this.headerMap.put(key, headerList);
                    values.forEach(value -> {
                        final HttpHeader header = new HttpHeader(key, value);
                        this.headers.add(header);
                        headerList.add(header);
                    });
                }
            });
        }

        /**
         * Returns the content encoding for response
         * @return  the content encoding
         */
        private Optional<String> getContentEncoding() {
            for (HttpHeader header : headers) {
                if (header.getKey().equals("Content-Encoding")) {
                    return Optional.ofNullable(header.getValue());
                }
            }
            return Optional.empty();
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }

        @Override
        public synchronized InputStream getStream() {
            if (stream != null) {
                return stream;
            } else {
                try {
                    final String encoding = getContentEncoding().orElse("default").toLowerCase();
                    if (encoding.equalsIgnoreCase("gzip")) {
                        this.stream = new GZIPInputStream(conn.getInputStream());
                        return stream;
                    } else {
                        this.stream = conn.getInputStream();
                        return stream;
                    }
                } catch (Exception ex) {
                    throw new HttpException("Failed to read from input stream", ex);
                }
            }
        }

        @Override
        public List<HttpHeader> getHeaders() {
            return headers;
        }

        @Override
        public List<HttpHeader> getHeaders(String key) {
            final List<HttpHeader> headers = headerMap.get(key);
            return headers != null ? Collections.unmodifiableList(headers) : Collections.emptyList();
        }

        @Override
        public Option<HttpHeader> getHeader(String key) {
            final List<HttpHeader> headers = headerMap.get(key);
            return headers != null && !headers.isEmpty() ? Option.of(headers.get(0)) : Option.empty();
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
