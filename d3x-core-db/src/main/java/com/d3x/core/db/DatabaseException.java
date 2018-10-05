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
package com.d3x.core.db;

/**
 * A RuntimeException generate by the Database adapter framework
 *
 * @author Xavier Witdouck
 */
public class DatabaseException extends RuntimeException {


    /**
     * Constructor
     * @param message   the exception message
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Constructor
     * @param message   the exception message
     * @param cause     the exception cause
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
