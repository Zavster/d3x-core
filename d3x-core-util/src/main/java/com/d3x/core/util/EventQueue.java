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
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * A simple wrapper on a BlockingQueue that provides an API for consumers to process events in batches
 * @param <E>   the event type for this queue
 *
 * @author Xavier Witdouck
 */
@Slf4j
public class EventQueue<E> {

    private int batchSize;
    private final BlockingQueue<E> queue = new LinkedBlockingQueue<>();

    /**
     * Constructor
     * @param batchSize the max batch size for event processing
     */
    public EventQueue(int batchSize) {
        this.batchSize = batchSize;
    }


   /**
     * Enqueues an event with this dispatcher
     * @param event     the event object to enqueue
     */
    public void enqueue(E event) throws InterruptedException {
        this.queue.put(event);
    }


    /**
     * Enqueues multiple events with this dispatcher
     * @param events    the iterable of events to enqueue
     */
    public void enqueueAll(Iterable<E> events) throws InterruptedException {
        for (E event : events) {
            this.queue.put(event);
        }
    }


    /**
     * Returns the next batch of events that need to be processed
     * @return      the next batch of events to be processed
     */
    public List<E> nextBatch() {
        try {
            while (true) {
                final E head = queue.take();
                if (head != null) {
                    final List<E> eventList = new ArrayList<>(batchSize);
                    eventList.add(head);
                    this.queue.drainTo(eventList, batchSize-1);
                    return eventList;
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to drains events from EventQueue", ex);
        }
    }


    public static void main(String[] args) throws Exception {
        final Random random = new Random();
        final EventQueue<String> queue = new EventQueue<>(1000);
        final ExecutorService executor = Executors.newFixedThreadPool(10);

        //Simulate fast producer
        executor.execute(() -> {
            try {
                while (true) {
                    final int count = random.nextInt(10);
                    final Stream<String> messages = IntStream.range(0, count).mapToObj(i -> "Message-" + i);
                    queue.enqueueAll(messages.collect(Collectors.toList()));
                    Thread.sleep(random.nextInt(20));
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        //Simulate slow consumers
        executor.execute(() -> {
            try {
                while (true) {
                    final List<String> batch = queue.nextBatch();
                    System.out.println("Processed batch of size " + batch.size() + ", queue size is " + queue.queue.size());
                    Thread.sleep(1000);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });

        Thread.currentThread().join();

    }

}
