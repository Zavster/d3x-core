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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A class that matches a collection of Consumer<T> objects and dispatch calls to them
 * @param <T>   the type for consumer
 *
 * @author Xavier Witdouck
 */
public class Consumers<T> {

    private List<Consumer<T>> consumers = new ArrayList<>();

    /**
     * Notifies all registered consumers of the value
     * @param value the value reference
     */
    public void accept(T value) {
        Objects.requireNonNull(value, "The value cannot be null");
        this.consumers.forEach(consumer -> consumer.accept(value));
    }

    /**
     * Attaches the consumer to this interest list
     * @param consumer  the consumer reference
     */
    public void attach(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "The consumer cannot be null");
        this.consumers.add(consumer);
    }

    /**
     * Detaches the consumer from this interest list
     * @param consumer  the consumer reference
     */
    public void detach(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "The consumer cannot be null");
        this.consumers.remove(consumer);
    }

}
