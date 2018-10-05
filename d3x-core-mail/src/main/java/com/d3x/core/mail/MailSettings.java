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
package com.d3x.core.mail;

import java.lang.reflect.Type;

import com.d3x.core.json.Json;
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
 * A class to capture the details of a SMTP mail server and outbound message properties.
 *
 * @author Xavier Witdouck
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MailSettings {

    /** The from name for mail message */
    @lombok.NonNull @lombok.Getter private String fromName;
    /** The from address for mail message */
    @lombok.NonNull @lombok.Getter private String fromAddress;
    /** Returns the smtp server hostname */
    @lombok.NonNull @lombok.Getter private String host;
    /** Returns the smtp server port */
    @lombok.Getter private int port;
    /** Returns the smtp server user name */
    @lombok.NonNull @lombok.Getter private String username;
    /** Returns the smtp server password */
    @lombok.NonNull @lombok.Getter private Password password;
    /** Returns true if this server is enabled */
    @lombok.Getter private boolean enabled;



    public static class Serializer implements JsonSerializer<MailSettings> {
        @Override
        public JsonElement serialize(MailSettings value, Type type, JsonSerializationContext context) {
            if (value == null) return JsonNull.INSTANCE;
            return Json.object(object -> {
                object.addProperty("fromName", value.fromName);
                object.addProperty("fromAddress", value.fromAddress);
                object.addProperty("host", value.host);
                object.addProperty("port", value.port);
                object.addProperty("username", value.username);
                object.add("password", context.serialize(value.password, Password.class));
                object.addProperty("enabled", value.isEnabled());
            });
        }
    }


    public static class Deserializer implements JsonDeserializer<MailSettings> {
        @Override
        public MailSettings deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || JsonNull.INSTANCE.equals(json)) return null;
            final JsonObject object = (JsonObject)json;
            final MailSettings settings = new MailSettings();
            settings.fromName = Json.getStringOrFail(object, "fromName");
            settings.fromAddress = Json.getStringOrFail(object, "fromAddress");
            settings.host = Json.getStringOrFail(object, "host");
            settings.port = Json.getIntOrFail(object, "port");
            settings.username = Json.getString(object, "username").orNull();
            settings.password = context.deserialize(Json.getElement(object, "password").orNull(), Password.class);
            settings.enabled = Json.getBooleanOrFail(object, "enabled");
            return settings;
        }
    }

}
