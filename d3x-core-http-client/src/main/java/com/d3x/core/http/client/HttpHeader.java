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

/**
 * A class that captures a http header key value pair
 *
 * @author Xavier Witdouck
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 */
@lombok.ToString()
@lombok.EqualsAndHashCode()
@lombok.AllArgsConstructor()
public class HttpHeader implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** The header key */
    @lombok.NonNull @lombok.Getter private String key;
    /** The header value */
    @lombok.NonNull @lombok.Getter private String value;
}
