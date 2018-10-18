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

import java.sql.Timestamp;
import java.util.stream.Stream;

import com.d3x.core.db.DatabaseMapping;
import com.d3x.core.db.DatabaseSql;

/**
 * A class that captures the details of an AuthToken for "remember me" functionality
 *
 * @author Xavier Witdouck
 */
@lombok.AllArgsConstructor
public class HttpAuthToken {

    @lombok.Getter  private String username;
    @lombok.Getter private String tokenKey;
    @lombok.Getter private String tokenValue;
    @lombok.Getter private String remoteIP;


    /**
     * An interface to a component that manages auth tokens
     */
    public interface Manager {

        /**
         * Adds an auth token to be managed
         * @param token the auth token
         */
        void add(HttpAuthToken token);

        /**
         * Returns the stream of all current auth tokens
         * @return      the stream of all auth tokens
         */
        Stream<HttpAuthToken> getTokens();

    }


    /**
     * The DatabaseMapping for the AuthToken class
     */
    public static class Mapping implements DatabaseMapping<HttpAuthToken> {

        @Override
        public Class<HttpAuthToken> type() {
            return HttpAuthToken.class;
        }

        @Override
        @DatabaseSql("SELECT * FROM AuthToken")
        public Mapper<HttpAuthToken> select() {
            return (rs, calendar) -> new HttpAuthToken(
                rs.getString("username"),
                rs.getString("tokenKey"),
                rs.getString("tokenValue"),
                rs.getString("remoteIP")
            );
        }

        @Override
        @DatabaseSql("INSERT INTO AuthToken (username, tokenKey, tokenValue, remoteIp, createUtc")
        public Binder<HttpAuthToken> insert() {
            return (record, stmt, calendar) -> {
                stmt.setString(1, record.username);
                stmt.setString(2, record.tokenKey);
                stmt.setString(3, record.tokenValue);
                stmt.setString(4, record.remoteIP);
                stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()), calendar);
            };
        }
    }

}

