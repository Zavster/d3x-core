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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A class used to provide access to application modules based on type information.
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class Modules extends LifeCycle.Base {

    private Map<Class<?>,Object> moduleMap = new LinkedHashMap<>();

    /**
     * Constructor
     */
    private Modules() {
        super();
    }


    /**
     * Returns a newly created suite of modules configure as per arg
     * @param configure the function to configure the modules
     * @return          the newly created runtime modules
     */
    public static Modules of(Consumer<Modules> configure) {
        final Modules modules = new Modules();
        configure.accept(modules);
        return modules;
    }


    @Override
    protected void doStart() throws RuntimeException {
        final long t1 = System.currentTimeMillis();
        final List<Object> modules = list();
        final long t2 = System.currentTimeMillis();
        log.info("Started " + modules.size() + " modules in " + (t2-t1) + " millis");
    }


    @Override
    protected void doStop() throws RuntimeException {
        final long t1 = System.currentTimeMillis();
        final List<Object> modules = list();
        modules.stream().filter(m -> m instanceof LifeCycle).forEach(m -> ((LifeCycle)m).stop());
        final long t2 = System.currentTimeMillis();
        log.info("Stopped " + modules.size() + " modules in " + (t2-t1) + " millis");
    }


    /**
     * Returns a list of all the registered modules
     * @return  the list of all registered modules
     */
    public synchronized List<Object> list() {
        return new ArrayList<>(moduleMap.keySet()).stream().map(this::getOrFail).collect(Collectors.toList());
    }


    /**
     * Returns a list of all the registered modules
     * @param predicate the predicate to filter on
     * @return  the list of all registered modules
     */
    public synchronized List<Object> list(Predicate<Object> predicate) {
        return new ArrayList<>(moduleMap.keySet()).stream().map(this::getOrFail).filter(predicate).collect(Collectors.toList());
    }


    /**
     * Returns a reference to an Option on the module instance of the type specified
     * @param type      the module type
     * @param <M>       the type
     * @return          the module instance Option
     */
    public synchronized final <M> Option<M> get(Class<M> type) {
        return Option.of(lookup(type, false));
    }


    /**
     * Returns a reference to the module instance of the type specified or throws an exception if non existent
     * @param type      the module type
     * @param <M>       the type
     * @return          the module instance
     * @throws RuntimeException if no module exists, or fails to initialize
     */
    public synchronized final <M> M getOrFail(Class<M> type) {
        return lookup(type, true);
    }


    /**
     * Returns a reference to the module instance of the type specified
     * @param type      the module type
     * @param fail      if true, throw exception of non-existent
     * @param <M>       the type
     * @return          the module instance
     * @throws RuntimeException if no module exists, or fails to initialize
     */
    @SuppressWarnings("unchecked")
    private synchronized <M> M lookup(Class<M> type, boolean fail) {
        Objects.requireNonNull(type, "The module type cannot be null");
        Object module = moduleMap.get(type);
        if (module == null && fail) {
            throw new RuntimeException("No Module registered for type: " + type);
        } else if (module instanceof Supplier) {
            log.info("Initializing module of type: " + type.getSimpleName());
            module = ((Supplier<?>)module).get();
            this.moduleMap.put(type, module);
            if (module instanceof LifeCycle) {
                final long t1 = System.currentTimeMillis();
                ((LifeCycle)module).start();
                final long t2 = System.currentTimeMillis();
                log.info("Started Module of type " + module.getClass().getSimpleName() + " in " + (t2-t1) + " millis");
            }
        }
        return (M)module;
    }



    /**
     * Registers a module supplier against the type specified
     * @param type      the module type
     * @param supplier  the module supplier
     * @param <M>       the type
     */
    public synchronized <M> void register(Class<M> type, Supplier<M> supplier) {
        Objects.requireNonNull(type, "The module type cannot be null");
        Objects.requireNonNull(supplier, "The module supplier cannot be null");
        this.moduleMap.put(type, supplier);
    }


    /**
     * Puts a module instance directly into these modules
     * @param type      the module type
     * @param module    the module instance
     * @param <M>       the type
     */
    public synchronized <M> void put(Class<M> type, M module) {
        Objects.requireNonNull(type, "The module type cannot be null");
        Objects.requireNonNull(module, "The module instance cannot be null");
        this.moduleMap.put(type, module);
    }

}
