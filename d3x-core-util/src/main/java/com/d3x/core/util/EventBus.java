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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class summary goes here...
 *
 * @author Xavier Witdouck
 */
public class EventBus {

    private Map<Class,EventListener[]> listenerArrays = new HashMap<>();
    private Map<Class,Set<EventListener>> listenerSets = new HashMap<>();

    /**
     * Constructor
     */
    public EventBus() {
        super();
    }

    /**
     * Publishes an event to all registered listeners for the event type
     * @param event     the event to publish, cannot be null
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        final Class eventType = event.getClass();
        final EventListener[] listeners = listenerArrays.get(eventType);
        if (listeners != null && listeners.length > 0) {
            for (EventListener listener : listeners) {
                try {
                    final long t1 = System.currentTimeMillis();
                    listener.onEvent(event);
                    final long t2 = System.currentTimeMillis();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    /**
     * Publishes a collection of events to all listeners of event type(s)
     * @param events    the collection of events
     */
    public <T> void publishAll(Collection<T> events) {
        for (T event : events) {
            this.publish(event);
        }
    }

    /**
     * Registers a listener with the event bus
     * @param eventType the event type
     * @param listener  the listener reference
     */
    public synchronized <T> void attach(Class<T> eventType, EventListener<T> listener) {
        if (eventType == null) {
            throw new IllegalArgumentException("The event type cannot be null");
        } else if (listener == null) {
            throw new IllegalArgumentException("The event listener cannot be null");
        } else {
            this.getEventListeners(eventType).add(listener);
            this.createArray(eventType);
        }
    }

    /**
     * Removes a listener from the event bus
     * @param eventType the event type
     * @param listener  the listener reference
     */
    public synchronized <T> void detach(Class<T> eventType, EventListener<T> listener) {
        if (eventType == null) {
            throw new IllegalArgumentException("The event type cannot be null");
        } else if (listener == null) {
            throw new IllegalArgumentException("The event listener cannot be null");
        } else {
            this.getEventListeners(eventType).remove(listener);
            this.createArray(eventType);
        }
    }

    /**
     * Returns the set of event listeners for the type specified
     * @param eventType the event type
     * @return          the set of event listeners
     */
    private synchronized Set<EventListener> getEventListeners(Class eventType) {
        Set<EventListener> listeners = listenerSets.get(eventType);
        if (listeners == null) {
            listeners = new LinkedHashSet<>();
            listenerSets.put(eventType, listeners);
        }
        return listeners;
    }

    /**
     * Generates a listener array for faster access
     * @param eventType the event type
     */
    private void createArray(Class eventType) {
        final Set<EventListener> listenerSet = getEventListeners(eventType);
        EventListener[] listeners = new EventListener[listenerSet.size()];
        listeners = listenerSet.toArray(listeners);
        listenerArrays.put(eventType, listeners);
    }


}
