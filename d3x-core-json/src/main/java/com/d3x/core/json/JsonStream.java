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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A class used to wrap objects as a message (header + body) so as to include meta-data about the contents
 *
 * @author Xavier Witdouck
 */
public class JsonStream {

    private static final Map<String,Type> schemaTypeMap = new HashMap<>();
    private static final Map<Type,String> schemaNameMap = new HashMap<>();


    /**
     * Registers the schema name for a data type to be wrapped in a JsonMessage
     * @param type          the data type
     * @param schemaName    the schema name for type
     */
    public static <T> void register(Type type, String schemaName) {
        Objects.requireNonNull(type, "The type cannot be null");
        Objects.requireNonNull(schemaName, "The schema name cannot be null");
        schemaTypeMap.put(schemaName, type);
        schemaNameMap.put(type, schemaName);
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
            final List<T> messages = new ArrayList<>();
            final JsonReader reader = new JsonReader(new InputStreamReader(new BufferedInputStream(is), "UTF-8"));
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                if (!reader.nextName().equalsIgnoreCase("header")) {
                    throw new RuntimeException("Malformed message, expected header");
                }
                final Header header = gson.fromJson(reader, Header.class);
                final String schemaName = header.getSchemaName();
                final Type schemaType = schemaTypeMap.get(schemaName);
                if (schemaType == null) {
                    throw new RuntimeException("No schema type registered for name: " + schemaName);
                } else {
                    final String nextName = reader.nextName();
                    if (!nextName.equalsIgnoreCase("body")) {
                        throw new RuntimeException("Malformed message, expected body");
                    } else {
                        try {
                            final T body = gson.fromJson(reader, schemaType);
                            messages.add(body);

                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to parse body of message for type: " + schemaType, ex);
                        }
                    }
                }
                reader.endObject();
            }
            reader.endArray();
            reader.close();
            return messages;
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
                    final String schemaName = schemaNameMap.get(type);
                    if (schemaName == null) {
                        throw new RuntimeException("No schema type registered for " + type);
                    } else {
                        writer.beginObject();
                        writer.name("header");
                        writer.beginObject();
                        writer.name("schemaName").value(schemaName);
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
        /** The schema name */
        @lombok.Getter @lombok.NonNull private String schemaName;
        /** The schema version */
        @lombok.Getter private int schemaVersion;
    }

}
