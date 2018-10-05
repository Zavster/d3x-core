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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Capture the scheduling data for a task in the form of a CRON like expression
 *
 * <hours> <dayOfMonth> <month> <year>
 * <minutes> <hours> <dayOfMonth> <month> <year>
 * <seconds> <minutes> <hours> <dayOfMonth> <month> <year>
 * <seconds> <minutes> <hours> <dayOfWeek> <dayOfMonth> <month> <year>
 *
 * @author Xavier Witdouck
 */
@lombok.Builder()
@lombok.AllArgsConstructor()
public class Cron {

    /** The second specification for cron */
    @lombok.NonNull private CronEntry seconds;
    /** The minute specification for cron */
    @lombok.NonNull private CronEntry minutes;
    /** The hour specification for cron */
    @lombok.NonNull private CronEntry hours;
    /** The day of month specification for cron */
    @lombok.NonNull private CronEntry daysOfMonth;
    /** The day of week specification for cron */
    @lombok.NonNull private CronEntry daysOfWeek;
    /** The month specification for cron */
    @lombok.NonNull private CronEntry months;
    /** The years specification for cron */
    @lombok.NonNull private CronEntry years;


    /**
     * Returns a Cron instance parsed from the expression provided
     * @param expression    the cron expression in a supported format
     * @return              the resulting cron instance
     */
    public static Cron parse(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("The cron expression cannot be null");
        } else {
            final List<String> tokens = expand(expression);
            return Cron.builder()
                    .seconds(CronEntry.parseSeconds(tokens.get(0)))
                    .minutes(CronEntry.parseMinutes(tokens.get(1)))
                    .hours(CronEntry.parseHours(tokens.get(2)))
                    .daysOfWeek(CronEntry.parseDaysOfWeek(tokens.get(3)))
                    .daysOfMonth(CronEntry.parseDaysOfMonth(tokens.get(4)))
                    .months(CronEntry.parseMonths(tokens.get(5)))
                    .years(CronEntry.parseYears(tokens.get(6)))
                    .build();
        }
    }


    /**
     * Expands the expression into the 7 required tokens, defaulting missing values
     * @param expression    the expression to parse
     * @return              the individial cron entries
     */
    private static List<String> expand(String expression) {
        final String[] tokens = expression.split("\\s+");
        if (tokens.length == 7) {
            return Arrays.asList(tokens);
        } else if (tokens.length == 4) {
            return List.of("0", "0", tokens[0], "*", tokens[1], tokens[2], tokens[3]);
        } else if (tokens.length == 5) {
            return List.of("0", tokens[0], tokens[1], "*", tokens[2], tokens[3], tokens[4]);
        } else if (tokens.length == 6) {
            return List.of(tokens[0], tokens[1], tokens[2], "*", tokens[3], tokens[4], tokens[5]);
        } else {
            throw new RuntimeException("Malformed cron expression, cannot parse: " + expression);
        }
    }


    /**
     * Returns an stream of UTC times for this cron definition
     * @param min   the lower bound for values in iterator
     * @param zone  the time zone to use to evaluate this cron
     * @return      the stream of UTC times
     */
    public Stream<Long> getTimes(long min, ZoneId zone) {
        final TimeZone timeZone = TimeZone.getTimeZone(zone.getId());
        final Calendar calendar = Calendar.getInstance(timeZone);
        final Iterator<Long> iterator  = new CronIterator(calendar, min);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }


    /**
     * Returns an stream of local dates for this cron definition
     * @param min   the lower bound for values in iterator
     * @param zone  the time zone to use to evaluate this cron
     * @return      the stream of local dates
     */
    public Stream<LocalDate> getLocalDates(LocalDate min, ZoneId zone) {
        final long millis = ZonedDateTime.of(min, LocalTime.MIDNIGHT, zone).toInstant().toEpochMilli();
        return getTimes(millis, zone).map(Instant::ofEpochMilli).map(v -> LocalDateTime.ofInstant(v, zone).toLocalDate()).distinct();
    }


    /**
     * Returns an stream of local date times for this cron definition
     * @param min   the lower bound for values in iterator
     * @param zone  the time zone to use to evaluate this cron
     * @return      the stream of local date times
     */
    public Stream<LocalDateTime> getLocalDateTimes(LocalDateTime min, ZoneId zone) {
        final long millis = min.atZone(zone).toInstant().toEpochMilli();
        return getTimes(millis, zone).map(Instant::ofEpochMilli).map(v -> LocalDateTime.ofInstant(v, zone));
    }


    /**
     * Returns an stream of zoned date times for this cron definition
     * @param min   the lower bound for values in iterator
     * @return      the stream of local date times
     */
    public Stream<ZonedDateTime> getZonedDateTimes(ZonedDateTime min) {
        final ZoneId zone = min.getZone();
        final long millis = min.toInstant().toEpochMilli();
        return getTimes(millis, zone).map(Instant::ofEpochMilli).map(v -> ZonedDateTime.ofInstant(v, zone));
    }


    /**
     * Returns the java.util.Calendar day of week given the cron day of the week
     * @param dayOfWeek     the cron day of week
     * @return              the calendar day of week
     */
    private int getCronDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:       return 0;
            case Calendar.MONDAY:       return 1;
            case Calendar.TUESDAY:      return 2;
            case Calendar.WEDNESDAY:    return 3;
            case Calendar.THURSDAY:     return 4;
            case Calendar.FRIDAY:       return 5;
            case Calendar.SATURDAY:     return 6;
            default: throw new IllegalArgumentException("Day of week out of range: " + dayOfWeek);
        }
    }



    /**
     * An iterator of time values
     */
    private class CronIterator implements Iterator<Long> {

        private Long next;
        private Calendar calendar;
        private Iterator<Integer> iteratorYears;
        private Iterator<Integer> iteratorMonths;
        private Iterator<Integer> iteratorDoM;
        private Iterator<Integer> iteratorHours;
        private Iterator<Integer> iteratorMinutes;
        private Iterator<Integer> iteratorSeconds;


        /**
         * Constructor
         * @param calendar  the calendar reference
         * @param start     the optional start for iterator
         */
        CronIterator(Calendar calendar, long start) {
            this.calendar = calendar;
            this.calendar.setTimeInMillis(start);
            this.iteratorYears = years.values().iterator();
            this.iteratorMonths = months.values().iterator();
            this.iteratorDoM = daysOfMonth();
            this.iteratorHours = hours.values().iterator();
            this.iteratorMinutes = minutes.values().iterator();
            this.iteratorSeconds = seconds.values().iterator();
            this.calendar.set(Calendar.MILLISECOND, 0);
            this.findStart(iteratorSeconds, Calendar.SECOND);
            this.findStart(iteratorMinutes, Calendar.MINUTE);
            this.findStart(iteratorHours, Calendar.HOUR_OF_DAY);
            this.findStart(iteratorDoM, Calendar.DAY_OF_MONTH);
            this.findStart(iteratorMonths, Calendar.MONTH);
            this.findStart(iteratorYears, Calendar.YEAR);
            final long time = calendar.getTimeInMillis();
            if (time < start) {
                this.next = findNext();
            } else {
                this.next = accept() ? Long.valueOf(calendar.getTimeInMillis()) : findNext();
            }
        }


        /**
         * Finds the starting point in the iterator based on start time
         * @param iterator  the iterator in which to find start value
         * @param field     the calendar field to operate on
         */
        private void findStart(Iterator<Integer> iterator, int field) {
            final int adj = field == Calendar.MONTH ? -1 : 0;
            final int value = calendar.get(field) - adj;
            while (iterator.hasNext()) {
                final int next = iterator.next();
                this.calendar.set(field, next + adj);
                if (next >= value) {
                    break;
                }
            }
        }


        /**
         * Returns the day of month iterator cognizant of the days in the current month
         * @return      the day of month iterator for current month
         */
        private Iterator<Integer> daysOfMonth() {
            final int max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            return daysOfMonth.values().filter(v -> v <= max).iterator();
        }


        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            } else {
                this.next = findNext();
                return next != null;
            }
        }


        @Override()
        public Long next() {
            if (next != null) {
                final Long value = next;
                this.next = null;
                return value;
            } else if (hasNext()) {
                return next();
            } else {
                throw new IllegalStateException("No more times in cron iterator");
            }
        }


        /**
         * True if the current date in the calendar is a valid value for iterator
         * @return      true if current value is acceptable for iterator
         */
        private boolean accept() {
            final int dom = calendar.get(Calendar.DAY_OF_MONTH);
            final int dow = calendar.get(Calendar.DAY_OF_WEEK);
            final int dowCron = getCronDayOfWeek(dow);
            return daysOfWeek.contains(dowCron) && daysOfMonth.contains(dom);
        }


        /**
         * Finds the next value acceptable to this iterator
         * @return  the next value, null if no more values
         */
        private Long findNext() {
            while (true) {
                final Long value = nextRaw();
                if (value == null) {
                    return null;
                } else if (accept()) {
                    return value;
                }
            }
        }


        /**
         * Returns the next UTC time without checking the DoW predicate
         * @return      the next value, which may violate the DoW predicate
         */
        private Long nextRaw() {
            if (iteratorSeconds.hasNext()) {
                final int second = iteratorSeconds.next();
                this.calendar.set(Calendar.SECOND, second);
                return calendar.getTimeInMillis();
            } else if (iteratorMinutes.hasNext()) {
                final int minute = iteratorMinutes.next();
                this.iteratorSeconds = seconds.values().iterator();
                this.calendar.set(Calendar.SECOND, iteratorSeconds.next());
                this.calendar.set(Calendar.MINUTE, minute);
                return calendar.getTimeInMillis();
            } else if (iteratorHours.hasNext()) {
                final int hour = iteratorHours.next();
                this.iteratorSeconds = seconds.values().iterator();
                this.iteratorMinutes = minutes.values().iterator();
                this.calendar.set(Calendar.SECOND, iteratorSeconds.next());
                this.calendar.set(Calendar.MINUTE, iteratorMinutes.next());
                this.calendar.set(Calendar.HOUR_OF_DAY, hour);
                return calendar.getTimeInMillis();
            } else if (iteratorDoM.hasNext()) {
                final int dom = iteratorDoM.next();
                this.calendar.set(Calendar.DAY_OF_MONTH, dom);
                this.iteratorSeconds = seconds.values().iterator();
                this.iteratorMinutes = minutes.values().iterator();
                this.iteratorHours = hours.values().iterator();
                this.calendar.set(Calendar.SECOND, iteratorSeconds.next());
                this.calendar.set(Calendar.MINUTE, iteratorMinutes.next());
                this.calendar.set(Calendar.HOUR_OF_DAY, iteratorHours.next());
                this.calendar.set(Calendar.DAY_OF_MONTH, dom);
                return calendar.getTimeInMillis();
            } else if (iteratorMonths.hasNext()) {
                final int month = iteratorMonths.next();
                this.calendar.set(Calendar.MONTH, month-1);
                this.iteratorDoM = daysOfMonth();
                this.iteratorSeconds = seconds.values().iterator();
                this.iteratorMinutes = minutes.values().iterator();
                this.iteratorHours = hours.values().iterator();
                this.calendar.set(Calendar.SECOND, iteratorSeconds.next());
                this.calendar.set(Calendar.MINUTE, iteratorMinutes.next());
                this.calendar.set(Calendar.HOUR_OF_DAY, iteratorHours.next());
                this.calendar.set(Calendar.DAY_OF_MONTH, iteratorDoM.next());
                return calendar.getTimeInMillis();
            } else if (iteratorYears.hasNext()) {
                final int year = iteratorYears.next();
                this.iteratorSeconds = seconds.values().iterator();
                this.iteratorMinutes = minutes.values().iterator();
                this.iteratorHours = hours.values().iterator();
                this.iteratorDoM = daysOfMonth.values().iterator();
                this.iteratorMonths = months.values().iterator();
                this.calendar.set(Calendar.SECOND, iteratorSeconds.next());
                this.calendar.set(Calendar.MINUTE, iteratorMinutes.next());
                this.calendar.set(Calendar.HOUR_OF_DAY, iteratorHours.next());
                this.calendar.set(Calendar.DAY_OF_MONTH, iteratorDoM.next());
                this.calendar.set(Calendar.MONTH, iteratorMonths.next()-1);
                this.calendar.set(Calendar.YEAR, year);
                return calendar.getTimeInMillis();
            } else {
                return null;
            }
        }
    }


    public static void main(String[] args) {
        final Cron cron = Cron.parse("0 0 15 31 * 2018");
        final long t1 = System.currentTimeMillis();
        final ZoneId zoneId = ZoneId.of("GMT");
        final Stream<LocalDateTime> times = cron.getLocalDateTimes(LocalDateTime.now(), zoneId);
        final long t2 = System.currentTimeMillis();
        System.out.println("Time to calculate schedule: " + (t2-t1) + " millis");
        times.forEach(v -> IO.println(v + " on " + v.getDayOfWeek()));
    }

}
