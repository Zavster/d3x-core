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

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the CronEntry class
 *
 * @author Xavier Witdouck
 */
public class CronEntryTests {

    @Test()
    public void testScalarParse() {
        Assert.assertEquals(CronEntry.parseSeconds("25"), CronEntry.scalar(CronEntry.Type.SECONDS, 25));
        Assert.assertEquals(CronEntry.parseMinutes("25"), CronEntry.scalar(CronEntry.Type.MINUTES, 25));
        Assert.assertEquals(CronEntry.parseHours("22"), CronEntry.scalar(CronEntry.Type.HOURS, 22));
        Assert.assertEquals(CronEntry.parseDaysOfMonth("22"), CronEntry.scalar(CronEntry.Type.DOM, 22));
        Assert.assertEquals(CronEntry.parseMonths("3"), CronEntry.scalar(CronEntry.Type.MONTH, 3));
        Assert.assertEquals(CronEntry.parseMonths("mar"), CronEntry.scalar(CronEntry.Type.MONTH, 3));
        Assert.assertEquals(CronEntry.parseYears("2018"), CronEntry.scalar(CronEntry.Type.YEARS, 2018));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("sun"), CronEntry.scalar(CronEntry.Type.DOW, 0));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("mon"), CronEntry.scalar(CronEntry.Type.DOW, 1));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("tue"), CronEntry.scalar(CronEntry.Type.DOW, 2));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("wed"), CronEntry.scalar(CronEntry.Type.DOW, 3));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("thu"), CronEntry.scalar(CronEntry.Type.DOW, 4));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("fri"), CronEntry.scalar(CronEntry.Type.DOW, 5));
        Assert.assertEquals(CronEntry.parseDaysOfWeek("sat"), CronEntry.scalar(CronEntry.Type.DOW, 6));
        Assert.assertEquals(CronEntry.parseMonths("jan"), CronEntry.scalar(CronEntry.Type.MONTH, 1));
        Assert.assertEquals(CronEntry.parseMonths("feb"), CronEntry.scalar(CronEntry.Type.MONTH, 2));
        Assert.assertEquals(CronEntry.parseMonths("mar"), CronEntry.scalar(CronEntry.Type.MONTH, 3));
        Assert.assertEquals(CronEntry.parseMonths("apr"), CronEntry.scalar(CronEntry.Type.MONTH, 4));
        Assert.assertEquals(CronEntry.parseMonths("may"), CronEntry.scalar(CronEntry.Type.MONTH, 5));
        Assert.assertEquals(CronEntry.parseMonths("jun"), CronEntry.scalar(CronEntry.Type.MONTH, 6));
        Assert.assertEquals(CronEntry.parseMonths("jul"), CronEntry.scalar(CronEntry.Type.MONTH, 7));
        Assert.assertEquals(CronEntry.parseMonths("aug"), CronEntry.scalar(CronEntry.Type.MONTH, 8));
        Assert.assertEquals(CronEntry.parseMonths("sep"), CronEntry.scalar(CronEntry.Type.MONTH, 9));
        Assert.assertEquals(CronEntry.parseMonths("oct"), CronEntry.scalar(CronEntry.Type.MONTH, 10));
        Assert.assertEquals(CronEntry.parseMonths("nov"), CronEntry.scalar(CronEntry.Type.MONTH, 11));
        Assert.assertEquals(CronEntry.parseMonths("dec"), CronEntry.scalar(CronEntry.Type.MONTH, 12));
    }



    @DataProvider(name="malformed")
    public Object[][] malformed() {
        return new Object[][] {
                {"80,90", CronEntry.Type.SECONDS },
                {"0-80", CronEntry.Type.SECONDS },
                {"10-5", CronEntry.Type.SECONDS },
                {"0-80", CronEntry.Type.MINUTES },
                {"80,90", CronEntry.Type.MINUTES },
                {"10-5", CronEntry.Type.SECONDS },
                {"80,90", CronEntry.Type.HOURS },
                {"0-80", CronEntry.Type.HOURS },
                {"10-5", CronEntry.Type.HOURS },
                {"90-100", CronEntry.Type.HOURS },
                {"32", CronEntry.Type.DOM },
                {"0-10", CronEntry.Type.DOM },
                {"44,45", CronEntry.Type.DOM },
                {"1-50/2", CronEntry.Type.DOM },
                {"0-10/2", CronEntry.Type.MONTH },
                {"1-20/2", CronEntry.Type.MONTH },
                {"0-10/2", CronEntry.Type.DOW }
        };
    }


    @Test(expectedExceptions = {RuntimeException.class}, dataProvider = "malformed")
    public void malformedTokens(String token, CronEntry.Type type) {
        CronEntry.parse(token, type);
    }


    @DataProvider(name="ranges")
    public Object[][] ranges() {
        return new Object[][] {
                { "0-10", CronEntry.Type.SECONDS, CronEntry.range(CronEntry.Type.SECONDS, 0, 10, 1) },
                { "0-10/2", CronEntry.Type.SECONDS, CronEntry.range(CronEntry.Type.SECONDS, 0, 10, 2) },
                { "0-10", CronEntry.Type.MINUTES, CronEntry.range(CronEntry.Type.MINUTES, 0, 10, 1) },
                { "0-10/2", CronEntry.Type.MINUTES, CronEntry.range(CronEntry.Type.MINUTES, 0, 10, 2) },
                { "0-10", CronEntry.Type.HOURS, CronEntry.range(CronEntry.Type.HOURS, 0, 10, 1) },
                { "0-10/2", CronEntry.Type.HOURS, CronEntry.range(CronEntry.Type.HOURS, 0, 10, 2) },
                { "0-6", CronEntry.Type.DOW, CronEntry.range(CronEntry.Type.DOW, 0, 6, 1) },
                { "0-6/2", CronEntry.Type.DOW, CronEntry.range(CronEntry.Type.DOW, 0, 6, 2) },
                { "sun-sat", CronEntry.Type.DOW, CronEntry.range(CronEntry.Type.DOW, 0, 6, 1) },
                { "sun-sat/2", CronEntry.Type.DOW, CronEntry.range(CronEntry.Type.DOW, 0, 6, 2) },
                { "1-6", CronEntry.Type.MONTH, CronEntry.range(CronEntry.Type.MONTH, 1, 6, 1) },
                { "1-6/2", CronEntry.Type.MONTH, CronEntry.range(CronEntry.Type.MONTH, 1, 6, 2) },
                { "jan-jun", CronEntry.Type.MONTH, CronEntry.range(CronEntry.Type.MONTH, 1, 6, 1) },
                { "jan-jun/2", CronEntry.Type.MONTH, CronEntry.range(CronEntry.Type.MONTH, 1, 6, 2) },
        };
    }


    @Test(dataProvider = "ranges")
    public void testRangeParse(String token, CronEntry.Type type, CronEntry expected) {
        Assert.assertEquals(CronEntry.parse(token, type), expected);
    }


    @Test()
    public void testCompositeParse() {
        CronEntry.Type type = CronEntry.Type.SECONDS;
        CronEntry actual = CronEntry.parse("0,1,2,3-6,7-10/2", type);
        CronEntry expected = CronEntry.composite(type, Stream.of(
                CronEntry.scalar(type, 0),
                CronEntry.scalar(type, 1),
                CronEntry.scalar(type, 2),
                CronEntry.range(type, 3, 6, 1),
                CronEntry.range(type, 7, 10, 2)
        ));
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void testMaxDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 9, 10, 0, 0);
        Assert.assertEquals(calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 31);
        calendar.set(2018, 1, 10, 0, 0);
        Assert.assertEquals(calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 28);
        calendar.set(2018, 3, 10, 0, 0);
        Assert.assertEquals(calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 30);
    }


    @Test()
    public void entry() {
        List<Integer> expected = List.of(0, 1, 2, 3, 4, 5);
        CronEntry range = CronEntry.parse("0-5", CronEntry.Type.SECONDS);
        expected.forEach(v -> Assert.assertTrue(range.contains(v)));
        Assert.assertTrue(!range.contains(7));
        Assert.assertTrue(range.values().boxed().collect(Collectors.toList()).containsAll(expected));
        Assert.assertEquals(range.values().boxed().toArray(), expected.toArray());
    }


    @Test()
    public void entryWithStep() {
        List<Integer> expected = List.of(0, 2, 4, 6, 8, 10);
        CronEntry range = CronEntry.parse("0-10/2", CronEntry.Type.SECONDS);
        expected.forEach(v -> Assert.assertTrue(range.contains(v)));
        Assert.assertTrue(!range.contains(3));
        Assert.assertTrue(range.values().boxed().collect(Collectors.toList()).containsAll(expected));
        Assert.assertEquals(range.values().boxed().toArray(), expected.toArray());
    }



    @Test()
    public void dualEntry() {
        List<Integer> expected = List.of(0, 1, 2, 4, 6, 8, 10);
        CronEntry entry = CronEntry.parse("0-2,4-10/2", CronEntry.Type.SECONDS);
        expected.forEach(v -> Assert.assertTrue(entry.contains(v)));
        Assert.assertTrue(!entry.contains(3));
        Assert.assertTrue(entry.values().boxed().collect(Collectors.toList()).containsAll(expected));
        Assert.assertEquals(entry.values().boxed().toArray(), expected.toArray());
    }


    @Test()
    public void dualEntryWithOverlap() {
        List<Integer> expected = List.of(0, 1, 2, 4, 6, 8, 10);
        CronEntry entry = CronEntry.parse("0-2,0-10/2", CronEntry.Type.SECONDS);
        expected.forEach(v -> Assert.assertTrue(entry.contains(v)));
        Assert.assertTrue(!entry.contains(3));
        Assert.assertTrue(entry.values().boxed().collect(Collectors.toList()).containsAll(expected));
        Assert.assertEquals(entry.values().boxed().toArray(), expected.toArray());
    }

    @Test()
    public void dualEntrySorted() {
        List<Integer> expected = List.of(0, 1, 2, 3, 4, 6, 8, 10);
        CronEntry entry = CronEntry.parse("4-10/2,0-4", CronEntry.Type.SECONDS);
        expected.forEach(v -> Assert.assertTrue(entry.contains(v)));
        Assert.assertTrue(!entry.contains(12));
        Assert.assertTrue(entry.values().boxed().collect(Collectors.toList()).containsAll(expected));
        Assert.assertEquals(entry.values().boxed().toArray(), expected.toArray());
    }

}
