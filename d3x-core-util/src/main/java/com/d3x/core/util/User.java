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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A class used to model a user in the system, with a thread local variable to manage per thread users in a multi user environment
 *
 * @author Xavier Witdouck
 */
@lombok.EqualsAndHashCode(of={"username"})
public class User {

    private static final Map<String,User> userMap = new HashMap<>();
    private static final User localUser = new User(System.getProperty("user.name"));
    private static final ThreadLocal<User> currentUser = ThreadLocal.withInitial(() -> localUser);
    private static final Function<String,User> userFactory = User::new;

    /** The user name */
    @lombok.Getter
    private String username;

    /**
     * Constructor
     * @param username  the username for this user
     */
    public User(String username) {
        this.username = username;
    }


    /**
     * Returns the user bound to this thread
     * @return  the user associated with current thread
     */
    public static User getCurrentUser() {
        final User user = currentUser.get();
        if (user == null) {
            throw new RuntimeException("No user associated with the current thread: " + Thread.currentThread().getName());
        } else {
            return user;
        }
    }


    /**
     * Sets the user for the current thread to null
     */
    public static void reset() {
        currentUser.set(null);
    }


    /**
     * Binds the user specified to the current thread
     * @param user      the user to bind to current thread
     */
    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }


    /**
     * Binds the user with username specified to the current thread
     * @param username      the username of user to bind to current thread
     */
    public static void setCurrentUser(String username) {
        currentUser.set(userMap.computeIfAbsent(username, userFactory));
    }


}
