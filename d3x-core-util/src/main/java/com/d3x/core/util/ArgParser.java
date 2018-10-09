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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple command line argument parser with some convenience functions to parse strings into typed values
 *
 * @author Xavier Witdouck
 */
public class ArgParser {

    private List<String> args;

    /**
     * Constructor
     * @param args  the args to parse
     */
    public ArgParser(String... args) {
        this(Arrays.asList(args));
    }


    /**
     * Constructor
     * @param args  the args to parse
     */
    public ArgParser(List<String> args) {
        this.args = args;
    }


    /**
     * Returns a newly created arg parser
     * @param args  the args to parse
     * @return      the arg parser
     */
    public static ArgParser of(String... args) {
        return new ArgParser(args);
    }


    /**
     * The list of args bound to this parser
     * @return  the list of args
     */
    public List<String> getArgs() {
        return Collections.unmodifiableList(args);
    }


    /**
     * Returns true if the specified arg exists
     * @param arg   the arg to test
     * @return      true if exists
     */
    public boolean contains(String arg) {
        return args.contains(arg);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<Integer> getInt(String option) {
        return getValue(option).map(Integer::parseInt);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<Long> getLong(String option) {
        return getValue(option).map(Long::parseLong);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<Double> getDouble(String option) {
        return getValue(option).map(Double::parseDouble);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<Duration> getDuration(String option) {
        return getValue(option).map(Long::parseLong).map(Duration::ofMillis);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<LocalDate> getLocalDate(String option) {
        return getValue(option).map(LocalDate::parse);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<LocalTime> getLocalTime(String option) {
        return getValue(option).map(LocalTime::parse);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<File> getFile(String option) {
        return getValue(option).map(File::new);
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<URL> getUrl(String option) throws MalformedURLException {
        final Option<String> value = getValue(option);
        return value.isEmpty() ? Option.empty() : Option.of(new URL(value.get()));
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<List<String>> getList(String option) {
        final Option<String> value = getValue(option);
        if (value.isEmpty()) {
            return Option.empty();
        } else {
            final String[] tokens = value.get().split(",");
            return Option.of(Stream.of(tokens).map(String::trim).collect(Collectors.toList()));
        }
    }


    /**
     * Returns the option value parsed from command line args
     * @param option        the option to search for
     * @return              the option value, which could be default
     */
    public Option<String> getValue(String option) {
        for (int i=0; i<args.size(); ++i) {
            final String arg = args.get(i);
            if (arg.equalsIgnoreCase(option)) {
                final String value = args.size() > i+1 ? args.get(i+1) : null;
                if (value != null && value.trim().length() > 0) {
                    return Option.of(value);
                } else {
                    final String msg = "Arg %s must be followed by valid value";
                    throw new IllegalArgumentException(String.format(msg, option));
                }
            }
        }
        return Option.empty();
    }

}
