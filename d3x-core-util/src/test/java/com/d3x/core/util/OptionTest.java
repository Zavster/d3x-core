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

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the Option class
 *
 * @author Xavier Witdouck
 */
public class OptionTest {


    @Test(expectedExceptions = {NoSuchElementException.class})
    public void empty() {
        AtomicBoolean ran = new AtomicBoolean();
        Assert.assertTrue(Option.empty().isEmpty());
        Assert.assertFalse(Option.empty().isPresent());
        Assert.assertTrue(Option.empty().map(v -> 12).isEmpty());
        Assert.assertEquals(Option.empty().orElse(12), 12);
        Assert.assertEquals(Option.empty().orElse(() -> 13), 13);
        Assert.assertNull(Option.empty().orNull());
        Option.empty().ifEmpty(() -> ran.set(true));
        Option.empty().ifPresent(v -> ran.set(false));
        Assert.assertTrue(ran.get());
        Option.empty().get();
    }


    @Test()
    public void present() {
        AtomicBoolean ran = new AtomicBoolean();
        Assert.assertFalse(Option.of(12).isEmpty());
        Assert.assertTrue(Option.of(12).isPresent());
        Assert.assertEquals(Option.of(16).get(), Integer.valueOf(16));
        Assert.assertTrue(Option.of(15).map(v -> 12).isPresent());
        Assert.assertEquals(Option.of(15).map(v -> 12).orNull(), Integer.valueOf(12));
        Assert.assertEquals(Option.of(8).orElse(12), Integer.valueOf(8));
        Assert.assertEquals(Option.of(2).orElse(() -> 13), Integer.valueOf(2));
        Assert.assertEquals(Option.of(3).orNull(), Integer.valueOf(3));
        Option.of(16).ifEmpty(() -> ran.set(false));
        Option.of(16).ifPresent(v -> ran.set(true));
        Assert.assertTrue(ran.get());
    }


    @Test(expectedExceptions = {RuntimeException.class})
    public void orThrow1() {
        Assert.assertEquals(Option.of(16).orThrow(() -> new IllegalStateException("")), Integer.valueOf(16));
        Option.empty().orThrow("No value for option");
    }


    @Test(expectedExceptions = {IllegalStateException.class})
    public void orThrow2() {
        Assert.assertEquals(Option.of(16).orThrow(() -> new RuntimeException("")), Integer.valueOf(16));
        Option.empty().orThrow(() -> new IllegalStateException("No value for option"));
    }

}
