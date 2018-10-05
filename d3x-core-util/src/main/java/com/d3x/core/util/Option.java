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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.EqualsAndHashCode;

/**
 * A class designed to operate like Scala's Option trait
 *
 * @param <T>   the type for Option
 *
 * @author Xavier Witdouck
 */
@EqualsAndHashCode(of={"value"})
public final class Option<T> implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final Option EMPTY = new Option<>(null);

    private T value;

    /**
     * Constructor
     * @param value the value for option
     */
    private Option(T value) {
        this.value = value;
    }

    /**
     * Returns an empty Option
     * @param <T>   the type for Option
     * @return      the empty Option
     */
    @SuppressWarnings("unchecked")
    public static <T> Option<T> empty() {
        return (Option<T>)EMPTY;
    }

    /**
     * Returns an Option on the value specified
     * @param value     the value to wrap in an Option (null allowed)
     * @param <T>       the type for Option
     * @return          the Option instance
     */
    public static <T> Option<T> of(T value) {
        return value == null ? empty() : new Option<>(value);
    }


    /**
     * Returns the value if not null, otherwise the fallback value
     * @param value         the value to check for null
     * @param fallback      the fallback value
     * @param <T>           the type for value
     * @return              the value or fallback if value is null
     */
    public static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }

    /**
     * Returns the value for this option, null if isEmpty()
     * @return  the value for this options, null if isEmpty()
     */
    public final T get() {
        if (value == null) {
            throw new NoSuchElementException("Option is empty");
        } else {
            return value;
        }
    }

    /**
     * Returns true if this option does not contain a value
     * @return      true if does not contain a value
     */
    public final boolean isEmpty() {
        return value == null;
    }

    /**
     * Returns true if this option contains a value
     * @return  true if contains a value
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Maps the optional value using the function provided
     * @param mapper    the mapper function
     * @param <R>       the return type
     * @return          the option on return
     */
    public final <R> Option<R> map(Function<T,R> mapper) {
        return isPresent() ? Option.of(mapper.apply(get())) : Option.empty();
    }

    /**
     * Returns the value in this Option if present, otherwise null
     * @return          the value for Option, or null if is empty
     */
    public final T orNull() {
        return isPresent() ? get() : null;
    }

    /**
     * Returns the value in this Option if present, otherwise the arg provided
     * @param value     the value to return if Option is empty
     * @return          the value for Option, or arg if is empty
     */
    public final T orElse(T value) {
        return isPresent() ? get() : value;
    }

    /**
     * Returns the value in this Option if present, otherwise the supplier result provided
     * @param supplier  the supplier to return result if Option is empty
     * @return          the value for Option, or arg if is empty
     */
    public final T orElse(Supplier<T> supplier) {
        return isPresent() ? get() : supplier.get();
    }

    /**
     * Returns the value in this Option if present, otherwise throws a NoSuchElementException
     * @param msg   the message for NoSuchElementException
     * @return      the value for Option
     * @throws NoSuchElementException   if option is empty
     */
    public final T orThrow(String msg) {
        if (isPresent()) {
            return get();
        } else {
            throw new NoSuchElementException(msg);
        }
    }


    /**
     * Returns the option value if present, or throws the exception returned by the supplier
     * @param supplier  the exception supplier
     * @return          the option value
     * @throws RuntimeException  the resulting exception if option is empty
     */
    public final T orThrow(Supplier<? extends RuntimeException> supplier) throws RuntimeException {
        if (isPresent()) {
            return get();
        } else {
            throw supplier.get();
        }
    }

    /**
     * Applies the value in this option to the consumer if it is present
     * @param consumer  the consumer to apply value to if present
     */
    public final void ifPresent(Consumer<T> consumer) {
        if (isPresent()) {
            consumer.accept(get());
        }
    }

    /**
     * Executes the provided runnable if this option is empty
     * @param runnable  the runnable to execute if empty
     */
    public final void ifEmpty(Runnable runnable) {
        if (isEmpty()) {
            runnable.run();
        }
    }

    @Override
    public String toString() {
        return isEmpty() ? "Option.empty()" : "Option.of(" + value + ")";
    }
}
