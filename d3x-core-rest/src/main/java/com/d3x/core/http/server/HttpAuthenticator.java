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

/**
 * Interface to a component that can validate whether login credentials are valid or not
 *
 * @author Xavier Witdouck
 */
@FunctionalInterface()
public interface HttpAuthenticator {

    /**
     * Verifies that the username and password credentials are valid
     * @param username  the username
     * @param password  the password
     * @throws SecurityException    if credentials are not valid
     */
    void verify(String username, char[] password) throws SecurityException;
}
