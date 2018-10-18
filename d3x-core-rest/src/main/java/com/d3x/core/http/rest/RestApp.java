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

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.Currency;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.d3x.core.json.JsonWritable;
import com.d3x.core.util.IO;
import com.d3x.core.util.Modules;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

/**
 * A generic JAX-RS application class that exposes singleton and resources via the Modules class.
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class RestApp extends Application {

    private Modules modules;

    /**
     * Constructor
     * @param modules   the application modules
     */
    public RestApp(@Context Modules modules) {
        this.modules = modules;
        this.modules.register(RestJsonWriter.class, () -> new RestJsonWriter(modules.getOrFail(Gson.class)));
        this.modules.register(RestJsonReader.class, () -> new RestJsonReader(modules.getOrFail(Gson.class)));
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            DefaultExceptionMapper.class,
            RestParameterConverterProvider.class
        );
    }

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<>(modules.list(module -> {
            final Class<?> resourceType = module.getClass();
            final Annotation[] paths = resourceType.getAnnotationsByType(Path.class);
            final Annotation[] providers = resourceType.getAnnotationsByType(Provider.class);
            if (paths.length > 0 || providers.length > 0) {
                return true;
            } else if (resourceType.isAssignableFrom(MessageBodyWriter.class)) {
                return true;
            } else if (resourceType.isAssignableFrom(MessageBodyReader.class)) {
                return true;
            } else {
                return false;
            }
        }));
    }


    /**
     * Returns a newly created Response wrapping the consumer in a JAX-RS StreamingOutput
     * @param mediaType     the media type
     * @param outputHandler the output handler
     * @return              the Response object
     */
    public static Response stream(MediaType mediaType, Consumer<OutputStream> outputHandler) {
        final StreamingOutput output = outputHandler::accept;
        return Response.ok(output, mediaType).build();
    }


    /**
     * Returns a newly created Response wrapping the consumer in a JAX-RS StreamingOutput
     * @param outputHandler the output handler
     * @return              the Response object
     */
    public static Response streamJson(Consumer<OutputStream> outputHandler) {
        final StreamingOutput output = outputHandler::accept;
        return Response.ok(output, MediaType.APPLICATION_JSON_TYPE).build();
    }


    /**
     * Returns a newly created Response wrapping the consumer in a JAX-RS StreamingOutput
     * @param outputHandler the output handler
     * @return              the Response object
     */
    public static Response streamHtml(Consumer<OutputStream> outputHandler) {
        final StreamingOutput output = outputHandler::accept;
        return Response.ok(output, MediaType.TEXT_HTML_TYPE).build();
    }


    /**
     * Returns a newly created Response wrapping the consumer in a JAX-RS StreamingOutput
     * @param outputHandler the output handler
     * @return              the Response object
     */
    public static Response streamPlainText(Consumer<OutputStream> outputHandler) {
        final StreamingOutput output = outputHandler::accept;
        return Response.ok(output, MediaType.TEXT_PLAIN_TYPE).build();
    }



    public static StreamingOutput stream(Consumer<OutputStream> outputHandler) {
        return outputHandler::accept;
    }


    /**
     * Returns a response that streams an iterator of JSON serializable objects
     * @param iterator  the iterator of objects
     * @return          the streaming Response
     */
    public static Response streamJsonArray(Iterator<? extends JsonWritable> iterator) {
        return stream(MediaType.APPLICATION_JSON_TYPE, os -> {
            JsonWriter output = null;
            try {
                output = new JsonWriter(new OutputStreamWriter(new BufferedOutputStream(os)));
                output.setIndent("  ");
                output.beginArray();
                while (iterator.hasNext()) {
                    final JsonWritable value = iterator.next();
                    value.write(output);
                }
                output.endArray();
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            } finally {
                IO.close(output);
            }
        });
    }


    /**
     * A JAX-RS provider used to handle exceptions
     */
    @Provider
    @lombok.extern.slf4j.Slf4j
    public static class DefaultExceptionMapper implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception e) {
            log.error(e.getMessage(), e);
            return Response.status(500, e.getMessage()).build();
        }
    }


    /**
     * A JAX-RS provider for parameter converters
     */
    @Provider
    public static class RestParameterConverterProvider implements ParamConverterProvider {
        @Override
        @SuppressWarnings("unchecked")
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type type, Annotation[] annotations) {
            if (rawType.equals(String.class)) {
                return (ParamConverter<T>)new StringParamConverter();
            } else if (rawType.equals(Boolean.class)) {
                return (ParamConverter<T>)new BooleanParamConverter();
            } else if (rawType.equals(Integer.class)) {
                return (ParamConverter<T>)new IntegerParamConverter();
            } else if (rawType.equals(Long.class)) {
                return (ParamConverter<T>)new LongParamConverter();
            } else if (rawType.equals(Double.class)) {
                return (ParamConverter<T>)new DoubleParamConverter();
            } else if (rawType.equals(LocalDate.class)) {
                return (ParamConverter<T>)new TemporalParamConverter<>(LocalDate.class);
            } else if (rawType.equals(Currency.class)) {
                return (ParamConverter<T>)new CurrencyParamConverter();
            } else if (rawType.equals(LocalTime.class)) {
                return (ParamConverter<T>)new TemporalParamConverter<>(LocalTime.class);
            } else if (rawType.equals(LocalDateTime.class)) {
                return (ParamConverter<T>)new LocalDateTimeParamConverter();
            } else if (rawType.equals(ZonedDateTime.class)) {
                return (ParamConverter<T>)new TemporalParamConverter<>(ZonedDateTime.class);
            } else if (rawType.equals(Instant.class)) {
                return (ParamConverter<T>)new TemporalParamConverter<>(Instant.class);
            } else if (rawType.equals(ZoneId.class) || rawType.getName().equals("java.time.ZoneRegion")) {
                return (ParamConverter<T>) new ZoneIdParamConverter();
            } else if (rawType.equals(TimeZone.class) || rawType.getName().equals("sun.util.calendar.ZoneInfo")) {
                return (ParamConverter<T>)new TimeZoneParamConverter();
            } else if (rawType.equals(List.class) && type instanceof ParameterizedType) {
                final Type paramType = ((ParameterizedType) type).getActualTypeArguments()[0];
                final ParamConverter<T> converter = getConverter((Class<T>) paramType, paramType, annotations);
                return (ParamConverter<T>) new ListParamConverter<>(converter);
            } else if (rawType.equals(Set.class)) {
                final Type paramType = ((ParameterizedType) type).getActualTypeArguments()[0];
                final ParamConverter<T> converter = getConverter((Class<T>) paramType, paramType, annotations);
                return (ParamConverter<T>) new SetParamConverter<>(converter);
            } else {
                return null;
            }
        }
    }

    /**
     * A param converter for Boolean
     */
    @Provider
    static class BooleanParamConverter implements ParamConverter<Boolean> {
        @Override
        public Boolean fromString(String value) {
            return value == null ? null : Boolean.parseBoolean(value);
        }
        @Override
        public String toString(Boolean value) {
            return value != null ? value.toString() : null;
        }
    }


    /**
     * A param converter for Integer
     */
    @Provider
    static class IntegerParamConverter implements ParamConverter<Integer> {
        @Override
        public Integer fromString(String value) {
            return value == null ? null : Integer.parseInt(value);
        }
        @Override
        public String toString(Integer value) {
            return value != null ? value.toString() : null;
        }
    }


    /**
     * A param converter for Long
     */
    @Provider
    static class LongParamConverter implements ParamConverter<Long> {
        @Override
        public Long fromString(String value) {
            return value == null ? null : Long.parseLong(value);
        }
        @Override
        public String toString(Long value) {
            return value != null ? value.toString() : null;
        }
    }


    /**
     * A param converter for Double
     */
    @Provider
    static class DoubleParamConverter implements ParamConverter<Double> {
        @Override
        public Double fromString(String value) {
            return value == null ? null : Double.parseDouble(value);
        }
        @Override
        public String toString(Double value) {
            return value != null ? value.toString() : null;
        }
    }


    /**
     * A param converter for String
     */
    @Provider
    static class StringParamConverter implements ParamConverter<String> {
        @Override
        public String fromString(String value) {
            return value;
        }
        @Override
        public String toString(String value) {
            return value;
        }
    }


    /**
     * A param converter for ZoneId
     */
    @Provider
    static class ZoneIdParamConverter implements ParamConverter<ZoneId> {
        @Override
        public ZoneId fromString(String value) {
            return value == null ? null : ZoneId.of(value);
        }
        @Override
        public String toString(ZoneId value) {
            return value != null ? value.getId() : null;
        }
    }


    /**
     * A param converter for TimeZone
     */
    @Provider
    static class TimeZoneParamConverter implements ParamConverter<TimeZone> {
        @Override
        public TimeZone fromString(String value) {
            return value == null ? null : TimeZone.getTimeZone(value);
        }
        @Override
        public String toString(TimeZone value) {
            return value != null ? value.getID() : null;
        }
    }


    /**
     * A param converter for Currency
     */
    @Provider
    static class CurrencyParamConverter implements ParamConverter<Currency> {
        @Override
        public Currency fromString(String code) {
            return code != null ? Currency.getInstance(code) : null;
        }

        @Override
        public String toString(Currency currency) {
            return currency != null ? currency.getCurrencyCode() : null;
        }
    }


    /**
     * A param converter for Lists
     * @param <T>   the type for converter
     */
    @lombok.AllArgsConstructor()
    static class ListParamConverter<T> implements ParamConverter<List<T>> {
        private ParamConverter<T> converter;
        @Override
        public List<T> fromString(String value) {
            if (value == null) {
                return Collections.emptyList();
            } else {
                final String[] tokens = value.split(",");
                return Stream.of(tokens).map(converter::fromString).collect(Collectors.toList());
            }
        }
        @Override
        public String toString(List<T> values) {
            if (values == null || values.isEmpty()) {
                return null;
            } else {
                final List<String> strings = values.stream().map(converter::toString).collect(Collectors.toList());
                return String.join(",", strings);
            }
        }
    }



    /**
     * A param converter for Sets
     * @param <T>   the type for converter
     */
    @lombok.AllArgsConstructor()
    static class SetParamConverter<T> implements ParamConverter<Set<T>> {
        private ParamConverter<T> converter;
        @Override
        public Set<T> fromString(String value) {
            if (value == null) {
                return Collections.emptySet();
            } else {
                final String[] tokens = value.split(",");
                return Stream.of(tokens).map(converter::fromString).collect(Collectors.toSet());
            }
        }
        @Override
        public String toString(Set<T> values) {
            if (values == null || values.isEmpty()) {
                return null;
            } else {
                final Set<String> strings = values.stream().map(converter::toString).collect(Collectors.toSet());
                return String.join(",", strings);
            }
        }
    }




    /**
     * A LocalDateTime param converter
     */
    @Provider
    static class LocalDateTimeParamConverter implements ParamConverter<LocalDateTime> {

        private DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        private DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        private DateTimeFormatter format3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private DateTimeFormatter format4 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime fromString(String string) {
            if (string == null || string.trim().length() == 0) {
                return null;
            } else if (string.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(string, format1);
            } else if (string.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(string, format2);
            } else if (string.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(string, format3);
            } else if (string.matches("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(string, format4);
            } else {
                return LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }

        @Override
        public String toString(LocalDateTime value) {
            return value == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
        }
    }


    /**
     * A param converter for Temporal data types
     * @param <T>   the data type
     */
    @Provider
    static class TemporalParamConverter<T extends Temporal> implements ParamConverter<T> {

        private Class<T> type;

        /**
         * Constructor
         * @param type  the temporal type
         */
        TemporalParamConverter(Class<T> type) {
            this.type = type;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T fromString(String string) {
            if (string == null || string.trim().length() == 0) {
                return null;
            } else if (type.equals(LocalDate.class)) {
                return (T)LocalDate.parse(string, DateTimeFormatter.ISO_DATE);
            } else if (type.equals(LocalTime.class)) {
                return (T)LocalTime.parse(string, DateTimeFormatter.ISO_TIME);
            } else if (type.equals(LocalDateTime.class)) {
                return (T)LocalDateTime.parse(string, DateTimeFormatter.ISO_DATE_TIME);
            } else if (type.equals(ZonedDateTime.class)) {
                return (T)ZonedDateTime.parse(string, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            } else if (type.equals(Instant.class)) {
                return (T)Instant.ofEpochMilli(Long.parseLong(string));
            } else {
                throw new IllegalStateException("Unsupported Temporal type for param converter: " + type);
            }
        }

        @Override
        public String toString(T temporal) {
            if (temporal == null) {
                return null;
            } else if (temporal instanceof LocalDate) {
                return DateTimeFormatter.ISO_DATE.format(temporal);
            } else if (temporal instanceof LocalTime) {
                return DateTimeFormatter.ISO_TIME.format(temporal);
            } else if (temporal instanceof LocalDateTime) {
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(temporal);
            } else if (temporal instanceof ZonedDateTime) {
                return DateTimeFormatter.ISO_ZONED_DATE_TIME.format(temporal);
            } else if (temporal instanceof Instant) {
                return String.valueOf(((Instant)temporal).toEpochMilli());
            } else {
                throw new IllegalStateException("Unsupported Temporal type for param converter: " + type);
            }
        }
    }

}
