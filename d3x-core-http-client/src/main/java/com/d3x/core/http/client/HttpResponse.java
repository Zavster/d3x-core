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

import java.io.Closeable;
import java.io.InputStream;
import java.util.List;

import com.d3x.core.util.Option;

/**
 * An interface to a response associated with an HttpRequest
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
public interface HttpResponse extends Closeable {

    /**
     * Returns the status for this response
     * @return  the response status
     */
    HttpStatus getStatus();

    /**
     * Returns the stream to read the response content
     * @return      the stream to read response content
     */
    InputStream getStream();

    /**
     * Returns the headers in the response
     * @return      the headers in the response
     */
    List<HttpHeader> getHeaders();

    /**
     * Returns the header values for the key specified
     * @param key   the header key
     * @return      the headers for key
     */
    List<HttpHeader> getHeaders(String key);

    /**
     * Returns the first header for the key specified
     * @param key   the header key
     * @return      the first header
     */
    Option<HttpHeader> getHeader(String key);

}
