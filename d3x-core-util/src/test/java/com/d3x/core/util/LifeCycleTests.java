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

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the LifeCycle Adapter
 */
public class LifeCycleTests {


    @Test()
    public void start() {
        Service service = new Service();
        Assert.assertEquals(service.counter.get(), 0);
        Assert.assertFalse(service.isStarted());
        service.start();
        Assert.assertEquals(service.counter.get(), 1);
        Assert.assertTrue(service.isStarted());
        service.start();
        service.start();
        service.start();
        service.start();
        Assert.assertEquals(service.counter.get(), 1);
        Assert.assertTrue(service.isStarted());
    }


    @Test()
    public void stop() {
        Service service = new Service();
        Assert.assertEquals(service.counter.get(), 0);
        Assert.assertFalse(service.isStarted());
        service.start();
        Assert.assertEquals(service.counter.get(), 1);
        Assert.assertTrue(service.isStarted());
        service.stop();
        Assert.assertEquals(service.counter.get(), 0);
        Assert.assertFalse(service.isStarted());
        service.stop();
        service.stop();
        service.stop();
        service.stop();
        Assert.assertEquals(service.counter.get(), 0);
        Assert.assertFalse(service.isStarted());
    }



    private class Service extends LifeCycle.Base {
        private AtomicInteger counter = new AtomicInteger(0);
        @Override
        protected void doStart() throws RuntimeException {
            counter.incrementAndGet();
        }
        @Override
        protected void doStop() throws RuntimeException {
            counter.decrementAndGet();
        }
    }
}
