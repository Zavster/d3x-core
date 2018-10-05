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
import java.util.function.Consumer;

/**
 * A basic API abstraction for making HTTP calls to allow other libraries to be used with additional customization
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
@lombok.extern.slf4j.Slf4j
public abstract class HttpClient {

    private static HttpClient defaultClient = new HttpClientJdk();

    /**
     * Constructor
     */
    public HttpClient() {
        super();
    }

    /**
     * Returns a reference to the default client
     * @return  the default client reference
     */
    public static HttpClient getDefault() {
        return defaultClient;
    }

    /**
     * Sets the default http client to use
     * @param defaultClient     the default http client to use
     */
    public static void setDefault(HttpClient defaultClient) {
        HttpClient.defaultClient = defaultClient;
    }


    /**
     * Executes an HTTP GET request using the configurator to setup the request descriptor
     * @param url           the URL or GET operation
     * @param configurator  the HTTP request configurator
     * @param <T>           the type produced by the response handler bound to the request
     * @return              the optional result produced by the response handler
     */
    public abstract <T> T doGet(URL url, Consumer<HttpRequest<T>> configurator);


    /**
     * Executes an HTTP POST request using the configurator to setup the request descriptor
     * @param url           the URL or POST operation
     * @param configurator  the HTTP request configurator
     * @param <T>           the type produced by the response handler bound to the request
     * @return              the optional result produced by the response handler
     */
    public abstract <T> T doPost(URL url, Consumer<HttpPost<T>> configurator);


    /**
     * Executes an HTTP GET request using the configurator to setup the request descriptor
     * @param url           the URL or GET operation
     * @param configurator  the HTTP request configurator
     * @param <T>           the type produced by the response handler bound to the request
     * @return              the optional result produced by the response handler
     */
    public abstract <T> T doGet(String url, Consumer<HttpRequest<T>> configurator);


    /**
     * Executes an HTTP POST request using the configurator to setup the request descriptor
     * @param url           the URL or POST operation
     * @param configurator  the HTTP request configurator
     * @param <T>           the type produced by the response handler bound to the request
     * @return              the optional result produced by the response handler
     */
    public abstract <T> T doPost(String url, Consumer<HttpPost<T>> configurator);



    /**
     * A callback interface to handle the response to an HttpRequest
     */
    public interface ResponseHandler<T> {

        /**
         * Called after an http request has been invoked
         * @param response      the response object
         * @return              the optional result for this handler
         * @throws Exception    if the handler fails to process request
         */
        T onResponse(HttpResponse response) throws Exception;
    }

}
