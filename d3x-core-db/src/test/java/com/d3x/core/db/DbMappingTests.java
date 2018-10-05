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
package com.d3x.core.db;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;

import com.d3x.core.util.IO;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the DatabaseMapping class
 *
 * @author Xavier Witdouck
 */
public class DbMappingTests {



    @DataProvider(name="lists")
    public Object[][] lists() {
        return new Object[][] {
            { List.of(1), "(1)" },
            { List.of(1, 2, 3), "(1, 2, 3)" },
            { List.of(1L, 2L, 3L), "(1, 2, 3)" },
            { List.of(1d, 2d, 3d), "(1.0, 2.0, 3.0)" },
            { List.of(true,false,true), "(1, 0, 1)" },
            { List.of(Currency.getInstance("GBP"), Currency.getInstance("USD")), "('GBP', 'USD')" },
            { List.of(LocalDate.parse("2014-01-01"), LocalDate.parse("2014-01-03")), "('2014-01-01', '2014-01-03')" },
            { List.of(LocalTime.parse("22:33:00"), LocalTime.parse("23:44:34")), "('22:33:00', '23:44:34')" },
            { List.of(LocalDateTime.parse("2014-01-01T22:33:00"), LocalDateTime.parse("2014-01-01T23:44:34")), "('2014-01-01T22:33:00', '2014-01-01T23:44:34')" },
        };
    }


    @Test(dataProvider="lists")
    public void in(List<?> values, String expected) {
        IO.println(expected);
        Assert.assertEquals(DatabaseMapping.in(values), expected);
    }
}
