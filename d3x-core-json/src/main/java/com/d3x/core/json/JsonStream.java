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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.d3x.core.util.IO;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A class used to wrap objects as a message (header + body) so as to include meta-data about the contents
 *
 * @author Xavier Witdouck
 */
public class JsonStream {

    private static final Map<String,Type> schemaDataTypeMap = new HashMap<>();
    private static final Map<Type,String> schemaTypeNameMap = new HashMap<>();


    /**
     * Registers the schema name for a data type to be wrapped in a JsonMessage
     * @param dataType      the data type
     * @param schemaType    the schema name for type
     */
    public static <T> void register(Type dataType, String schemaType) {
        Objects.requireNonNull(dataType, "The type cannot be null");
        Objects.requireNonNull(schemaType, "The schema name cannot be null");
        schemaDataTypeMap.put(schemaType, dataType);
        schemaTypeNameMap.put(dataType, schemaType);
    }


    /**
     * Returns the read adapter to read objects from Json encoded as JsonMessages
     * @param gson  the GSON for parsing
     * @return      the read adapter
     */
    public static <T> Reader<T> reader(Gson gson) {
        return new Reader<>(gson);
    }


    /**
     * Returns the write adapter to write objects to Json encoded as JsonMessages
     * @param gson  the GSON for writing
     * @param os    the output stream to write to
     * @return      the write adapter
     */
    public static <T> Writer<T> writer(Gson gson, OutputStream os) {
        return new Writer<>(gson, os);
    }


    /**
     * Returns the write adapter to write objects to Json encoded as JsonMessages
     * @param os    the output stream to write to
     * @return      the write adapter
     */
    public static <T extends JsonWritable> Writer<T> writer(OutputStream os) {
        return new Writer<>(null, os);
    }



    /**
     * The read adapter for JsonMessage objects
     */
    public static class Reader<T> {

        private Gson gson;

        /**
         * Constructor
         * @param gson  the GSON adapter for parsing
         */
        Reader(Gson gson) {
            this.gson = gson;
        }

        @SuppressWarnings("unchecked")
        public List<T> list(InputStream is) throws IOException {
            return list(new InputStreamReader(new BufferedInputStream(is)));
        }


        @SuppressWarnings("unchecked")
        public List<T> list(java.io.Reader reader) throws IOException {
            final List<T> results = new ArrayList<>();
            final JsonReader jsonReader = new JsonReader(new BufferedReader(reader));
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                final JsonObject next = gson.fromJson(jsonReader, JsonObject.class);
                final JsonObject header = Json.getObjectOrFail(next, "header");
                final String schemaType = Json.getStringOrFail(header, "schemaType");
                final Type dataType = schemaDataTypeMap.get(schemaType);
                if (dataType == null) {
                    throw new RuntimeException("No schema type registered for name: " + schemaType);
                } else {
                    try {
                        final TypeAdapter adapter = gson.getAdapter((Class<?>)dataType);
                        if (adapter instanceof ReflectiveTypeAdapterFactory.Adapter) {
                            final JsonElement body = Json.getElementOrFail(next, "body");
                            final T result = gson.fromJson(body, dataType);
                            results.add(result);
                        } else {
                            final T result = gson.fromJson(next, dataType);
                            results.add(result);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to parse of message: " + gson.toJson(next), ex);
                    }
                }
            }
            jsonReader.endArray();
            jsonReader.close();
            return results;
        }
    }



    /**
     * The write adapter for JsonMessages
     */
    public static class Writer<T> {

        private Gson gson;
        private OutputStream os;

        /**
         * Constructor
         * @param gson  the GSON adapter for writing
         */
        Writer(Gson gson, OutputStream os) {
            this.gson = gson;
            this.os = os;
        }

        @SuppressWarnings("unchecked")
        public void write(Iterator<T> values) throws IOException {
            JsonWriter writer = null;
            try {
                writer = new JsonWriter(new OutputStreamWriter(new BufferedOutputStream(os)));
                writer.setIndent("  ");
                writer.beginArray();
                while (values.hasNext()) {
                    final T value = values.next();
                    final Class<?> type = value.getClass();
                    final String schemaType = schemaTypeNameMap.get(type);
                    if (schemaType == null) {
                        throw new RuntimeException("No schema type registered for " + type);
                    } else {
                        writer.beginObject();
                        writer.name("header");
                        writer.beginObject();
                        writer.name("schemaType").value(schemaType);
                        writer.name("schemaVersion").value(1);
                        writer.endObject();
                        writer.name("body");
                        if (value instanceof JsonWritable) {
                            ((JsonWritable)value).write(writer);
                            writer.endObject();
                        } else {
                            gson.toJson(value, type, writer);
                            writer.endObject();
                        }
                    }
                }
                writer.endArray();
            } finally {
                IO.close(writer);
            }
        }
    }



    /**
     * The header for a Json message
     */
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Header {
        /** The schema type name */
        @lombok.Getter @lombok.NonNull private String schemaType;
        /** The schema version */
        @lombok.Getter private int schemaVersion;
    }

}
