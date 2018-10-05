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

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.d3x.core.util.Collect;
import com.d3x.core.util.Option;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests for the Database class
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class DbTests {

    private Database database;


    @BeforeClass()
    public void setup() {
        final File tempFile = new File("test-db");
        final DatabaseConfig config = DatabaseConfig.h2(tempFile.getAbsoluteFile(), "sa", null);
        log.info("Creating test database at " + tempFile.getAbsolutePath());
        this.database = Database.of(config, null);
        this.database.register(new DbRecord.Mapping());
        this.database.onStart(db -> db.execute("/sql/test-ddl.sql", Option.empty()));
        this.database.start();
    }


    @AfterClass()
    public void teardown() {
        this.database.execute("TRUNCATE TABLE DbRecord", Option.empty());
        this.database.stop();
    }


    @DataProvider(name="counts")
    public Object[][] counts() {
        return new Object[][] { {100}, {1000}, {10000} };
    }


    @Test()
    public void verify() {
        this.database.getConfig().verify();
        Assert.assertTrue(database.tableExists("DbRecord"));
        Assert.assertEquals(database.getConfig().getDriver().getDriverClassName(), DatabaseDriver.H2.getDriverClassName());
    }


    @Test(dataProvider="counts", dependsOnMethods={"verify"})
    public void populate(int count) {
        final TimeZone gmt = TimeZone.getTimeZone("GMT");
        this.database.execute("TRUNCATE TABLE DbRecord", Option.empty());
        List<DbRecord> input = DbRecord.random(count).collect(Collectors.toList());
        Assert.assertEquals(database.select(DbRecord.class).stream().count(), 0);
        this.database.insert(DbRecord.class).timeZone(gmt).apply(input);
        List<DbRecord> records = database.select(DbRecord.class).timeZone(gmt).list();
        Assert.assertEquals(records.size(), count);
        Assert.assertEquals(records, input);
    }


    @Test(dependsOnMethods={"populate"})
    public void customBooleanSelect() {
        final String sql = "SELECT * FROM DbRecord where enabled = ?";
        final List<DbRecord> values = database.select(DbRecord.class).sql(sql).args(true).list();
        Assert.assertTrue(values.size() > 0);
        values.forEach(v -> Assert.assertTrue(v.isEnabled()));
    }


    @Test(dependsOnMethods={"populate"})
    public void customDateSelect() {
        final String sql = "SELECT * FROM DbRecord WHERE enabled = ? AND aquireDate > ?";
        final LocalDate minDate = LocalDate.now().plusDays(50);
        final List<DbRecord> values = database.select(DbRecord.class).sql(sql).args(false, minDate).list();
        Assert.assertTrue(values.size() > 0);
        values.forEach(v -> {
            Assert.assertFalse(v.isEnabled());
            Assert.assertTrue(v.getAquireDate().isAfter(minDate));
        });
    }


    @Test(dependsOnMethods={"populate"})
    public void customDoubleSelect() {
        final double minPrice = 500d;
        final String sql = "SELECT * FROM DbRecord WHERE enabled = ? AND price > ?";
        final List<DbRecord> values = database.select(DbRecord.class).sql(sql).args(true, minPrice).list();
        Assert.assertTrue(values.size() > 0);
        values.forEach(v -> {
            Assert.assertTrue(v.isEnabled());
            Assert.assertTrue(v.getPrice() > minPrice);
        });
    }


    @Test(dependsOnMethods={"populate"})
    public void customClasspathSelect() {
        final String sql = "/sql/test-query.sql";
        final LocalDate minDate = LocalDate.now().plusDays(50);
        final List<DbRecord> values = database.select(DbRecord.class).sql(sql).args(true, minDate).list();
        Assert.assertTrue(values.size() > 0);
        values.forEach(v -> {
            Assert.assertTrue(v.isEnabled());
            Assert.assertTrue(v.getAquireDate().isAfter(minDate));
        });
    }



    @Test(dependsOnMethods={"populate"})
    public void customMapper() {
        final String sql = "SELECT * FROM DbRecord WHERE enabled = ?";
        final List<DbRecord> values = database.select(DbRecord.class)
                .sql(sql)
                .args(true)
                .list((rs, calendar) -> new DbRecord(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getLong("utcValue"),
                    rs.getBoolean("enabled"),
                    100d,
                    ZoneId.systemDefault(),
                    rs.getDate("aquireDate").toLocalDate(),
                    rs.getTime("aquireTime").toLocalTime(),
                    rs.getTimestamp("lastUpdated", calendar).toLocalDateTime()
                ));
        Assert.assertTrue(values.size() > 0);
        values.forEach(v -> {
            Assert.assertTrue(v.isEnabled());
            Assert.assertEquals(v.getPrice(), 100d);
            Assert.assertEquals(v.getZoneId(), ZoneId.systemDefault());
        });
    }




    @Test(dependsOnMethods={"populate"})
    public void iterator() {
        List<DbRecord> values1 = database.select(DbRecord.class).list();
        List<DbRecord> values2 = database.select(DbRecord.class).stream().collect(Collectors.toList());
        List<DbRecord> values3 = Collect.asList(database.select(DbRecord.class).iterator());
        Assert.assertEquals(values1, values2);
        Assert.assertEquals(values1, values3);
    }


    @Test(dependsOnMethods={"populate"})
    public void limit() {
        Assert.assertEquals(database.select(DbRecord.class).limit(10).stream().count(), 10);
    }


    @Test(dependsOnMethods={"populate"})
    public void first() {
        final List<DbRecord> values = database.select(DbRecord.class).limit(10).list();
        final Option<DbRecord> first = database.select(DbRecord.class).first();
        Assert.assertEquals(values.size(), 10);
        Assert.assertTrue(first.isPresent());
        Assert.assertEquals(first.get(), values.get(0));
        Assert.assertTrue(database.select(DbRecord.class).sql("select * from DbRecord where 1=2").first().isEmpty());
    }


    @Test(dependsOnMethods={"populate"}, expectedExceptions={IllegalArgumentException.class})
    public void badLimit() {
        database.select(DbRecord.class).limit(-10).list();
    }



    @Test(dependsOnMethods={"populate"})
    public void update() {
        final List<DbRecord> values = database.select(DbRecord.class).limit(10).list();
        final List<DbRecord> updates = values.stream().map(DbRecord::reset).collect(Collectors.toList());
        this.database.update(DbRecord.class).apply(updates);
        final Set<Integer> keys = values.stream().map(DbRecord::getId).collect(Collectors.toSet());
        final String sql = "SELECT * from DbRecord where id in " + DatabaseMapping.in(keys);
        final List<DbRecord> results = database.select(DbRecord.class).sql(sql).list();
        Collections.sort(updates);
        Collections.sort(results);
        Assert.assertEquals(results, updates);
    }


    @Test(dependsOnMethods={"update"})
    public void updateCustom() {
        String countSql = "SELECT count(*) from DbRecord where enabled = true";
        int enabledCount = database.count(countSql);
        Assert.assertTrue(enabledCount > 0, "There are some enabled records");
        int updateCount = database.executeUpdate("UPDATE DbRecord set enabled = ? WHERE enabled = ?", false, true);
        Assert.assertEquals(updateCount, enabledCount);
        int count = database.count(countSql);
        Assert.assertEquals(count, 0, "No more enabled values");
    }


    @Test(dependsOnMethods={"updateCustom"})
    public void updateAsync() throws Exception {
        String selectSql = "SELECT * from DbRecord where enabled = ?";
        List<DbRecord> records = database.select(DbRecord.class).sql(selectSql).args(false).list();
        Stream<DbRecord> updates = records.stream().map(DbRecord::enable);
        Assert.assertTrue(records.size() > 0, "There are some disabled records");
        records.forEach(v -> Assert.assertFalse(v.isEnabled()));
        int updateCount = database.update(DbRecord.class).applyAsync(updates).get();
        Assert.assertEquals(updateCount, records.size());
        int count = database.count("SELECT * from DbRecord where enabled = ?", false);
        Assert.assertEquals(count, 0, "No more enabled values");
    }


    @Test(dependsOnMethods={"updateCustom", "badLimit"})
    public void delete() {
        String countSql = "SELECT count(*) from DbRecord";
        final int oldCount = database.count(countSql);
        final List<DbRecord> values = database.select(DbRecord.class).limit(10).list();
        Assert.assertEquals(values.size(), 10);
        this.database.delete(DbRecord.class).apply(values);
        final int newCount = database.count(countSql);
        Assert.assertEquals(newCount, oldCount - 10);
    }


    @Test(dependsOnMethods={"delete", "badLimit"})
    public void deleteCustom() {
        final String countSql = "SELECT count(*) from DbRecord";
        final List<DbRecord> values = database.select(DbRecord.class).limit(10).list();
        final Set<Integer> keys = values.stream().map(DbRecord::getId).collect(Collectors.toSet());
        final String inCaluse = DatabaseMapping.in(keys);
        final int deleteCount = database.executeUpdate("DELETE DbRecord where id in "+ inCaluse);
        Assert.assertEquals(deleteCount, values.size());
        Assert.assertEquals(database.count(countSql + " where id in " + inCaluse), 0);
    }

}
