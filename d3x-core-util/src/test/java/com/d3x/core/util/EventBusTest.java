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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the EventBus class
 *
 * @author Xavier Witdouck
 */
public class EventBusTest {


    @Test()
    public void single() throws Exception {
        Results<Integer> ints = new Results<>(Integer.class);
        Results<Double> doubles = new Results<>(Double.class);
        Results<Long> longs = new Results<>(Long.class);
        EventBus eventBus = new EventBus();
        eventBus.attach(Integer.class, ints);
        eventBus.attach(Long.class, longs);
        eventBus.attach(Double.class, doubles);
        List.of(1, 2, 3).forEach(eventBus::publish);
        List.of("1", "2", "3").forEach(eventBus::publish);
        List.of(1d, 2d, 3d, 4d).forEach(eventBus::publish);
        Thread.sleep(1000);
        Assert.assertEquals(ints.messages.size(), 3);
        Assert.assertEquals(longs.messages.size(), 0);
        Assert.assertEquals(doubles.messages.size(), 4);
        Assert.assertEquals(ints.messages, List.of(1, 2, 3));
        Assert.assertEquals(longs.messages, Collections.emptyList());
        Assert.assertEquals(doubles.messages, List.of(1d, 2d, 3d, 4d));
    }


    @Test()
    public void multiple() throws Exception {
        Results<Integer> ints = new Results<>(Integer.class);
        Results<Double> doubles = new Results<>(Double.class);
        Results<Long> longs = new Results<>(Long.class);
        EventBus eventBus = new EventBus();
        eventBus.attach(Integer.class, ints);
        eventBus.attach(Long.class, longs);
        eventBus.attach(Double.class, doubles);
        eventBus.publishAll(List.of(1, 2, 3));
        eventBus.publishAll(List.of("1", "2", "3"));
        eventBus.publishAll(List.of(1d, 2d, 3d, 4d));
        Thread.sleep(1000);
        Assert.assertEquals(ints.messages.size(), 3);
        Assert.assertEquals(longs.messages.size(), 0);
        Assert.assertEquals(doubles.messages.size(), 4);
        Assert.assertEquals(ints.messages, List.of(1, 2, 3));
        Assert.assertEquals(longs.messages, Collections.emptyList());
        Assert.assertEquals(doubles.messages, List.of(1d, 2d, 3d, 4d));
    }



    private class Results<T> implements EventListener<T> {
        private Class<T> type;
        private List<T> messages = new ArrayList<>();
        Results(Class<T> type) {
            this.type = type;
        }
        @Override
        public void onEvent(T message) {
            Assert.assertNotNull(message);
            Assert.assertEquals(message.getClass(), type);
            this.messages.add(message);
        }
    }
}
