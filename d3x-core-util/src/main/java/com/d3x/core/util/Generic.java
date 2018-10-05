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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * A class used to manufacture types to represent Generic types
 *
 * @author Xavier Witdouck
 */
public class Generic {


    /**
     * Returns a ParameterizedType of a List of type specified
     * @param typeArg   the parameter type for list
     * @return              the newly created ParameterizedType
     */
    public static Type ofList(Type typeArg) {
        return of(List.class, typeArg);
    }

    /**
     * Returns a ParameterizedType for the args provided
     * @param rawType       the raw type
     * @param typeArg       the type arg
     * @return              the newly created ParameterizedType
     */
    public static Type of(Type rawType, Type typeArg) {
        return new Generic1(rawType, typeArg);
    }


    private static class Generic1 implements ParameterizedType {

        private Type rawType;
        private Type typeArg;

        private Generic1(Type rawType, Type typeArg) {
            this.rawType = rawType;
            this.typeArg = typeArg;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] { typeArg };
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

    }
}
