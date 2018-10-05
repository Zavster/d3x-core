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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the Memory class
 *
 * @author Xavier Witdouck
 */
public class MemoryTests {


    @Test()
    public void parse() {
        Assert.assertEquals(Memory.parse("25"), Memory.of(25));
        Assert.assertEquals(Memory.parse("1024"), Memory.of(Memory.ONE_KB));
        Assert.assertEquals(Memory.parse("1K"), Memory.of(Memory.ONE_KB));
        Assert.assertEquals(Memory.parse("1M"), Memory.of(Memory.ONE_MB));
        Assert.assertEquals(Memory.parse("1G"), Memory.of(Memory.ONE_GB));
        Assert.assertEquals(Memory.parse("1T"), Memory.of(Memory.ONE_TB));
        Assert.assertEquals(Memory.parse("1KB"), Memory.of(Memory.ONE_KB));
        Assert.assertEquals(Memory.parse("1MB"), Memory.of(Memory.ONE_MB));
        Assert.assertEquals(Memory.parse("1GB"), Memory.of(Memory.ONE_GB));
        Assert.assertEquals(Memory.parse("1TB"), Memory.of(Memory.ONE_TB));
        Assert.assertEquals(Memory.parse(String.valueOf(Math.pow(1024, 2))), Memory.of(Memory.ONE_MB));
        Assert.assertEquals(Memory.parse(String.valueOf(Math.pow(1024, 3))), Memory.of(Memory.ONE_GB));
        Assert.assertEquals(Memory.parse(String.valueOf(Math.pow(1024, 4))), Memory.of(Memory.ONE_TB));
    }


    @Test()
    public void sizing() {
        Assert.assertEquals(Memory.parse("10KB").getKiloBytes(), 10d);
        Assert.assertEquals(Memory.parse("10MB").getMegaBytes(), 10d);
        Assert.assertEquals(Memory.parse("10GB").getGigaBytes(), 10d);
        Assert.assertEquals(Memory.parse("10TB").getTeraBytes(), 10d);
    }
}
