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
package com.d3x.core.util;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ArgParserTests {


    @Test()
    public void basic() {
        final ArgParser parser = ArgParser.of("--help", "--count", "24");
        Assert.assertEquals(parser.getArgs().size(), 3);
        Assert.assertTrue(parser.contains("--help"));
        Assert.assertEquals(parser.getInt("--count").get().intValue(), 24);
    }


    @Test()
    public void testDouble() throws Exception {
        final ArgParser parser = ArgParser.of("--price", "23.456d");
        Assert.assertEquals(parser.getArgs().size(), 2);
        Assert.assertEquals(parser.getDouble("--price").get(), 23.456d, 0.000001d);
    }


    @Test()
    public void testDate() throws Exception {
        final ArgParser parser = ArgParser.of("--date", "2014-01-01");
        Assert.assertEquals(parser.getArgs().size(), 2);
        Assert.assertEquals(parser.getLocalDate("--date").get(), LocalDate.of(2014, 1, 1));
    }


    @Test()
    public void testTime() throws Exception {
        final ArgParser parser = ArgParser.of("--time", "18:22");
        Assert.assertEquals(parser.getArgs().size(), 2);
        Assert.assertEquals(parser.getLocalTime("--time").get(), LocalTime.of(18, 22, 0));
    }


    @Test()
    public void testUrl() throws Exception {
        final ArgParser parser = ArgParser.of("--url", "https://www.d3xsystems.com");
        Assert.assertEquals(parser.getArgs().size(), 2);
        Assert.assertEquals(parser.getUrl("--url").get(), new URL("https://www.d3xsystems.com"));
    }

}
