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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A test data object to read / write to the database
 *
 * @author Xavier Witdouck
 */
@lombok.NoArgsConstructor()
@lombok.AllArgsConstructor()
@lombok.ToString(exclude={"id"})
@lombok.EqualsAndHashCode(exclude={"id"})
public class DbRecord implements Comparable<DbRecord>, Cloneable {

    @lombok.Getter private int id;
    @lombok.Getter private String name;
    @lombok.Getter private long utcValue;
    @lombok.Getter private boolean enabled;
    @lombok.Getter private double price;
    @lombok.Getter private ZoneId zoneId;
    @lombok.Getter private LocalDate aquireDate;
    @lombok.Getter private LocalTime aquireTime;
    @lombok.Getter private LocalDateTime lastUpdated;



    DbRecord enable() {
        try {
            if (enabled) {
                throw new RuntimeException("The record is already enabled");
            } else {
                final DbRecord clone = (DbRecord)super.clone();
                clone.enabled = true;
                return clone;
            }
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Failed to clone record", ex);
        }
    }


    DbRecord reset() {
        try {
            final DbRecord clone = (DbRecord)super.clone();
            clone.enabled = true;
            clone.utcValue = 100;
            clone.price = 100d;
            clone.aquireDate = LocalDate.now();
            clone.aquireTime = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            clone.lastUpdated = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Failed to clone record", ex);
        }
    }


    /**
     * Returns a stream of random test objects
     * @param count the number of objects to generate
     * @return      the stream of objects
     */
    static Stream<DbRecord> random(int count) {
        final Random random = new Random();
        return IntStream.range(0, count).mapToObj(i -> {
            final DbRecord object = new DbRecord();
            object.id = i;
            object.name = "Name-" + i;
            object.utcValue = System.currentTimeMillis();
            object.enabled = i % 2 == 0;
            object.price = random.nextDouble() * 1000d;
            object.zoneId = ZoneId.systemDefault();
            object.aquireDate = LocalDate.now().plusDays(random.nextInt(500));
            object.aquireTime = LocalTime.now().plusMinutes(random.nextInt(20)).truncatedTo(ChronoUnit.SECONDS);
            object.lastUpdated = LocalDateTime.now().plusSeconds(random.nextInt(1000)).truncatedTo(ChronoUnit.SECONDS);
            return object;
        });
    }


    @Override
    public int compareTo(DbRecord other) {
        return Integer.compare(this.id, other.id);
    }


    /**
     * The database mapping for DbRecord
     */
    static class Mapping implements DatabaseMapping<DbRecord> {

        @Override
        public Class<DbRecord> type() {
            return DbRecord.class;
        }

        @Override
        @DatabaseSql("SELECT * FROM DbRecord order by id")
        public Mapper<DbRecord> select() {
            return (rs, calendar) -> new DbRecord(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getLong("utcValue"),
                rs.getBoolean("enabled"),
                rs.getDouble("price"),
                ZoneId.of(rs.getString("zoneId")),
                rs.getDate("aquireDate").toLocalDate(),
                rs.getTime("aquireTime").toLocalTime(),
                rs.getTimestamp("lastUpdated", calendar).toLocalDateTime()
            );
        }

        @Override
        @DatabaseSql("INSERT INTO DbRecord (name, utcValue, enabled, price, zoneId, aquireDate, aquireTime, lastUpdated) values (?,?,?,?,?,?,?,?)")
        public DatabaseMapping.Binder<DbRecord> insert() {
            return (record, stmt, calendar) -> {
                stmt.setString(1, record.name);
                stmt.setLong(2, record.utcValue);
                stmt.setBoolean(3, record.enabled);
                stmt.setDouble(4, record.price);
                stmt.setString(5, record.zoneId.getId());
                stmt.setDate(6, java.sql.Date.valueOf(record.aquireDate));
                stmt.setTime(7, java.sql.Time.valueOf(record.aquireTime));
                stmt.setTimestamp(8, new Timestamp(record.lastUpdated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), calendar);
            };
        }

        @Override
        @DatabaseSql("UPDATE DbRecord set name = ?, utcValue = ?, enabled = ?, price = ?, zoneId = ?, aquireDate = ?, aquireTime = ?, lastUpdated = ? WHERE id = ?")
        public DatabaseMapping.Binder<DbRecord> update() {
            return (record, stmt, calendar) -> {
                stmt.setString(1, record.name);
                stmt.setLong(2, record.utcValue);
                stmt.setBoolean(3, record.enabled);
                stmt.setDouble(4, record.price);
                stmt.setString(5, record.zoneId.getId());
                stmt.setDate(6, java.sql.Date.valueOf(record.aquireDate));
                stmt.setTime(7, java.sql.Time.valueOf(record.aquireTime));
                stmt.setTimestamp(8, new Timestamp(record.lastUpdated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), calendar);
                stmt.setInt(9, record.id);
            };
        }

        @Override
        @DatabaseSql("DELETE DbRecord WHERE id = ?")
        public DatabaseMapping.Binder<DbRecord> delete() {
            return (record, stmt, calendar) -> stmt.setInt(1, record.id);
        }
    }

}
