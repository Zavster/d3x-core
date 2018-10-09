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

import java.io.File;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.d3x.core.json.Json;
import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import com.d3x.core.util.Password;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * A class that capture database configuration details
 *
 * @author Xavier Witdouck
 */
@lombok.Builder()
@lombok.ToString()
@lombok.EqualsAndHashCode()
@lombok.AllArgsConstructor()
@lombok.extern.slf4j.Slf4j
public class DatabaseConfig {

    /** The driver definition */
    @lombok.Getter @lombok.NonNull private DatabaseDriver driver;
    /** The JDBC connection url */
    @lombok.Getter @lombok.NonNull private String url;
    /** The JDBC user name*/
    @lombok.Getter @lombok.NonNull private Option<String> user;
    /** The JDBC password */
    @lombok.Getter @lombok.NonNull private Option<Password> password;
    /** The inital size of the connection pool */
    @lombok.Getter @lombok.NonNull private Option<Integer> initialPoolSize;
    /** The max size of connection pool */
    @lombok.Getter @lombok.NonNull private Option<Integer> maxPoolSize;
    /** The max number of idle connections in pool */
    @lombok.Getter @lombok.NonNull private Option<Integer> maxPoolIdleSize;
    /** True if connections should be set to read only */
    @lombok.Getter @lombok.NonNull private Option<Boolean> readOnly;
    /** True to mark connection as auto commit */
    @lombok.Getter @lombok.NonNull private Option<Boolean> autoCommit;
    /** The query time out in seconds */
    @lombok.Getter @lombok.NonNull private Option<Integer> queryTimeOutSeconds;
    /** The max time to wait for an available connection */
    @lombok.Getter @lombok.NonNull private Option<Integer> maxWaitTimeMillis;


    /**
     * Returns a new database config for an H2 database
     * @param dbFile        the database file
     * @param user          the username
     * @param password      the password
     * @return              the config
     */
    public static DatabaseConfig h2(File dbFile, String user, String password) {
        return DatabaseConfig.builder()
                .url("jdbc:h2:file:" + dbFile.getAbsolutePath())
                .driver(DatabaseDriver.H2)
                .initialPoolSize(Option.of(1))
                .maxPoolSize(Option.of(5))
                .maxPoolIdleSize(Option.of(5))
                .readOnly(Option.of(false))
                .autoCommit(Option.of(true))
                .queryTimeOutSeconds(Option.of(60))
                .maxWaitTimeMillis(Option.of(5000))
                .user(Option.of(user))
                .password(Option.of(password).map(Password::of))
                .build();
    }


    /**
     * Attempts to connect to the database given this config to assess if it is valid
     * @return  this configuration object
     * @throws DatabaseException    if fails to make a connection
     */
    public DatabaseConfig verify() {
        Connection conn = null;
        final String className = driver.getDriverClassName();
        try {
            log.info("Connecting to " + url);
            Class.forName(className);
            final String pass = password.map(p -> new String(p.getValue())).orNull();
            conn = DriverManager.getConnection(url, user.orNull(), pass);
            log.info("Connection successful to " + url);
            return this;
        } catch (ClassNotFoundException ex) {
            throw new DatabaseException("Failed to load JDBC driver class " + className, ex);
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to connect to database with " + url, ex);
        } finally {
            IO.close(conn);
        }
    }



    /**
     * A JSON serializer for DatabaseConfig
     */
    public static class Serializer implements JsonSerializer<DatabaseConfig> {
        @Override
        public JsonElement serialize(DatabaseConfig value, Type type, JsonSerializationContext context) {
            if (value == null) {
                return JsonNull.INSTANCE;
            } else {
                return Json.object(db -> {
                    db.addProperty("driver", value.getDriver().getDriverClassName());
                    db.addProperty("url", value.getUrl());
                    db.addProperty("user", value.getUser().orNull());
                    db.add("password", context.serialize(value.getPassword().orNull(), Password.class));
                    db.addProperty("initialPoolSize", value.initialPoolSize.orNull());
                    db.addProperty("maxPoolSize", value.maxPoolSize.orNull());
                    db.addProperty("maxPoolIdleSize", value.maxPoolIdleSize.orNull());
                    db.addProperty("readOnly", value.readOnly.orNull());
                    db.addProperty("autoCommit", value.autoCommit.orNull());
                    db.addProperty("queryTimeOutSeconds", value.queryTimeOutSeconds.orNull());
                    db.addProperty("maxWaitTimeMills", value.maxWaitTimeMillis.orNull());
                });
            }
        }
    }


    /**
     * A JSON deserializer for DatabaseConfig
     */
    public static class Deserializer implements JsonDeserializer<DatabaseConfig> {
        @Override
        public DatabaseConfig deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json == JsonNull.INSTANCE) {
                return null;
            } else {
                final JsonObject db = (JsonObject)json;
                return DatabaseConfig.builder()
                    .driver(DatabaseDriver.of(Json.getStringOrFail(db, "driver")))
                    .url(Json.getStringOrFail(db, "url"))
                    .user(Json.getString(db, "user"))
                    .password(Json.getElement(db, "password").map(e -> context.deserialize(e, Password.class)))
                    .initialPoolSize(Json.getInt(db, "initialPoolSize"))
                    .maxPoolSize(Json.getInt(db, "maxPoolSize"))
                    .maxPoolIdleSize(Json.getInt(db, "maxPoolIdleSize"))
                    .readOnly(Json.getBoolean(db, "readOnly"))
                    .autoCommit(Json.getBoolean(db, "autoCommit"))
                    .queryTimeOutSeconds(Json.getInt(db, "queryTimeOutSeconds"))
                    .maxWaitTimeMillis(Json.getInt(db, "maxWaitTimeMills"))
                    .build();
            }
        }
    }

}
