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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An interface to an object that exposes a unique identifier
 *
 * @param <K>   the unique id type
 *
 * @author Xavier Witdouck
 */
public interface Identity<K> {

    /**
     * Returns the unique identifier for this object
     * @return  the unique identifier for object
     */
    K getId();


    /**
     * Returns a map of keyed values created from the collection
     * @param values    the collection of Keyed values
     * @param <K>       the key type
     * @param <T>       the value type
     * @return          the map of keyed values
     */
    static <K,T extends Identity<K>> Map<K,T> toMap(Collection<T> values) {
        final Map<K,T> result = new HashMap<>(values.size());
        for (T value : values) {
            if (value != null) {
                final K key = value.getId();
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Returns a ordered map of keyed values created from the collection
     * @param values    the collection of Keyed values
     * @param <K>       the key type
     * @param <T>       the value type
     * @return          the order3ed map of keyed values
     */
    static <K,T extends Identity<K>> Map<K,T> toOrderedMap(Collection<T> values) {
        final Map<K,T> result = new LinkedHashMap<>(values.size());
        for (T value : values) {
            if (value != null) {
                final K key = value.getId();
                result.put(key, value);
            }
        }
        return result;
    }

}
