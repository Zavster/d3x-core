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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/**
 * A MessageBodyReader that uses a Google Gson instance to write objects
 *
 * @author Xavier Witdouck
 */
@Provider
public class RestJsonReader implements MessageBodyReader<Object> {

    private Gson gson;
    private Set<MediaType> mediaTypeSet = Set.of(
        MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE.withCharset(StandardCharsets.UTF_8.name())
    );


    /**
     * Constructor
     * @param gson   the GSON adapter
     */
    public RestJsonReader(Gson gson) {
        this.gson = gson;
    }


    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return mediaTypeSet.contains(mediaType);
    }

    @Override
    public Object readFrom(
        Class<Object> aClass,
        Type type,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, String> multivaluedMap,
        InputStream is) throws IOException, WebApplicationException {
        JsonReader reader = null;
        try {
            reader = new JsonReader(new BufferedReader(new InputStreamReader(new BufferedInputStream(is))));
            return gson.fromJson(reader, type);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
