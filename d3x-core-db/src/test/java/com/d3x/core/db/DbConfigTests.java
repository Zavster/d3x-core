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
import java.security.Key;

import com.d3x.core.json.Json;
import com.d3x.core.util.Crypto;
import com.d3x.core.util.Option;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for DbConfig class
 *
 * @author Xavier Witdouck
 */
public class DbConfigTests {



    @Test()
    public void serialization() throws Exception {
        Key key = Crypto.createKey("AES", 128, Option.empty());
        Crypto crypto = new Crypto("AES", key);
        GsonBuilder builder = Json.createGsonBuilder(Option.of(crypto));
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(DatabaseConfig.class, new DatabaseConfig.Serializer());
        builder.registerTypeAdapter(DatabaseConfig.class, new DatabaseConfig.Deserializer());
        Gson gson = builder.create();
        DatabaseConfig expected = DatabaseConfig.h2(new File("test-db"), "sa", "hello");
        String jsonString = gson.toJson(expected);
        DatabaseConfig result = gson.fromJson(jsonString, DatabaseConfig.class);
        Assert.assertEquals(result, expected);
    }
}
