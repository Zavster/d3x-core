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
package com.d3x.core.json;

import java.lang.reflect.Type;

import com.d3x.core.util.Option;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * An component that implements Json serialization using the Google GSON library
 * @param <T>   the data type that can be serialization
 */
public interface JsonSerialization<T> extends JsonSerializer<T>, JsonDeserializer<T> {


    /**
     * Returns a wrapper of this serialization with the schema and version
     * @param name      the schema name
     * @param version   the schema version
     * @return          the wrapper serialization
     */
    default JsonSerialization<T> withSchema(String name, int version) {
        return new WithSchema<>(name, version, this);
    }


    @Override
    default T deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        throw new UnsupportedOperationException("Deserialization of type " + type + " not supported");
    }


    @Override
    default JsonElement serialize(T t, Type type, JsonSerializationContext jsonSerializationContext) {
        throw new UnsupportedOperationException("Serialization of type " + type + " not supported");
    }


    /**
     * A JsonSerialization implementation that wraps a delegate and includes schema information
     */
    @lombok.AllArgsConstructor()
    class WithSchema<T> implements JsonSerialization<T> {

        /** The schema name for this serialization */
        @lombok.NonNull @lombok.Getter private String schemaType;
        /** The schema version for this serialization */
        @lombok.Getter private int schemaVersion;
        /** The delegate for this wrapper */
        @lombok.NonNull @lombok.Getter private JsonSerialization<T> delegate;


        @Override
        public JsonElement serialize(T value, Type type, JsonSerializationContext context) {
            final JsonObject header = new JsonObject();
            header.addProperty("schemaType", schemaType);
            header.addProperty("schemaVersion", schemaVersion);
            return Json.object(message -> {
                message.add("header", header);
                message.add("body", delegate.serialize(value, type, context));
            });
        }

        @Override
        public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.equals(JsonNull.INSTANCE)) {
                return null;
            } else if (!(json instanceof JsonObject)) {
                return delegate.deserialize(json, type, context);
            } else {
                final JsonObject root = json.getAsJsonObject();
                final Option<JsonObject> header = Json.getObject(root, "header");
                final String schemaType = header.map(v -> Json.getString(v, "schemaType").orNull()).orNull();
                final Integer schemaVersion = header.map(v -> Json.getInt(v, "schemaVersion").orNull()).orNull();
                if (schemaType == null || schemaVersion == null) {
                    return delegate.deserialize(json, type, context);
                } else {
                    final Option<JsonElement> body = Json.getElement(root, "body");
                    final JsonElement target = body.orElse(json);
                    return delegate.deserialize(target, type, context);
                }
            }
        }
    }

}
