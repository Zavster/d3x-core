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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the JsonMessage class
 *
 * @author Xavier Witdouck
 */
public class JsonStreamTests {


    static {
        JsonStream.register(Type1.class, "message-type-1");
        JsonStream.register(Type2.class, "message-type-2");
    }

    @Test()
    public void stream() throws IOException {
        List<JsonWritable> expected = new ArrayList<>();
        expected.addAll(IntStream.range(0, 20).mapToObj(i -> Type1.random()).collect(Collectors.toList()));
        expected.addAll(IntStream.range(0, 20).mapToObj(i -> Type2.random()).collect(Collectors.toList()));
        Collections.shuffle(expected);
        Gson gson = Json.createGsonBuilder(Option.empty()).create();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024 * 100);
        JsonStream.writer(bytes).write(expected.iterator());
        IO.println(new String(bytes.toByteArray()));
        List<JsonWritable> actual = JsonStream.<JsonWritable>reader(gson).list(new ByteArrayInputStream(bytes.toByteArray()));
        Assert.assertEquals(actual, expected);
    }






    @lombok.ToString()
    @lombok.EqualsAndHashCode()
    @lombok.AllArgsConstructor()
    public static class Type1 implements JsonWritable {
        private Year year;
        private Month month;
        private DayOfWeek dayOfWeek;

        static Type1 random() {
            Random random = new Random();
            return new Type1(
                    Year.of(random.nextInt(2018)),
                    Month.JANUARY,
                    DayOfWeek.WEDNESDAY
            );
        }

        @Override
        public void write(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("year");
            writer.value(year != null ? year.getValue() : null);
            writer.name("month");
            writer.value(month != null ? month.name() : null);
            writer.name("dayOfWeek");
            writer.value(dayOfWeek != null ? dayOfWeek.name() : null);
            writer.endObject();
        }
    }


    @lombok.ToString()
    @lombok.EqualsAndHashCode()
    @lombok.AllArgsConstructor()
    public static class Type2 implements JsonWritable {
        private String name;
        private double price;
        private long lastChanged;

        static Type2 random() {
            return new Type2(String.valueOf(Math.random()), Math.random(), System.currentTimeMillis());
        }

        @Override
        public void write(JsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("name");
            writer.value(name);
            writer.name("price");
            writer.value(price);
            writer.name("lastChanged");
            writer.value(lastChanged);
            writer.endObject();
        }
    }


}
