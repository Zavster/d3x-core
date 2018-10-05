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

import java.util.Arrays;
import java.util.Random;

/**
 * A class to wrap the details of a password
 *
 * @author Xavier Witdouck
 */
@lombok.EqualsAndHashCode()
public class Password implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private char[] value;

    /**
     * Constructor
     * @param value the password value
     */
    private Password(char[] value) {
        this.value = value;
    }

    /**
     * Returns a newly created password with the value specified
     * @param value     the password value
     * @return          the password
     */
    public static Password of(String value) {
        return new Password(value.toCharArray());
    }

    /**
     * Returns a newly created password with the value specified
     * @param value     the password value
     * @return          the password
     */
    public static Password of(char[] value) {
        return new Password(value);
    }

    /**
     * Returns the value for this password
     * @return  the value for password
     */
    public char[] getValue() {
        return value;
    }

    /**
     * Returns true if the password arg matches this password
     * @param password      the password to check against
     * @return          true if this password matches arg
     */
    public boolean matches(char[] password) {
        return this.value != null && password != null && Arrays.equals(this.value, password);
    }


    /**
     * Returns a newly created random password
     * @param length    the password length
     * @return          the newly created password
     */
    public static Password random(int length) {
        final String numbers = "0123456789";
        final String symbols = "!@#$%^&*_=+-/.?<>)";
        final String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String values = letters + symbols + numbers + letters.toLowerCase();
        final Random random = new Random();
        final char[] password = new char[length];
        for (int i = 0; i < length; i++) {
            password[i] = values.charAt(random.nextInt(values.length()));
        }
        return Password.of(password);
    }


    @Override
    public String toString() {
        return "************";
    }


    public static void main(String[] args) {
        System.out.println(new String(Password.random(20).getValue()));
    }



}

