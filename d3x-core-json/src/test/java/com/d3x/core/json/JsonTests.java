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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.d3x.core.util.Crypto;
import com.d3x.core.util.Formatter;
import com.d3x.core.util.Generic;
import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import com.d3x.core.util.Password;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the Json adapter
 *
 * @author Xavier Witdouck
 */
public class JsonTests {


    @Test()
    public void serialization() {
        Payload expected = Payload.random();
        Gson gson = Json.createGsonBuilder(Option.empty()).setPrettyPrinting().create();
        String jsonString = gson.toJson(expected);
        IO.println(jsonString);
        Payload actual = gson.fromJson(jsonString, Payload.class);
        JsonObject object = (JsonObject)gson.toJsonTree(actual);
        Assert.assertEquals(actual, expected);
        Assert.assertEquals(Json.getIntOrFail(object, "id"), expected.id.intValue());
        Assert.assertEquals(Json.getBooleanOrFail(object, "test"), expected.test.booleanValue());
        Assert.assertEquals(Json.getStringOrFail(object, "name"), expected.name);
        Assert.assertEquals(Json.getUrlOrFail(object, "url"), expected.url);
        Assert.assertEquals(Json.getDoubleOrFail(object, "price"), expected.price);
    }


    @Test()
    public void nulls() {
        Payload expected = new Payload();
        Gson gson = Json.createGsonBuilder(Option.empty()).setPrettyPrinting().create();
        String jsonString = gson.toJson(expected);
        IO.println(jsonString);
        Payload actual = gson.fromJson(jsonString, Payload.class);
        Assert.assertEquals(actual, expected);
        JsonObject object = (JsonObject)gson.toJsonTree(expected);
        Assert.assertTrue(Json.getInt(object, "id").isEmpty());
        Assert.assertTrue(Json.getString(object, "name").isEmpty());
        Assert.assertTrue(Json.getUrl(object, "url").isEmpty());
        Assert.assertTrue(Json.getBoolean(object, "test").isEmpty());
        Assert.assertTrue(Json.getElement(object, "missing").isEmpty());
    }


    @Test()
    public void objectCreate() {
        JsonObject value = Json.object(o -> {
            o.addProperty("name", "Xavier");
            o.addProperty("date", "2018-01-01");
            o.addProperty("price", 123.4d);
            o.addProperty("integer", 123);
            o.addProperty("long", 123456L);
            o.addProperty("enabled", true);
            o.addProperty("dayOfWeek", DayOfWeek.WEDNESDAY.name());
            o.add("array", Json.array(1, 2, 3));
            o.add("child", Json.object(c -> {
                c.addProperty("parent", "xxx");
                c.addProperty("axis", "7");
            }));
        });
        Assert.assertEquals(Json.getStringOrFail(value, "name"), "Xavier");
        Assert.assertEquals(Json.getStringOrFail(value, "date"), "2018-01-01");
        Assert.assertNotNull(Json.getDouble(value, "price").orNull());
        Assert.assertNotNull(Json.getNumber(value, "integer").orNull());
        Assert.assertNotNull(Json.getLong(value, "long").orNull());
        Assert.assertNotNull(Json.getEnum(value, "dayOfWeek", DayOfWeek.class).orNull());
        Assert.assertEquals(Json.getDoubleOrFail(value, "price"), 123.4d);
        Assert.assertEquals(Json.getNumberOrFail(value, "integer"), 123);
        Assert.assertEquals(Json.getLongOrFail(value, "long"), 123456L);
        Assert.assertEquals(Json.getEnumOrFail(value, "dayOfWeek", DayOfWeek.class), DayOfWeek.WEDNESDAY);
        Assert.assertTrue(Json.getBooleanOrFail(value, "enabled"));
        Assert.assertNotNull(Json.getArray(value, "array").orNull());
        Assert.assertNotNull(Json.getObject(value, "child").orNull());
        Assert.assertEquals(Json.getArrayOrFail(value, "array"), Json.array(1, 2, 3));
        Assert.assertEquals(Json.getObjectOrFail(value, "child"), Json.object(o -> {
            o.addProperty("parent", "xxx");
            o.addProperty("axis", "7");
        }));
    }


    @Test()
    public void arrayCreate() {
        Assert.assertEquals(Json.array(v -> v.addAll(Json.array(List.of(1, 2, 3)))), Json.array(1, 2, 3));
        Assert.assertEquals(Json.stream(Json.array(List.of(1, 2, 3))).map(JsonElement::getAsNumber).collect(Collectors.toList()), List.of(1, 2, 3));
        Assert.assertEquals(Json.stream(Json.array(List.of(1d, 2d, 3d))).map(JsonElement::getAsNumber).collect(Collectors.toList()), List.of(1d, 2d, 3d));
        Assert.assertEquals(Json.stream(Json.array(List.of("1", "2", "3"))).map(JsonElement::getAsString).collect(Collectors.toList()), List.of("1", "2", "3"));
        Assert.assertEquals(Json.stream(Json.array(List.of(true, false))).map(JsonElement::getAsBoolean).collect(Collectors.toList()), List.of(true, false));
    }



    @Test()
    public void password() throws Exception {
        String expected = "Hello World!";
        Crypto crypto = new Crypto("AES", Crypto.createKey("AES", 128, Option.empty()));
        Gson gson = Json.createGsonBuilder(Option.of(crypto)).setPrettyPrinting().create();
        String cipherText = crypto.encrypt(expected);
        Password password = Password.of(expected);
        JsonElement element = gson.toJsonTree(password);
        Assert.assertEquals(element.getAsString(), "encrypted(" + cipherText  + ")");
        Assert.assertEquals(gson.fromJson(element, Password.class).getValue(), password.getValue());
        IO.println(gson.toJsonTree(password).getAsString());
    }


    @Test()
    public void writable() {
        Gson gson = Json.createGsonBuilder(Option.empty()).create();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        List<Payload> expected = IntStream.range(0, 20).mapToObj(i -> Payload.random()).collect(Collectors.toList());
        JsonWritable.serializer(expected.stream()).accept(bytes);
        String jsonString = new String(bytes.toByteArray());
        List<Payload> actual = gson.fromJson(jsonString, Generic.of(List.class, Payload.class));
        Assert.assertEquals(actual, expected);
    }




    @lombok.ToString()
    @lombok.EqualsAndHashCode()
    public static class Payload implements JsonWritable {
        private Integer id;
        private Boolean test;
        private String name;
        private LocalDate date;
        private LocalTime time;
        private LocalDateTime dateTime;
        private ZoneId zoneId;
        private URL url;
        private double price;
        private Currency currency;
        private Duration duration;
        private TimeZone timeZone;
        private ZonedDateTime lastUpdated;
        private Option<String> option;
        private DayOfWeek dayOfWeek;

        static Payload random() {
            try {
                Payload value = new Payload();
                value.id = new Random().nextInt();
                value.test = false;
                value.name = "Xavier Witdouck";
                value.date = LocalDate.now();
                value.time = LocalTime.now();
                value.url = new URL("https://www.d3xsytems.com");
                value.price = Math.random();
                value.dateTime = LocalDateTime.now();
                value.zoneId = ZoneId.systemDefault();
                value.currency = Currency.getInstance("GBP");
                value.duration = Duration.ofMillis(1200);
                value.timeZone = TimeZone.getDefault();
                value.lastUpdated = ZonedDateTime.now();
                value.option = Option.of("Option Value");
                value.dayOfWeek = DayOfWeek.SATURDAY;
                return value;
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }

        @Override
        public void write(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("id").value(id);
            writer.name("test").value(test);
            writer.name("name").value(name);
            writer.name("date");
            writer.value(date == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format(date));
            writer.name("time");
            writer.value(time == null ? null : DateTimeFormatter.ISO_LOCAL_TIME.format(time));
            writer.name("dateTime");
            writer.value(dateTime == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime));
            writer.name("zoneId");
            writer.value(zoneId == null ? null : zoneId.getId());
            writer.name("url");
            writer.value(url == null ? null : url.toString());
            writer.name("price").value(price);
            writer.name("currency");
            writer.value(currency == null ? null : currency.getCurrencyCode());
            writer.name("duration");
            writer.value(duration == null ? null : Formatter.format(duration));
            writer.name("timeZone");
            writer.value(timeZone == null ? null : timeZone.getID());
            writer.name("lastUpdated");
            writer.value(lastUpdated == null ? null : DateTimeFormatter.ISO_ZONED_DATE_TIME.format(lastUpdated));
            writer.name("option");
            writer.value(option == null || option.isEmpty() ? null : option.get());
            writer.name("dayOfWeek");
            writer.value(dayOfWeek == null ? null : dayOfWeek.name());
            writer.endObject();
        }
    }
}
