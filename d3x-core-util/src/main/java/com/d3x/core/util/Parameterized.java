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

import java.util.function.Function;

/**
 * An interface to a object that can be parameterized in some way
 *
 * @param <T>   true if an object is parameterized
 */
public interface Parameterized<T> {

    /**
     * Returns a deep copy of this object with any parameters replaced by the function specified
     * @param parameters    the function that resolves parameters into values
     * @return              a deep copy of this object with any parameters replaced
     */
    T replaceParams(Function<String,String> parameters);

}
