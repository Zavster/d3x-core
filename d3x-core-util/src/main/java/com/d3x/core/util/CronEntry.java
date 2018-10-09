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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A class that represents an entry within a cron expression
 *
 * @author Xavier Witdouck
 */
public abstract class CronEntry  {

    private static final Map<String,Integer> monthMap = new HashMap<>();
    private static final Map<String,Integer> dayOfWeekMap = new HashMap<>();


    enum Type {

        SECONDS(0, 59),
        MINUTES(0, 59),
        HOURS(0, 23),
        MONTH(1, 12),
        YEARS(1900, 3000),
        DOM(1, 31),
        DOW(1, 7);

        private int min;
        private int max;

        /**
         * Construcator
         * @param min   the min allowed value
         * @param max   the max allow value
         */
        Type(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }


    private Type type;


    /*
     * Static initializer
     */
    static {
        monthMap.put("jan", 1);
        monthMap.put("feb", 2);
        monthMap.put("mar", 3);
        monthMap.put("apr", 4);
        monthMap.put("may", 5);
        monthMap.put("jun", 6);
        monthMap.put("jul", 7);
        monthMap.put("aug", 8);
        monthMap.put("sep", 9);
        monthMap.put("oct", 10);
        monthMap.put("nov", 11);
        monthMap.put("dec", 12);
    }

    /*
     * Static initializer
     */
    static {
        dayOfWeekMap.put("mon", 1);
        dayOfWeekMap.put("tue", 2);
        dayOfWeekMap.put("wed", 3);
        dayOfWeekMap.put("thu", 4);
        dayOfWeekMap.put("fri", 5);
        dayOfWeekMap.put("sat", 6);
        dayOfWeekMap.put("sun", 7);
    }


    /**
     * Constructor
     * @param type  the type for this entry
     */
    CronEntry(Type type) {
        this.type = type;
    }

    /**
     * Returns the type for this entry
     * @return  the type for this entry
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the string representation of this entry
     * @return  the string representation
     */
    public abstract String asString();

    /**
     * Returns a stream over the values for this entry
     * @return      the stream of values for this entry
     */
    public abstract IntStream values();

    /**
     * Returns true if this entry contains the value specified
     * @param value     the value to check
     * @return          true if value is contained
     */
    public abstract boolean contains(int value);


    /**
     * Returns a cron entry with the scalar value
     * @param type      the type for entry
     * @param value     the value for scalar
     * @return          the cron entry
     */
    public static CronEntry scalar(Type type, int value) {
        return new Scalar(type, value);
    }


    /**
     * Returns a cron entry for the closed range
     * @param type      the type for entry
     * @param start     the start value for range, inclusive
     * @param end       the end value for range, inclusive
     * @param step      the step value increment
     * @return          the cron entry
     */
    public static CronEntry range(Type type, int start, int end, int step) {
        return new Range(type, start, end, step);
    }


    /**
     * Returns a cron entry made up of one or more entries
     * @param type      the type for entry
     * @param entries   the entries for composite
     * @return          the newly created entry
     */
    public static CronEntry composite(Type type, Stream<CronEntry> entries) {
        return new Composite(type, entries.collect(Collectors.toList()));
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 0,2,3 or 0-10/2 or 0,2,3,5-10/2 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseSeconds(String token) {
        return parse(token, Type.SECONDS);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 0,2,3 or 0-10/2 or 0,2,3,5-10/2 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseMinutes(String token) {
        return parse(token, Type.MINUTES);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 0,2,3 or 0-10/2 or 0,2,3,5-10/2 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseHours(String token) {
        return parse(token, Type.HOURS);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 1,2,3 or 1-10/2 or 1,2,3,5-10/2 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseDaysOfMonth(String token) {
        return parse(token, Type.DOM);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 1,2,3 or 1-10/2 or 1,2,3,5-10/2 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseMonths(String token) {
        return parse(token, Type.MONTH);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 1,2,3 or 1-10/2 or 1,2,3,5-10/2 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseYears(String token) {
        return parse(token, Type.YEARS);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 2 or mon or mon,tue,wed or 0-6/2 or mon,tue,3-6/1 etc...)
     * @return      the cron entry
     */
    public static CronEntry parseDaysOfWeek(String token) {
        return parse(token, Type.DOW);
    }


    /**
     * Parses the entry within a cron expression into a CronEntry object
     * @param token the token to parse (eg 12 or 0,2,3 or 0-10/2 or 0,2,3,5-10/2 etc...)
     * @param type  the type for this token
     * @return      the cron entry
     */
    public static CronEntry parse(String token, Type type) {
        final Stream<String> stream = Arrays.stream(token.split(",")).map(String::trim).map(String::toLowerCase);
        final List<String> items = stream.collect(Collectors.toList());
        if (items.size() > 1) {
            final List<CronEntry> entries = items.stream().map(v -> parse(v, type)).collect(Collectors.toList());
            return new Composite(type, entries);
        } else {
            final String item = items.get(0);
            final Matcher matcher1 = Pattern.compile("(\\d+)").matcher("");
            final Matcher matcher2 = Pattern.compile("(\\d+)-(\\d+)").matcher("");
            final Matcher matcher3 = Pattern.compile("(\\d+)-(\\d+)/(\\d+)").matcher("");
            final Matcher matcher4 = Pattern.compile("(\\p{Alpha}{3})-(\\p{Alpha}{3})").matcher("");
            final Matcher matcher5 = Pattern.compile("(\\p{Alpha}{3})-(\\p{Alpha}{3})/(\\d+)").matcher("");
            if (token.equals("*")) {
                return new Range(type, type.min, type.max, 1);
            } else if (matcher1.reset(item).matches()) {
                final int value = Integer.parseInt(matcher1.group(1));
                return new Scalar(type, value);
            } else if (matcher2.reset(item).matches()) {
                final int start = Integer.parseInt(matcher2.group(1));
                final int end = Integer.parseInt(matcher2.group(2));
                return new Range(type, start, end, 1);
            } else if (matcher3.reset(item).matches()) {
                final int start = Integer.parseInt(matcher3.group(1));
                final int end = Integer.parseInt(matcher3.group(2));
                final int step = Integer.parseInt(matcher3.group(3));
                return new Range(type, start, end, step);
            } else if (matcher4.reset(item).matches()) {
                final int start = getCronIndex(matcher4.group(1));
                final int end = getCronIndex(matcher4.group(2));
                return new Range(type, start, end, 1);
            } else if (matcher5.reset(item).matches()) {
                final int start = getCronIndex(matcher5.group(1));
                final int end = getCronIndex(matcher5.group(2));
                final int step = Integer.parseInt(matcher5.group(3));
                return new Range(type, start, end, step);
            } else if (monthMap.containsKey(item) || dayOfWeekMap.containsKey(item)) {
                final int value = getCronIndex(item);
                return new Scalar(type, value);
            } else {
                throw new IllegalArgumentException("Malformed expression within cron expression: " +  token);
            }
        }
    }


    /**
     * Returns the day of week index for the day of week token
     * @param token     the day of week token (sun|mon|tue|wed|thu|fri|sat)
     * @return          the day of week index (0|1|2|3|4|5|6)
     */
    private static int getCronIndex(String token) {
        if (monthMap.containsKey(token.toLowerCase())) {
            return monthMap.get(token.toLowerCase());
        } else if (dayOfWeekMap.containsKey(token.toLowerCase())) {
            return dayOfWeekMap.get(token.toLowerCase());
        } else {
            throw new IllegalArgumentException("Unrecognized token specified in cron expression: " + token);
        }
    }



    /**
     * The simplest entry in a cron expression, representing a Scalar value
     */
    @lombok.ToString()
    @lombok.EqualsAndHashCode(callSuper = false)
    private static class Scalar extends CronEntry {

        private int value;

        /**
         * Constructor
         * @param type      the type for this entry
         * @param value     the values for this entry
         */
        private Scalar(Type type, int value) {
            super(type);
            this.value = value;
            if (value < type.min) {
                throw new RuntimeException("Malformed cron value; " + value + " out of bounds for " + type.name());
            } else if (value > type.max) {
                throw new RuntimeException("Malformed cron value; " + value + " out of bounds for " + type.name());
            }
        }

        @Override
        public String asString() {
            return String.valueOf(value);
        }

        @Override()
        public IntStream values() {
            return IntStream.of(value);
        }

        @Override()
        public boolean contains(int value) {
            return this.value == value;
        }
    }


    /**
     * Represents an range definition within a Cron expression
     */
    @lombok.ToString()
    @lombok.EqualsAndHashCode(callSuper = false)
    private static class Range extends CronEntry {

        private int start;
        private int end;
        private int step;

        /**
         * Constructor
         * @param type  the type for this entry
         * @param start the start value
         * @param end   the end value
         * @param step  the step value
         */
        private Range(Type type, int start, int end, int step) {
            super(type);
            this.start = start;
            this.end = end;
            this.step = step;
            if (step <= 0) {
                throw new RuntimeException("The step value for range must be > 0");
            } else if (start > end) {
                throw new RuntimeException("Malformed cron entry; start must be <= end: " + start + " > " + end);
            } else if (start < type.min) {
                throw new RuntimeException("Malformed cron entry; " + start + " out of bounds for " + type.name());
            } else if (end > type.max) {
                throw new RuntimeException("Malformed cron entry; " + end + " out of bounds for " + type.name());
            }
        }

        @Override
        public String asString() {
            if (step != 1) {
                return String.format("%s-%s/%s", start, end, step);
            } else if (start != getType().min || end != getType().max) {
                return String.format("%s-%s", start, end);
            } else {
                return "*";
            }
        }

        @Override()
        public IntStream values() {
            return IntStream.iterate(start, v -> v <= end, v -> v + step);
        }


        @Override()
        public boolean contains(int value) {
            return value >= start && value <= end && value % step == 0;
        }
    }


    /**
     * Represents a composite of 2 or more CronEntries
     */
    @lombok.ToString()
    @lombok.EqualsAndHashCode(callSuper = false)
    private static class Composite extends CronEntry {

        private List<CronEntry> entries;

        /**
         * Constructor
         * @param entries   the entries for this composite
         */
        private Composite(Type type, List<CronEntry> entries) {
            super(type);
            this.entries = entries;
            if (!entries.stream().allMatch(v -> v.getType() == type)) {
                throw new IllegalArgumentException("Types for composite entry must match: " + type);
            }
        }

        @Override
        public String asString() {
            return String.join(",", entries.stream().map(CronEntry::asString).collect(Collectors.toList()));
        }

        @Override()
        public boolean contains(int value) {
            return entries.stream().anyMatch(v -> v.contains(value));
        }

        @Override()
        public IntStream values() {
            return entries.stream().map(CronEntry::values)
                    .reduce(IntStream::concat).map(IntStream::distinct)
                    .orElse(IntStream.empty()).sorted();
        }
    }

}
