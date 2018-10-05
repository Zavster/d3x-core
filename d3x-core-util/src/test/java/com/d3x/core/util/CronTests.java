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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the Cron class
 *
 * @author Xavier Witdouck
 */
public class CronTests {



    @Test()
    public void cronWithTwoTimes() {
        List<LocalDateTime> expected = List.of(
                LocalDateTime.of(2018, 12, 25, 12, 0),
                LocalDateTime.of(2018, 12, 25, 16, 0),
                LocalDateTime.of(2019, 12, 25, 12, 0),
                LocalDateTime.of(2019, 12, 25, 16, 0)
        );
        ZoneId zone = ZoneId.systemDefault();
        Cron cron = Cron.parse("0 0 12,16 25 dec 2018-2019");
        LocalDateTime start = expected.get(0);
        List<LocalDateTime> actual = cron.getLocalDateTimes(start, zone).collect(Collectors.toList());
        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void cronWithDateRange() {
        List<LocalDateTime> expected = List.of(
                LocalDateTime.of(2018, 12, 24, 12, 0),
                LocalDateTime.of(2018, 12, 24, 12, 15),
                LocalDateTime.of(2018, 12, 24, 12, 30),
                LocalDateTime.of(2018, 12, 24, 12, 45),
                LocalDateTime.of(2018, 12, 24, 16, 0),
                LocalDateTime.of(2018, 12, 24, 16, 15),
                LocalDateTime.of(2018, 12, 24, 16, 30),
                LocalDateTime.of(2018, 12, 24, 16, 45),
                LocalDateTime.of(2018, 12, 25, 12, 0),
                LocalDateTime.of(2018, 12, 25, 12, 15),
                LocalDateTime.of(2018, 12, 25, 12, 30),
                LocalDateTime.of(2018, 12, 25, 12, 45),
                LocalDateTime.of(2018, 12, 25, 16, 0),
                LocalDateTime.of(2018, 12, 25, 16, 15),
                LocalDateTime.of(2018, 12, 25, 16, 30),
                LocalDateTime.of(2018, 12, 25, 16, 45)
        );
        ZoneId zone = ZoneId.systemDefault();
        Cron cron = Cron.parse("0 0-59/15 12,16 24-25 dec 2018");
        LocalDateTime start = expected.get(0);
        List<LocalDateTime> actual = cron.getLocalDateTimes(start, zone).collect(Collectors.toList());
        actual.forEach(v -> IO.println(v + " on " + v.getDayOfWeek()));
        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void cronWithDayOfWeek() {
        List<LocalDateTime> expected = List.of(
                LocalDateTime.of(2018, 12, 24, 12, 0),
                LocalDateTime.of(2018, 12, 24, 12, 15),
                LocalDateTime.of(2018, 12, 24, 12, 30),
                LocalDateTime.of(2018, 12, 24, 12, 45),
                LocalDateTime.of(2018, 12, 24, 16, 0),
                LocalDateTime.of(2018, 12, 24, 16, 15),
                LocalDateTime.of(2018, 12, 24, 16, 30),
                LocalDateTime.of(2018, 12, 24, 16, 45)
        );
        ZoneId zone = ZoneId.systemDefault();
        Cron cron = Cron.parse("0 0-59/15 12,16 mon 24-25 dec 2018");
        LocalDateTime start = expected.get(0);
        List<LocalDateTime> actual = cron.getLocalDateTimes(start, zone).collect(Collectors.toList());
        Assert.assertTrue(actual.stream().allMatch(v -> v.getDayOfWeek() == DayOfWeek.MONDAY));
        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void cronWithMonthAndDayOfWeek() {
        List<LocalDateTime> expected = List.of(
                LocalDateTime.of(2018, 10, 3, 12, 15),
                LocalDateTime.of(2018, 10, 10, 12, 15),
                LocalDateTime.of(2018, 10, 17, 12, 15),
                LocalDateTime.of(2018, 10, 24, 12, 15),
                LocalDateTime.of(2018, 10, 31, 12, 15)
        );
        ZoneId zone = ZoneId.systemDefault();
        Cron cron = Cron.parse("0 15 12 wed * oct 2018");
        LocalDateTime start = expected.get(0);
        List<LocalDateTime> actual = cron.getLocalDateTimes(start, zone).collect(Collectors.toList());
        actual.forEach(v -> IO.println(v + " on " + v.getDayOfWeek()));
        Assert.assertTrue(actual.stream().allMatch(v -> v.getDayOfWeek() == DayOfWeek.WEDNESDAY));
        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void thirtyFirstOfEveryMonth() {
        List<LocalDateTime> expected = List.of(
                LocalDateTime.of(2018, 1, 31, 16, 15),
                LocalDateTime.of(2018, 3, 31, 16, 15),
                LocalDateTime.of(2018, 5, 31, 16, 15),
                LocalDateTime.of(2018, 7, 31, 16, 15),
                LocalDateTime.of(2018, 8, 31, 16, 15),
                LocalDateTime.of(2018, 10, 31, 16, 15),
                LocalDateTime.of(2018, 12, 31, 16, 15)
        );
        ZoneId zone = ZoneId.systemDefault();
        final Cron cron = Cron.parse("0 15 16 31 * 2018");
        LocalDateTime start = expected.get(0);
        List<LocalDateTime> actual = cron.getLocalDateTimes(start, zone).collect(Collectors.toList());
        actual.forEach(v -> IO.println(v + " on " + v.getDayOfWeek()));
        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void thirtyFirstOfEveryMonthOnlyWednesdays() {
        List<LocalDateTime> expected = List.of(
                LocalDateTime.of(2018, 1, 31, 16, 15),
                LocalDateTime.of(2018, 10, 31, 16, 15)
        );
        ZoneId zone = ZoneId.systemDefault();
        final Cron cron = Cron.parse("0 15 16 wed 31 * 2018");
        LocalDateTime start = expected.get(0);
        List<LocalDateTime> actual = cron.getLocalDateTimes(start, zone).collect(Collectors.toList());
        actual.forEach(v -> IO.println(v + " on " + v.getDayOfWeek()));
        Assert.assertTrue(actual.stream().allMatch(v -> v.getDayOfWeek() == DayOfWeek.WEDNESDAY));
        Assert.assertEquals(actual.size(), expected.size());
        Assert.assertTrue(actual.containsAll(expected));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void testTimeZone() {
        ZoneId gmt = ZoneId.of("GMT");
        ZoneId ldn = ZoneId.of("Europe/London");
        ZoneId nyc = ZoneId.of("America/New_York");
        LocalDateTime first = LocalDateTime.of(2018, 1, 1, 2, 15, 22);
        Cron cron = Cron.parse("22 15 2 * * 2018");
        Assert.assertEquals(cron.getLocalDates(first.toLocalDate(), ldn).iterator().next(), first.toLocalDate());
        Assert.assertEquals(cron.getLocalDates(first.toLocalDate(), nyc).iterator().next(), first.toLocalDate());
        Assert.assertEquals(cron.getLocalDateTimes(first, ldn).iterator().next(), first);
        Assert.assertEquals(cron.getZonedDateTimes(first.atZone(gmt)).iterator().next(), first.atZone(gmt));
        Assert.assertEquals(cron.getZonedDateTimes(first.atZone(nyc)).iterator().next(), first.atZone(nyc));
        Assert.assertEquals(cron.getZonedDateTimes(first.atZone(ldn)).iterator().next(), first.atZone(ldn));
    }


    @DataProvider(name="start-times")
    public Object[][] startTimes() {
        return new Object[][] {
                { "22 15 2 * 12 *", LocalDateTime.parse("2018-12-03T02:15:22"), LocalDateTime.parse("2018-12-03T02:15:22") },
                { "22 15 2 * 12 *", LocalDateTime.parse("2018-12-03T01:15:00"), LocalDateTime.parse("2018-12-03T02:15:22") },
                { "22 15 2 * 12 *", LocalDateTime.parse("2018-12-03T02:30:00"), LocalDateTime.parse("2018-12-04T02:15:22") },
                { "22 15 2 * 12 *", LocalDateTime.parse("2018-12-31T02:30:00"), LocalDateTime.parse("2019-12-01T02:15:22") },
                { "22 15 2 6,8,31 12 *", LocalDateTime.parse("2018-12-31T02:30:00"), LocalDateTime.parse("2019-12-06T02:15:22") },
        };
    }


    @Test(dataProvider = "start-times")
    public void testStartTime(String expression, LocalDateTime start, LocalDateTime expected) {
        Cron cron = Cron.parse(expression);
        LocalDateTime actual = cron.getLocalDateTimes(start, ZoneId.of("GMT")).iterator().next();
        Assert.assertEquals(actual, expected);
    }

}
