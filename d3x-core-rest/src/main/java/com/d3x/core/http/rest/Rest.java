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

import java.io.OutputStream;
import java.util.function.Consumer;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

public class Rest {


    /**
     * Convenience function to generate a JAX-RS response using a Consumer / Builder
     * @param status        the status code for response
     * @param configure     the consumer to configure response
     * @return              the resulting response
     */
    public static Response respond(int status, Consumer<Response.ResponseBuilder> configure) {
        final Response.ResponseBuilder builder = Response.status(status);
        configure.accept(builder);
        return builder.build();
    }


    /**
     * Convenience function to generate a JAX-RS response using a Consumer / Builder
     * @param status        the status code for response
     * @param mediaType          the media type for response
     * @param configure     the consumer to configure response
     * @return              the resulting response
     */
    public static Response respond(int status, MediaType mediaType, Consumer<Response.ResponseBuilder> configure) {
        final Response.ResponseBuilder builder = Response.status(status);
        builder.type(mediaType);
        configure.accept(builder);
        return builder.build();
    }


    /**
     * Returns a 404 response with the message specified
     * @param message   the message for response
     * @return          the response
     */
    public static Response notFound(String message) {
        return respond(404, response -> {
            response.type(MediaType.TEXT_PLAIN_TYPE);
            response.entity(message);
        });
    }


    /**
     * Returns a 400 response with the message specified
     * @param message   the message for response
     * @return          the response
     */
    public static Response badRequest(String message) {
        return respond(400, response -> {
            response.type(MediaType.TEXT_PLAIN_TYPE);
            response.entity(message);
        });
    }


    /**
     * Returns a 403 response with the message specified
     * @param message   the message for response
     * @return          the response
     */
    public static Response forbidden(String message) {
        return respond(403, response -> {
            response.type(MediaType.TEXT_PLAIN_TYPE);
            response.entity(message);
        });
    }


    public static StreamingOutput stream(Consumer<OutputStream> outputHandler) {
        return outputHandler::accept;
    }

}
