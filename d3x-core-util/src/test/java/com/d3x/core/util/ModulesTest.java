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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the Modules class
 *
 * @author Xavier Witdouck
 */
public class ModulesTest {

    @Test()
    public void modules() {
        Modules modules = Modules.of(m -> {
            m.register(ServiceA.class, () -> new ServiceA(m));
            m.register(ServiceB.class, () -> new ServiceB(m));
        });
        modules.start();
        Assert.assertTrue(modules.getOrFail(ServiceA.class).isStarted());
        Assert.assertTrue(modules.getOrFail(ServiceB.class).isStarted());
        Assert.assertNotNull(modules.getOrFail(ServiceA.class).serviceB);
        Assert.assertNotNull(modules.getOrFail(ServiceB.class).serviceA);
    }


    @Test(expectedExceptions = {RuntimeException.class})
    public void fail() {
        Modules.of(IO::println).getOrFail(ServiceA.class);
    }


    @Test()
    public void empty() {
        Assert.assertTrue(Modules.of(IO::println).get(ServiceA.class).isEmpty());
    }


    @Test()
    public void replace() {
        Modules modules = Modules.of(m -> {
            m.register(ServiceA.class, () -> new ServiceA(m));
            m.register(ServiceB.class, () -> new ServiceB(m));
        });
        ServiceA initial = modules.getOrFail(ServiceA.class);
        modules.register(ServiceA.class, () -> new ServiceA(modules));
        ServiceA next = modules.getOrFail(ServiceA.class);
        Assert.assertNotEquals(initial, next);
    }



    public static class ServiceA extends LifeCycle.Base {

        private Modules modules;
        private ServiceB serviceB;

        ServiceA(Modules modules) {
            this.modules = modules;
        }

        @Override
        protected void doStart() throws RuntimeException {
            this.serviceB = modules.getOrFail(ServiceB.class);
        }
    }


    public static class ServiceB extends LifeCycle.Base {

        private Modules modules;
        private ServiceA serviceA;

        ServiceB(Modules modules) {
            this.modules = modules;
        }

        @Override
        protected void doStart() throws RuntimeException {
            this.serviceA = modules.getOrFail(ServiceA.class);
        }
    }

}
