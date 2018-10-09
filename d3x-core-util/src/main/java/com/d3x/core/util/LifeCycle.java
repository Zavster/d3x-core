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

/**
 * An interface to a component that has a controlled life-cycle that can be started and stopped
 *
 * @author Xavier Witdouck
 */
public interface LifeCycle {

    /**
     * Starts the module if it has not already started
     * This method should be idempotent
     * @throws RuntimeException  if fails to start
     */
    void start() throws RuntimeException;

    /**
     * Stops the module if it is running
     * This method should be idempotent
     * @throws RuntimeException  if fails to stop
     */
    void stop() throws RuntimeException;

    /**
     * Returns true if this component has been started
     * @return  true if component started
     */
    boolean isStarted();


    /**
     * Convenience method to start an array of LifeCycle objects
     * @param values    the values to start
     */
    static void start(LifeCycle... values) {
        for (LifeCycle value : values) {
            value.start();
        }
    }


    /**
     * Convenience method to start an array of LifeCycle objects
     * @param values    the values to start
     */
    static void stop(LifeCycle... values) {
        for (LifeCycle value : values) {
            value.stop();;
        }
    }



    /**
     * A convenience base class for implement components with a LifeCycle that ensures idempotent behaviour
     */
    abstract class Base implements LifeCycle {

        private boolean started;

        /**
         * Implement start logic for component
         * @throws RuntimeException if fails to start
         */
        protected abstract void doStart() throws RuntimeException;

        /**
         * Implement stop logic for a component by over-riding this method
         * @throws RuntimeException if fails to stop gracefully
         */
        protected void doStop() throws RuntimeException { }

        @Override()
        public synchronized boolean isStarted() {
            return started;
        }


        @Override
        public synchronized void start() throws RuntimeException {
            if (!started) {
                try {
                    this.doStart();
                    this.started = true;
                } catch (RuntimeException ex) {
                    this.doStop();
                    this.started = false;
                    throw ex;
                }
            }
        }

        @Override
        public synchronized void stop() throws RuntimeException {
            if (started) {
                try {
                    this.doStop();
                } finally {
                    this.started = false;
                }
            }
        }
    }
}
