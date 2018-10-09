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

import java.util.function.Supplier;

/**
 * A class that wraps a Java 8 Supplier in order to define values that evaluate lazily.
 *
 * <p><strong>This is open source software released under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a></strong></p>
 *
 * @author  Xavier Witdouck
 */
public class Lazy<T> {

    private T value;
    private Supplier<T> supplier;
    private long timeNanos;

    /**
     * Constructor
     * @param supplier  the supplier for this lazy value
     */
    private Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
        this.timeNanos = -1;
    }

    /**
     * Returns a new lazy value over the provided supplier
     * @param supplier  the supplier
     * @param <T>       the value type
     * @return          the newly created lazy value
     */
    public static <T> Lazy<T> of(Supplier<T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("The Supplier cannot be null for a LazyValue");
        } else {
            return new Lazy<>(supplier);
        }
    }

    /**
     * Returns true if this lazy value has been evaluated
     * @return  true if lazy value has been evaluated
     */
    public synchronized boolean isReady() {
        return value != null;
    }

    /**
     * Returns the time in nanos it took this lazy value to evaluate
     * @return  the time in nanos it took to evaluate
     */
    public synchronized long getTimeNanos() {
        return timeNanos;
    }

    /**
     * Resets this lazy value so that it will evaluate again
     */
    public synchronized void reset() {
        this.value = null;
        this.timeNanos = -1;
    }

    /**
     * Returns the underlying value for this lazy value
     * @return      the underlying value for this lazy value
     */
    public synchronized T get() {
        if (value != null) {
            return value;
        } else {
            final long t1 = System.nanoTime();
            this.value = supplier.get();
            final long t2 = System.nanoTime();
            this.timeNanos = t2 - t1;
            return value;
        }
    }

    /**
     * Returns the value for this Lazy as an option
     * @return  the Option over this value
     */
    public Option<T> toOption() {
        return Option.of(this).map(Lazy::get);
    }
}
