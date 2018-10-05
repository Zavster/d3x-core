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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.d3x.core.util.IO;
import com.google.gson.stream.JsonWriter;

/**
 * An interface to an object that can be written out to Json
 *
 * @author Xavier Witdouck
 */
public interface JsonWritable {

    /**
     * Writes this object to JSON via the writer provided
     * @param writer    the writer
     * @throws IOException  if there is an I/O error
     */
    void write(JsonWriter writer) throws IOException;



    /**
     * Returns a serializer function that will write the stream of objects to an OutputStream provided
     * @param stream    the stream of objects to serialize
     * @return          the serializer function for stream
     */
    static <T extends JsonWritable> Consumer<OutputStream> serializer(Stream<T> stream) {
        return os -> {
            JsonWriter writer = null;
            try {
                final Iterator<T> iterator = stream.iterator();
                writer = new JsonWriter(new OutputStreamWriter(new BufferedOutputStream(os)));
                writer.beginArray();
                while (iterator.hasNext()) {
                    final T value = iterator.next();
                    value.write(writer);
                }
                writer.endArray();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to serialize objects to json", ex);
            } finally {
                IO.close(writer);
            }
        };
    }
}
