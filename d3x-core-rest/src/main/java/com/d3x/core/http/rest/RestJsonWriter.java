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
package com.d3x.core.http.rest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

/**
 * A MessageBodyWriter that uses a Google Gson instance to write objects
 *
 * @author Xavier Witdouck
 */
@Provider
public class RestJsonWriter implements MessageBodyWriter<Object> {

    private Gson gson;

    /**
     * Constructor
     * @param gson the GSON engine
     */
    public RestJsonWriter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return mediaType.equals(MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public void writeTo(
        Object object,
        Class<?> aClass,
        Type type,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, Object> multivaluedMap,
        OutputStream os) throws IOException, WebApplicationException {
        JsonWriter writer = null;
        try {
            writer = new JsonWriter(new OutputStreamWriter(new BufferedOutputStream(os)));
            writer.setIndent("  ");
            gson.toJson(object, type, writer);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }
}
