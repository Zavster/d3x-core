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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit test for Identity interface
 *
 * @author Xavier Witdouck
 */
public class IdentityTest {

    private List<Person> persons = List.of(
        new Person("id-1"),
        new Person("id-2"),
        new Person("id-3"),
        new Person("id-4"),
        new Person("id-5"),
        new Person("id-6")
    );



    @Test()
    public void toMap() {
        Map<String,Person> map = Identity.toMap(persons);
        Assert.assertEquals(map.size(), persons.size());
        persons.forEach(p -> {
            Person value = map.get(p.getId());
            Assert.assertEquals(value.getId(), p.getId());
            Assert.assertEquals(value, p);
        });
    }


    @Test()
    public void toOrderedMap() {
        AtomicInteger counter = new AtomicInteger(7);
        List<Person> reversed = new ArrayList<>(persons);
        Collections.reverse(reversed);
        Map<String,Person> map = Identity.toOrderedMap(reversed);
        Assert.assertEquals(map.size(), persons.size());
        map.forEach((key, value) -> {
            String id = "id-" + counter.decrementAndGet();
            Person expected = persons.get(counter.get()-1);
            Assert.assertEquals(key, id);
            Assert.assertEquals(value.getId(), id);
            Assert.assertEquals(value, expected);
        });
    }





    @lombok.ToString()
    @lombok.AllArgsConstructor()
    private static class Person implements Identity<String> {
        @lombok.Getter private String id;
    }


}



