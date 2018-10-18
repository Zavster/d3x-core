/*
 * Copyright 2018, D3X Systems - All Rights Reserved
 *
 * Licensed under a proprietary end-user agreement issued by D3X Systems.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.d3xsystems.com/terms/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.http.rest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;

import com.d3x.core.json.JsonWritable;
import com.d3x.core.util.Option;
import com.google.gson.stream.JsonWriter;

import com.d3x.core.util.IO;

/**
 * A component that collects and dispatches events as JSON messages to registered callers based on unique session ids
 *
 * @param <E>   the event type
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class RestEventDispatcher<E> {

    private String name;
    private Thread thread;
    private final Serializer<E> serializer;
    private final ReentrantLock sync = new ReentrantLock();
    private final Condition condition = sync.newCondition();
    private final AtomicBoolean started = new AtomicBoolean();
    private final Map<String,Long> lastPublishTimes = new HashMap<>();
    private final Map<String,Long> lastSubscribeTimes = new HashMap<>();
    private final Map<String,AsyncResponse> responseMap = new HashMap<>();
    private final Map<String,BlockingQueue<E>> queueMap = new HashMap<>();
    private final Map<String,Predicate<E>> predicateMap = new HashMap<>();


    /**
     * Constructor
     * @param name          the name for this event dispatcher
     * @param serializer    the function that serializes a stream of events to JSON
     */
    public RestEventDispatcher(String name, Serializer<E> serializer) {
        this.name = name;
        this.serializer = serializer;
    }


    /**
     * Returns a new RestEventDispatcher for the type specified
     * @param type      the type for dispatcher
     * @param debug     true to operate in debug mode
     * @param <T>       the data type
     * @return          the newly created dispatcher
     */
    public static <T extends JsonWritable> RestEventDispatcher<T> of(Class<T> type, boolean debug) {
        return new RestEventDispatcher<>(type.getSimpleName() + "Dispatcher", serializer(type, debug));
    }


    /**
     * Returns a function that can serialize an iterator of serializable objects to an output stream
     * @return      the serializer function
     */
    public static <T extends JsonWritable> Serializer<T> serializer(Class<T> type, boolean debug) {
        return (iterator, os) -> {
            JsonWriter writer = null;
            try {
                int count = 0;
                writer = new JsonWriter(new OutputStreamWriter(new BufferedOutputStream(os)));
                writer.beginArray();
                while (iterator.hasNext()) {
                    final T value = iterator.next();
                    if (debug) log.info("Serializing: " + value);
                    value.write(writer);
                    ++count;
                }
                writer.endArray();
                return count;
            } catch (Exception ex) {
                throw new RuntimeException("Failed to serialize objects to JSON for " + type.getSimpleName(), ex);
            } finally {
                IO.close(writer);
            }
        };
    }


    /**
     * Starts this event dispatcher
     * @return      this event dispatcher
     */
    public synchronized RestEventDispatcher<E> start() {
        if (thread == null) {
            log.info("Starting RestEventDispatcher named " + name);
            this.started.set(true);
            this.thread = new Thread(runnable(), "RestEventDispatcher-" + name);
            this.thread.setDaemon(true);
            this.thread.start();
        }
        return this;
    }


    /**
     * Stops this event dispatcher
     * @return      this event dispatcher
     */
    public synchronized RestEventDispatcher<E> stop() {
        try {
            if (thread != null) {
                log.info("Stopping RestEventDispatcher named " + name);
                this.started.set(false);
                this.thread.interrupt();
            }
            return this;
        } finally {
            this.started.set(false);
        }
    }


    /**
     * Returns true if this event dispatcher is started
     * @return  true if started
     */
    public boolean isStarted() {
        return started.get();
    }


    /**
     * Returns true if a filter exists for the session
     * @param sessionId     the unique session identifier
     * @return              true if a filter exists
     */
    public boolean isFiltered(String sessionId) {
        return predicateMap.get(sessionId) != null;
    }


    /**
     * Returns a Runnable for the publishing thread
     * @return  the runnablle for publishing thread
     */
    private Runnable runnable() {
        return () -> {
            while (isStarted()) {
                try {
                    //Blocks until events become available
                    final List<EventBatch> batches = this.events();
                    final long t1 = System.nanoTime();
                    batches.forEach(EventBatch::publish);
                    final double millis = (System.nanoTime() - t1) / 1000000d;
                    log.info("Published events to " + batches.size() + " open sessions in " + millis + " millis");
                } catch (Exception ex) {
                    log.error("Failed to publish events", ex);
                }
            }
        };
    }


    /**
     * Enqueues an event with this dispatcher
     * @param event     the event object to enqueue
     */
    public void enqueue(E event) {
        this.sync.lock();
        try {
            this.purgeStaleSessions();
            for (String sessionId : queueMap.keySet()) {
                final BlockingQueue<E> queue = queueMap.get(sessionId);
                if (queue != null) {
                    queue.put(event);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to enqueue event with dispatcher", ex);
        } finally {
            this.condition.signal();
            this.sync.unlock();
        }
    }


    /**
     * Enqueues multiple events with this dispatcher
     * @param events    the iterable of events to enqueue
     */
    public void enqueueAll(Iterable<E> events) {
        this.sync.lock();
        try {
            this.purgeStaleSessions();
            for (String sessionId : queueMap.keySet()) {
                final BlockingQueue<E> queue = queueMap.get(sessionId);
                if (queue != null) {
                    for (E event : events) {
                        queue.put(event);
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to enqueue event with dispatcher", ex);
        } finally {
            this.condition.signal();
            this.sync.unlock();
        }
    }


    /**
     * Sets or removes the event filter for the session id specified
     * @param sessionId     the session identifier for client
     * @param predicate     the predicate to filter events, null to remove an existing filter
     */
    public void setPredicate(String sessionId, Predicate<E> predicate) {
        Objects.requireNonNull(sessionId, "The session id cannot be null");
        this.sync.lock();
        try {
            this.predicateMap.put(sessionId, predicate);
            this.lastSubscribeTimes.put(sessionId, System.nanoTime());
        } finally {
            this.sync.unlock();
        }
    }


    /**
     * Subscribes to receive events via the response provided
     * @param sessionId     the session identifier for client
     * @param async         the async response
     */
    public void subscribe(String sessionId, AsyncResponse async) {
        this.subscribe(sessionId, async, Option.empty());
    }


    /**
     * Subscribes to receive events via the response provided
     * @param sessionId     the session identifier for client
     * @param response      the async response
     * @param predicate     the optional predicate to filter events
     */
    public void subscribe(String sessionId, AsyncResponse response, Option<Predicate<E>> predicate) {
        Objects.requireNonNull(sessionId, "The session id cannot be null");
        Objects.requireNonNull(response, "The AsyncResponse cannot be null");
        this.sync.lock();
        try {
            log.info("Polling events called for sessionId " + sessionId);
            this.responseMap.put(sessionId, response);
            this.lastSubscribeTimes.put(sessionId, System.nanoTime());
            predicate.ifPresent(p -> predicateMap.put(sessionId, p));
            if (!queueMap.containsKey(sessionId)) {
                this.queueMap.put(sessionId, new LinkedBlockingQueue<>());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register async response for session id: " + sessionId, ex);
        } finally {
            this.condition.signal();
            this.sync.unlock();
        }
    }


    /**
     * Purges any stale sessions that appear to be no longer listening
     */
    private void purgeStaleSessions() {
        try {
            final long now = System.nanoTime();
            final Set<String> sessionIds = new HashSet<>(queueMap.keySet());
            for (String sessionId : sessionIds) {
                final long lastSubscribe = lastSubscribeTimes.getOrDefault(sessionId, 0L);
                final long lastPublished = lastPublishTimes.getOrDefault(sessionId, 1L);
                final double ageMillis = (now - lastPublished) / 1000000d;
                if (lastSubscribe < lastPublished && ageMillis > 15000) {
                    log.info("Purging stale event session for " + sessionId);
                    this.queueMap.remove(sessionId);
                    this.lastSubscribeTimes.remove(sessionId);
                    this.lastPublishTimes.remove(sessionId);
                    this.predicateMap.remove(sessionId);
                    this.responseMap.remove(sessionId);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to purge stale sessions", ex);
        }
    }



    /**
     * Returns the list of events to publish, blocking until some are available
     * @return  the list of events to publish
     */
    private List<EventBatch> events() {
        this.sync.lock();
        try {
            final List<EventBatch> batchList = new ArrayList<>();
            while (true) {
                final long now = System.nanoTime();
                for (String sessionId : queueMap.keySet()) {
                    final Optional<EventBatch> option = batch(sessionId);
                    option.ifPresent(batch -> {
                        lastPublishTimes.put(batch.sessionId, now);
                        batchList.add(batch);
                    });
                }
                if (batchList.isEmpty()) {
                    this.condition.await();
                } else {
                    break;
                }
            }
            return batchList;
        } catch (Exception ex) {
            log.error("Failed to build event list to publish", ex);
            return Collections.emptyList();
        } finally {
            this.sync.unlock();
        }
    }


    /**
     * Returns a optional batch of events to publish for the session id specified
     * @param sessionId     the session identifier for consumer
     * @return              the optional event batch
     */
    private Optional<EventBatch> batch(String sessionId) {
        final BlockingQueue<E> queue = queueMap.get(sessionId);
        if (queue != null && !queue.isEmpty()) {
            final AsyncResponse response = responseMap.get(sessionId);
            if (response != null) {
                final List<E> events = new ArrayList<>();
                final int count = queue.drainTo(events);
                if (count > 0) {
                    final Predicate<E> predicate = predicateMap.get(sessionId);
                    final Iterator<E> iterator = predicate != null ? events.stream().filter(predicate).iterator() : events.iterator();
                    if (iterator.hasNext()) {
                        this.responseMap.remove(sessionId);
                        return Optional.of(new EventBatch(sessionId, iterator, response));
                    }
                }
            }
        }
        return Optional.empty();
    }


    /**
     * A class to capture a batch of events and response to publish them to
     */
    private class EventBatch {

        private String sessionId;
        private Iterator<E> events;
        private AsyncResponse async;

        /**
         * Constructor
         * @param sessionId the session id
         * @param events    the events to publish
         * @param response  the response to send the events to
         */
        EventBatch(String sessionId, Iterator<E> events, AsyncResponse response) {
            this.sessionId = sessionId;
            this.async = response;
            this.events = events;
        }

        /**
         * Publishes the list of events to the receiver
         */
        public void publish() {
            try {
                final long t1 = System.currentTimeMillis();
                this.async.resume(RestApp.stream(MediaType.APPLICATION_JSON_TYPE, os -> {
                    try {
                        final int count = serializer.write(events, os);
                        final long t2 = System.currentTimeMillis();
                        log.info("Published " + count + " events in " + (t2-t1) + " millis");
                    } catch (Exception ex) {
                        log.error("Failed to serialize events for session: " + this.sessionId, ex);
                    }
                }));
            } catch (Exception ex) {
                log.error("Failed to send events for session: " + this.sessionId, ex);
            }
        }
    }


    /**
     * A Serializer that can efficiently write objects to an output stream
     * @param <E>   the element type for iterator
     */
    public interface Serializer<E> {

        /**
         * Writes the items in the Iterator to the output stream specified
         * @param items     the items to write
         * @param os        the output stream reference
         * @return          the number of items written
         */
        int write(Iterator<E> items, OutputStream os) throws IOException;
    }

}
