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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A utility class with some convenience API calls related to Java collections
 *
 * @author Xavier Witdouck
 */
public class Collect {


    /**
     * Returns the additions made to the new collection given the old collection
     * @param prior     the prior version of the collection
     * @param current   the current version of the collection
     * @return          the stream of additions
     */
    public static <T> Set<T> additions(Collection<T> prior, Collection<T> current) {
        return current.stream().filter(k -> !prior.contains(k)).collect(Collectors.toSet());
    }


    /**
     * Returns the set of deletions to the new collection given the old collection
     * @param prior     the prior version of the collection
     * @param current   the current version of the collection
     * @return          the stream of deletions
     */
    public static <T> Set<T> deletions(Collection<T> prior, Collection<T> current) {
        return prior.stream().filter(k -> !current.contains(k)).collect(Collectors.toSet());
    }


    /**
     * Returns a new List of the values specified
     * @param values    the values to create a new list from
     * @param <T>       the element type
     * @return          the newly created list
     */
    @SafeVarargs
    public static <T> List<T> asList(T... values) {
        final List<T> result = new ArrayList<>(values.length);
        for (T value : values) result.add(value);
        return result;
    }


    /**
     * Returns a new List of the values specified
     * @param values    the values to create a new list from
     * @param <T>       the element type
     * @return          the newly created list
     */
    public static <T> List<T> asList(Iterable<T> values) {
        final List<T> result = new ArrayList<>();
        for (T value : values) result.add(value);
        return result;
    }


    /**
     * Returns a new List of the values specified
     * @param values    the values to create a new list from
     * @param <T>       the element type
     * @return          the newly created list
     */
    public static <T> List<T> asList(Iterator<T> values) {
        final List<T> list = new ArrayList<>();
        values.forEachRemaining(list::add);
        return list;
    }


    /**
     * Returns a new List of the values specified
     * @param values    the values to create a new list from
     * @param <T>       the element type
     * @return          the newly created list
     */
    @SafeVarargs
    public static <T> List<T> asLinkedList(T... values) {
        final List<T> list = new LinkedList<>();
        for (T value : values) list.add(value);
        return list;
    }


    /**
     * Returns a new List of the values specified
     * @param values    the values to create a new list from
     * @param <T>       the element type
     * @return          the newly created list
     */
    public static <T> List<T> asLinkedList(Iterable<T> values) {
        final List<T> list = new LinkedList<>();
        for (T value : values) list.add(value);
        return list;
    }


    /**
     * Returns a new List of the values specified
     * @param values    the values to create a new list from
     * @param <T>       the element type
     * @return          the newly created list
     */
    public static <T> List<T> asLinkedList(Iterator<T> values) {
        final List<T> list = new LinkedList<>();
        values.forEachRemaining(list::add);
        return list;
    }


    /**
     * Returns a new Stream of the values from the Iterator
     * @param values    the values to create a new Set from
     * @param <T>       the element type
     * @return          the newly created set
     */
    public static <T> Stream<T> asStream(Iterator<T> values) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(values, Spliterator.ORDERED), false);
    }


    /**
     * Returns a new Stream of the values from the Iterable
     * @param values    the values to create a new Set from
     * @param <T>       the element type
     * @return          the newly created set
     */
    public static <T> Stream<T> asStream(Iterable<T> values) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(values.iterator(), Spliterator.ORDERED), false);
    }


    /**
     * Returns a new Set of the values specified
     * @param values    the values to create a new Set from
     * @param <T>       the element type
     * @return          the newly created set
     */
    @SafeVarargs
    public static <T> Set<T> asSet(T... values) {
        final Set<T> result = new HashSet<>(values.length);
        for (T value : values) result.add(value);
        return result;
    }


    /**
     * Returns a new Set of the values specified
     * @param values    the values to create a new Set from
     * @param <T>       the element type
     * @return          the newly created set
     */
    @SafeVarargs
    public static <T> SortedSet<T> asSortedSet(T... values) {
        final SortedSet<T> result = new TreeSet<>();
        for (T value : values) result.add(value);
        return result;
    }


    /**
     * Returns a new Set of the values specified
     * @param values    the values to create a new Set from
     * @param <T>       the element type
     * @return          the newly created set
     */
    public static <T> Set<T> asSet(Iterable<T> values) {
        final SortedSet<T> result = new TreeSet<>();
        for (T value : values) result.add(value);
        return result;
    }


    /**
     * Returns a new SortedSet of the values specified
     * @param values    the values to create a new SortedSet from
     * @param <T>       the element type
     * @return          the newly created set
     */
    public static <T> SortedSet<T> asSortedSet(Iterable<T> values) {
        final SortedSet<T> result = new TreeSet<>();
        for (T value : values) result.add(value);
        return result;
    }


    /**
     * Returns a new created Map initialized with whatever the consumer does
     * @param mapper            the consumer that sets up mappings
     * @param <K>               the key type
     * @param <V>               the value type
     * @return                  the newly created map
     */
    public static <K,V> Map<K,V> asMap(Consumer<Map<K,V>> mapper) {
        final Map<K,V> map = new HashMap<>();
        mapper.accept(map);
        return map;
    }


    /**
     * Returns a new created Map initialized with whatever the consumer does
     * @param initialCapacity   the initial capacity for apply
     * @param mapper            the consumer that sets up mappings
     * @param <K>               the key type
     * @param <V>               the value type
     * @return                  the newly created map
     */
    public static <K,V> Map<K,V> asMap(int initialCapacity, Consumer<Map<K,V>> mapper) {
        final Map<K,V> map = new HashMap<>(initialCapacity);
        mapper.accept(map);
        return map;
    }


    /**
     * Returns a new created Map initialized with whatever the consumer does
     * @param mapper            the consumer that sets up mappings
     * @param <K>               the key type
     * @param <V>               the value type
     * @return                  the newly created map
     */
    public static <K,V> SortedMap<K,V> asSortedMap(Consumer<Map<K,V>> mapper) {
        final SortedMap<K,V> map = new TreeMap<>();
        mapper.accept(map);
        return map;
    }


    /**
     * Returns a new created Map initialized with whatever the consumer does
     * @param mapper            the consumer that sets up mappings
     * @param <K>               the key type
     * @param <V>               the value type
     * @return                  the newly created map
     */
    public static <K,V> Map<K,V> asOrderedMap(Consumer<Map<K,V>> mapper) {
        final Map<K,V> map = new LinkedHashMap<>();
        mapper.accept(map);
        return map;
    }


    /**
     * Returns a new Iterable wrapper of the stream
     * @param stream        the stream to wrap
     * @param <T>           the entity type
     * @return              the newly created iterable
     */
    public static <T> Iterable<T> asIterable(Stream<T> stream) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return stream.iterator();
            }
        };
    }


    /**
     * Returns a apply that reverses the input apply
     * @param map   the apply reference to reverse
     * @param <K>   the type for key
     * @param <V>   the type for value
     * @return      the reverse mapped
     */
    public static <K,V> Map<V,K> reverse(Map<K,V> map) {
        if (map instanceof SortedMap) {
            final Map<V,K> result = new TreeMap<>();
            map.forEach((key, value) -> result.put(value, key));
            return result;
        } else if (map instanceof LinkedHashMap) {
            final Map<V,K> result = new LinkedHashMap<>(map.size());
            map.forEach((key, value) -> result.put(value, key));
            return result;
        } else {
            final Map<V,K> result = new HashMap<>(map.size());
            map.forEach((key, value) -> result.put(value, key));
            return result;
        }
    }
}
