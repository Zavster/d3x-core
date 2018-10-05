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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CollectTests {


    @Test()
    public void additions() {
        final List<Integer> values = Collect.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final List<Integer> expected = Collect.asList(15, 16, 17);
        final List<Integer> combined = Stream.concat(values.stream(), expected.stream()).collect(Collectors.toList());
        final Collection<Integer> additions = Collect.additions(values, combined);
        Assert.assertEquals(additions.size(), expected.size());
        Assert.assertTrue(additions.containsAll(expected));
    }


    @Test()
    public void deletions() {
        final List<Integer> prior = Collect.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final List<Integer> updated = Collect.asList(1, 2, 3, 4, 5, 6);
        final List<Integer> expected = Collect.asList(7, 8, 9);
        final Collection<Integer> deletions = Collect.deletions(prior, updated);
        Assert.assertEquals(deletions.size(), expected.size());
        Assert.assertTrue(deletions.containsAll(expected));
    }


    @Test()
    public void iterator() {
        final List<Integer> list = Collect.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final List<Integer> result = Collect.asList(list.iterator());
        Assert.assertTrue(System.identityHashCode(list) != System.identityHashCode(result));
        Assert.assertEquals(list.size(), result.size());
        Assert.assertTrue(result.containsAll(list));
    }


    @Test()
    public void stream() {
        final List<Integer> list = Collect.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        final List<Integer> x = Collect.asStream(list.iterator()).collect(Collectors.toList());
        final List<Integer> y = Collect.asStream(list).collect(Collectors.toList());
        Assert.assertEquals(x.size(), list.size());
        Assert.assertEquals(y.size(), list.size());
        Assert.assertTrue(x.containsAll(list));
        Assert.assertTrue(y.containsAll(list));
    }


    @Test()
    public void linkedList() {
        final List<Integer> list = Collect.asLinkedList(5, 6, 7);
        Assert.assertTrue(list instanceof LinkedList);
        Assert.assertEquals(list.size(), 3);
        Assert.assertTrue(list.containsAll(List.of(5, 6, 7)));
    }

}
