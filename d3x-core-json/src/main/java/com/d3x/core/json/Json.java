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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.d3x.core.util.Crypto;
import com.d3x.core.util.Formatter;
import com.d3x.core.util.Option;
import com.d3x.core.util.Password;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


/**
 * A utility class that provides some convenience functions over the GSON api
 *
 * @author Xavier Witdouck
 */
public class Json {


    /**
     * Returns a stream of the elements in the JSON array
     * @param array     the JSON array
     * @return          the stream of elements
     */
    public static Stream<JsonElement> stream(JsonArray array) {
        return IntStream.range(0, array.size()).mapToObj(array::get);
    }


    /**
     * Returns a newly created object initialized by the consumer
     * @param initializer   the object initializer
     * @return              newly created object
     */
    public static JsonObject object(Consumer<JsonObject> initializer) {
        final JsonObject object = new JsonObject();
        if (initializer != null) initializer.accept(object);
        return object;
    }


    /**
     * Returns a newly created array initialized by the consumer
     * @param initializer   the array initializer
     * @return              newly created array
     */
    public static JsonArray array(Consumer<JsonArray> initializer) {
        final JsonArray array = new JsonArray();
        if (initializer != null) initializer.accept(array);
        return array;
    }


    /**
     * Returns a JSON array from the array of values
     * @param values    the array to create an array from
     * @return          the newly created json array
     */
    public static <T> JsonArray array(T... values) {
        return array(Arrays.asList(values));
    }


    /**
     * Returns a JSON array from the collection of values
     * @param values    the collection to create an array from
     * @return          the newly created json array
     */
    public static <T> JsonArray array(Collection<T> values) {
        final JsonArray array = new JsonArray();
        for (T value : values) {
            if (value instanceof String) {
                array.add((String)value);
            } else if (value instanceof Integer) {
                array.add((Integer)value);
            } else if (value instanceof Double) {
                array.add((Double)value);
            } else if (value instanceof Number) {
                array.add(((Number)value).doubleValue());
            } else if (value instanceof Boolean) {
                array.add((Boolean)value);
            } else {
                throw new IllegalArgumentException("Unsupported type for Json array: " + value.getClass());
            }
        }
        return array;
    }



    /**
     * Returns an option on the element in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option element
     */
    public static Option<JsonElement> getElement(JsonObject object, String name) {
        final JsonElement element = object.get(name);
        if (element == null || element.equals(JsonNull.INSTANCE)) {
            return Option.empty();
        } else {
            return Option.of(element);
        }
    }


    /**
     * Returns the field element in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field value is null or missing
     */
    public static JsonElement getElementOrFail(JsonObject object, String name) {
        final JsonElement element = object.get(name);
        if (element == null || element.equals(JsonNull.INSTANCE)) {
            throw new RuntimeException("No JSON value for field named: " + name);
        } else {
            return element;
        }
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<Boolean> getBoolean(JsonObject object, String name) {
        return getElement(object, name).map(JsonElement::getAsBoolean);
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<Integer> getInt(JsonObject object, String name) {
        return getElement(object, name).map(e -> e.getAsNumber().intValue());
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<Long> getLong(JsonObject object, String name) {
        return getElement(object, name).map(e -> e.getAsNumber().longValue());
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<Double> getDouble(JsonObject object, String name) {
        return getElement(object, name).map(e -> e.getAsNumber().doubleValue());
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<Number> getNumber(JsonObject object, String name) {
        return getElement(object, name).map(JsonElement::getAsNumber);
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<String> getString(JsonObject object, String name) {
        return getElement(object, name).map(JsonElement::getAsString);
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<JsonArray> getArray(JsonObject object, String name) {
        return getElement(object, name).map(JsonElement::getAsJsonArray);
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<JsonObject> getObject(JsonObject object, String name) {
        return getElement(object, name).map(JsonElement::getAsJsonObject);
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static <V extends Enum<V>> Option<V> getEnum(JsonObject object, String name, Class<V> type) {
        return getString(object, name).map(v -> Enum.valueOf(type, v));
    }


    /**
     * Returns an option on the field value in the object specified
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the option value
     */
    public static Option<URL> getUrl(JsonObject object, String name) {
        return getString(object, name).map(Json::toUrl);
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static boolean getBooleanOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsBoolean();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static int getIntOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsNumber().intValue();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static long getLongOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsNumber().longValue();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static double getDoubleOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsNumber().doubleValue();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static Number getNumberOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsNumber();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */

    public static String getStringOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsString();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static URL getUrlOrFail(JsonObject object, String name) {
        return getUrl(object, name).orThrow("No JSON value for field named: " + name);
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static JsonObject getObjectOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsJsonObject();
    }


    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static JsonArray getArrayOrFail(JsonObject object, String name) {
        return getElementOrFail(object, name).getAsJsonArray();
    }

    /**
     * Returns the value of a field in the object specified, or throws an exception if it is not set or is null
     * @param object    the object from which to extract the field value
     * @param name      the name of the field to extract
     * @return          the field value
     * @throws RuntimeException if the field is not set or is null
     */
    public static <V extends Enum<V>> V getEnumOrFail(JsonObject object, String name, Class<V> type) {
        return Enum.valueOf(type, getStringOrFail(object, name));
    }


    public static void forObject(JsonObject parent, String fieldName, boolean required, Consumer<JsonObject> handler) {
        final JsonElement element = parent.get(fieldName);
        if (element == null && required) {
            throw new RuntimeException("Missing required entry in JSON object for field named " + fieldName);
        } else if (element != null) {
            final JsonObject object = element.getAsJsonObject();
            handler.accept(object);
        }
    }


    public static void forArray(JsonObject parent, String fieldName, boolean required, Consumer<JsonArray> handler) {
        final JsonElement element = parent.get(fieldName);
        if (element == null && required) {
            throw new RuntimeException("Missing required entry in JSON object for field named " + fieldName);
        } else if (element != null) {
            final JsonArray array = element.getAsJsonArray();
            handler.accept(array);
        }
    }


    /**
     * Returns a URL object with the string provided
     * @param urlString     the url string
     * @return              the newly created URL
     */
    private static URL toUrl(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Malformed URL: " + urlString, ex);
        }
    }


    /**
     * Returns the GsonBuilder with a bunch of standard adapters
     * @param crypto    optional crypto to support encrypted password serialization
     * @return      the GSON builder instance
     */
    public static GsonBuilder createGsonBuilder(Option<Crypto> crypto) {
        final GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.serializeNulls();
        builder.serializeSpecialFloatingPointValues();
        builder.registerTypeAdapter(Option.class, new OptionSerializer());
        builder.registerTypeAdapter(Option.class, new OptionDeserializer());
        builder.registerTypeAdapter(ZoneId.class, new ZoneIdSerializer());
        builder.registerTypeAdapter(ZoneId.class, new ZoneIdDeserializer());
        builder.registerTypeAdapter(Year.class, new YearSerializer());
        builder.registerTypeAdapter(Year.class, new YearDeserializer());
        builder.registerTypeAdapter(TimeZone.class, new TimeZoneSerializer());
        builder.registerTypeAdapter(TimeZone.class, new TimeZoneDeserializer());
        builder.registerTypeAdapter(Currency.class, new CurrencySerializer());
        builder.registerTypeAdapter(Currency.class, new CurrencyDeserializer());
        builder.registerTypeAdapter(Duration.class, new DurationSerializer());
        builder.registerTypeAdapter(Duration.class, new DurationDeserializer());
        builder.registerTypeAdapter(Password.class, new PasswordSerializer(crypto));
        builder.registerTypeAdapter(Password.class, new PasswordDeserializer(crypto));
        builder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        builder.registerTypeAdapter(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE));
        builder.registerTypeAdapter(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
        builder.registerTypeAdapter(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ISO_LOCAL_TIME));
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        builder.registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        builder.registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        return builder;
    }



    public static class YearSerializer implements JsonSerializer<Year> {
        @Override
        public JsonElement serialize(Year value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.getValue());
        }
    }

    public static class YearDeserializer implements JsonDeserializer<Year> {
        @Override
        public Year deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : Year.of(json.getAsInt());
        }
    }


    @lombok.AllArgsConstructor()
    public static class LocalDateSerializer implements JsonSerializer<LocalDate> {
        private DateTimeFormatter formatter;
        @Override
        public JsonElement serialize(LocalDate value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(formatter.format(value));
        }
    }


    @lombok.AllArgsConstructor()
    public static class LocalTimeSerializer implements JsonSerializer<LocalTime> {
        private DateTimeFormatter formatter;
        @Override
        public JsonElement serialize(LocalTime value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(formatter.format(value));
        }
    }


    @lombok.AllArgsConstructor()
    public static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime> {
        private DateTimeFormatter formatter;
        @Override
        public JsonElement serialize(LocalDateTime value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(formatter.format(value));
        }
    }


    @lombok.AllArgsConstructor()
    public static class ZonedDateTimeSerializer implements JsonSerializer<ZonedDateTime> {
        private DateTimeFormatter formatter;
        @Override
        public JsonElement serialize(ZonedDateTime value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(formatter.format(value));
        }
    }


    @lombok.AllArgsConstructor()
    public static class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
        private DateTimeFormatter formatter;
        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : LocalDate.parse(json.getAsString(), formatter);
        }
    }


    @lombok.AllArgsConstructor()
    public static class LocalTimeDeserializer implements JsonDeserializer<LocalTime> {
        private DateTimeFormatter formatter;
        @Override
        public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : LocalTime.parse(json.getAsString(), formatter);
        }
    }


    @lombok.AllArgsConstructor()
    public static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime> {
        private DateTimeFormatter formatter;
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : LocalDateTime.parse(json.getAsString(), formatter);
        }
    }


    @lombok.AllArgsConstructor()
    public static class ZonedDateTimeDeserializer implements JsonDeserializer<ZonedDateTime> {
        private DateTimeFormatter formatter;
        @Override
        public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : ZonedDateTime.parse(json.getAsString(), formatter);
        }
    }


    public static class ZoneIdSerializer implements JsonSerializer<ZoneId> {
        @Override
        public JsonElement serialize(ZoneId value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.getId());
        }
    }


    public static class ZoneIdDeserializer implements JsonDeserializer<ZoneId> {
        @Override
        public ZoneId deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : ZoneId.of(json.getAsString());
        }
    }


    public static class TimeZoneSerializer implements JsonSerializer<TimeZone> {
        @Override
        public JsonElement serialize(TimeZone value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.getID());
        }
    }


    public static class TimeZoneDeserializer implements JsonDeserializer<TimeZone> {
        @Override
        public TimeZone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : TimeZone.getTimeZone(json.getAsString());
        }
    }


    public static class CurrencySerializer implements JsonSerializer<Currency> {
        @Override
        public JsonElement serialize(Currency value, Type typeOfSrc, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value.getCurrencyCode());
        }
    }


    public static class CurrencyDeserializer implements JsonDeserializer<Currency> {
        @Override
        public Currency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json == null || json.equals(JsonNull.INSTANCE) ? null : Currency.getInstance(json.getAsString());
        }
    }


    public static class DurationSerializer implements JsonSerializer<Duration> {
        @Override
        public JsonElement serialize(Duration value, Type type, JsonSerializationContext context) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(Formatter.format(value));
        }
    }


    public static class OptionSerializer implements JsonSerializer<Option<?>> {
        @Override
        public JsonElement serialize(Option<?> option, Type typeOfSrc, JsonSerializationContext context) {
            if (option == null || option.isEmpty()) return JsonNull.INSTANCE;
            else {
                final Object value = option.get();
                final Type valueType = value.getClass();
                return context.serialize(value, valueType);
            }
        }
    }


    public static class OptionDeserializer implements JsonDeserializer {
        @Override
        public Object deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.equals(JsonNull.INSTANCE)) {
                return Option.empty();
            } else {
                final ParameterizedType pType = (ParameterizedType)type;
                final Type actual = pType.getActualTypeArguments()[0];
                final Object value = context.deserialize(json, actual);
                return Option.of(value);
            }
        }
    }


    @lombok.AllArgsConstructor()
    public static class PasswordSerializer implements JsonSerializer<Password> {
        private Option<Crypto> crypto;
        @Override
        public JsonElement serialize(Password password, Type type, JsonSerializationContext context) {
            if (password == null) {
                return JsonNull.INSTANCE;
            } else {
                try {
                    final char[] value = password.getValue();
                    if (crypto.isEmpty()) {
                        return new JsonPrimitive(new String(value));
                    } else {
                        final String result = crypto.get().encrypt(new String(value));
                        return new JsonPrimitive("encrypted(" + result + ")");
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to encrypt password", ex);
                }
            }
        }
    }


    @lombok.AllArgsConstructor()
    public static class PasswordDeserializer implements JsonDeserializer<Password> {
        private Option<Crypto> crypto;
        @Override
        public Password deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (element == null || element.equals(JsonNull.INSTANCE)) {
                return null;
            } else {
                try {
                    final String value = element.getAsString();
                    if (value.length() == 0) {
                        return null;
                    } else if (value.trim().length() == 0) {
                        return Password.of(new char[0]);
                    } else {
                        final Matcher matcher = Pattern.compile("encrypted\\((.+)\\)").matcher("");
                        if (!matcher.reset(value).matches()) {
                            return Password.of(value.toCharArray());
                        } else if (crypto.isEmpty()) {
                            throw new RuntimeException("Cannot decrypt password as no Crypto configured in password deserializer");
                        } else {
                            final String cipherText = matcher.group(1);
                            final String clearText = crypto.get().decrypt(cipherText);
                            return Password.of(clearText.toCharArray());
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to decrypt password", ex);
                }
            }
        }
    }



    public static class DurationDeserializer implements JsonDeserializer<Duration> {
        private static final Pattern numberPattern = Pattern.compile("\\d+");
        private static final Pattern decimalPattern = Pattern.compile("\\d+\\.?\\d?");
        private static final Pattern secondPattern = Pattern.compile("(\\d+\\.?\\d?)\\s*(s|sec)", Pattern.CASE_INSENSITIVE);
        private static final Pattern minutePattern = Pattern.compile("(\\d+\\.?\\d?)\\s*(m|min)", Pattern.CASE_INSENSITIVE);
        private static final Pattern hourPattern = Pattern.compile("(\\d+\\.?\\d?)\\s*(h|hr)", Pattern.CASE_INSENSITIVE);
        private static final Pattern dayPattern = Pattern.compile("(\\d+\\.?\\d?)\\s*(d|days?)", Pattern.CASE_INSENSITIVE);
        private static final Pattern millisPattern = Pattern.compile("(\\d+\\.?\\d?)\\s*(ms|millis)", Pattern.CASE_INSENSITIVE);
        @Override
        public Duration deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json == JsonNull.INSTANCE) {
                return null;
            } else {
                final String text = json.getAsString();
                if (text.trim().length() == 0) {
                    return Duration.ZERO;
                } else {
                    try {
                        if (numberPattern.matcher(text).matches()) {
                            return Duration.ofMillis(Long.parseLong(text));
                        } else {
                            final Matcher millis = millisPattern.matcher(text);
                            if (millis.matches()) {
                                final long value = Long.parseLong(millis.group(1));
                                return Duration.ofMillis(value);
                            }
                            final Matcher seconds = secondPattern.matcher(text);
                            if (seconds.matches()) {
                                final double value = Double.parseDouble(seconds.group(1)) * 1000d;
                                return Duration.ofMillis((long)value);
                            }
                            final Matcher minutes = minutePattern.matcher(text);
                            if (minutes.matches()) {
                                final double value = Double.parseDouble(minutes.group(1)) * 60d * 1000d;
                                return Duration.ofMillis((long)value);
                            }
                            final Matcher hours = hourPattern.matcher(text);
                            if (hours.matches()) {
                                final double value = Double.parseDouble(hours.group(1)) * 60d * 60d * 1000d;
                                return Duration.ofMillis((long)value);
                            }
                            final Matcher days = dayPattern.matcher(text);
                            if (days.matches()) {
                                final double value = Double.parseDouble(days.group(1)) * 24d * 60d * 60d * 1000d;
                                return Duration.ofMillis((long)value);
                            }
                            final Matcher decimal = decimalPattern.matcher(text);
                            if (decimal.matches()) {
                                final double value = Double.parseDouble(text);
                                return Duration.ofMillis((long)value);
                            }
                            throw new RuntimeException("Malformed duration specification " + text);
                        }
                    } catch (Exception t) {
                        throw new RuntimeException("Failed to parse duration specification: " + text, t);
                    }
                }
            }
        }
    }

}
