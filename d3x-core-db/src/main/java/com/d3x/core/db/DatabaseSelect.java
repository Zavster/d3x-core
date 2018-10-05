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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;

/**
 * A class used to setup and execute a database select
 *
 * @author Xavier Witdouck
 */
public class DatabaseSelect<T> {

    private Database db;
    private Class<T> type;
    private List<Object> args;
    private Option<String> sql;
    private TimeZone timeZone;
    private Option<Integer> limit;

    /**
     * Constructor
     * @param db        the database reference
     * @param type      the data type for this operation
     */
    DatabaseSelect(Database db, Class<T> type) {
        this.db = db;
        this.type = type;
        this.sql = Option.empty();
        this.limit = Option.empty();
        this.args = new ArrayList<>();
        this.timeZone = TimeZone.getDefault();
    }


    /**
     * Binds a custom SQL statement to this select
     * @param sql   the SQL statement or classpath resource to SQL statement
     * @return      this select object
     */
    public DatabaseSelect<T> sql(String sql) {
        this.sql = Option.of(sql);
        return this;
    }


    /**
     * Binds arguments to this select
     * @param args  the arguments for SQL string
     * @return      this select object
     */
    public DatabaseSelect<T> args(Object... args) {
        this.args = Arrays.asList(args);
        return this;
    }


    /**
     * Binds the TimeZone to this Select to initialize the Calendar
     * @param timeZone  the time zone
     * @return          this select object
     */
    public DatabaseSelect<T> timeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }


    /**
     * Binds the ZoneId to this Select to initialize the Calendar
     * @param zoneId  the time zone
     * @return          this select object
     */
    public DatabaseSelect<T> timeZone(ZoneId zoneId) {
        this.timeZone = TimeZone.getTimeZone(zoneId.getId());
        return this;
    }


    /**
     * Sets the max number of records to include
     * @param limit    the max number of records, must be > 0
     * @return          this select object
     */
    public DatabaseSelect<T> limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be >= 0");
        }
        this.limit = Option.of(limit);
        return this;
    }


    /**
     * Returns the default mapper for this select
     * @return  the default mapper for this
     */
    private DatabaseMapping.Mapper<T> mapper() {
        return db.mapping(type).select();
    }


    /**
     * Returns the first object matched by this select
     * @return          the optional first object
     */
    public Option<T> first() {
        return first(mapper());
    }


    /**
     * Returns the list of objects that match this select
     * @return          the list of resulting objects
     */
    public List<T> list() {
        return list(mapper());
    }


    /**
     * Returns a iterator of objects that match this select
     * @return          the iterator of resulting objects
     */
    public Iterator<T> iterator() {
        return iterator(mapper());
    }


    /**
     * Returns a stream of objects that match this select
     * @return          the stream of resulting objects
     */
    public Stream<T> stream() {
        return stream(mapper());
    }


    /**
     * Returns the SQL expression for this select
     * @return  the SQL expression for select
     */
    private String resolveSql() {
        return sql.map(Database::sql).orElse(() -> {
            final DatabaseMapping<T> mapping = db.mapping(type);
            return DatabaseMapping.getSelectSql(mapping);
        });
    }


    /**
     * Returns the first object matched by this select
     * @param mapper    the mapper used to generate objects from ResultSet
     * @return          the optional first object
     */
    public Option<T> first(DatabaseMapping.Mapper<T> mapper) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        final String sql = this.resolveSql();
        final Calendar calendar = Calendar.getInstance(timeZone);
        try {
            conn = db.getDataSource().getConnection();
            stmt = DatabaseMapping.bindArgs(conn.prepareStatement(sql), timeZone, args);
            rs = stmt.executeQuery();
            if (rs.next()) {
                final T result = mapper.map(rs, calendar);
                return Option.of(result);
            } else {
                return Option.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to load first record for sql: " + sql, ex);
        } finally {
            IO.close(rs, stmt, conn);
        }
    }


    /**
     * Returns the list of objects that match this select
     * @param mapper    the mapper to generate objects from ResultSet
     * @return          the list of resulting objects
     */
    public List<T> list(DatabaseMapping.Mapper<T> mapper) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        final String sql = this.resolveSql();
        final int limitValue = limit.orElse(Integer.MAX_VALUE);
        final Calendar calendar = Calendar.getInstance(timeZone);
        try {
            int count = 0;
            conn = db.getDataSource().getConnection();
            stmt = DatabaseMapping.bindArgs(conn.prepareStatement(sql), timeZone, args);
            if (limit.isPresent()) stmt.setMaxRows(limit.get());
            rs = stmt.executeQuery();
            final List<T> results = new ArrayList<>();
            while (rs.next()) {
                final T record = mapper.map(rs, calendar);
                results.add(record);
                if (++count >= limitValue) {
                    break;
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to load records for sql: " + sql, ex);
        } finally {
            IO.close(rs, stmt, conn);
        }
    }


    /**
     * Returns a stream of objects that match this select
     * @param mapper    the mapper to generate objects from ResultSet
     * @return          the stream of resulting objects
     */
    public Stream<T> stream(DatabaseMapping.Mapper<T> mapper) {
        final Iterator<T> iterator = iterator(mapper);
        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false).limit(limit.orElse(Integer.MAX_VALUE));
    }


    /**
     * Returns a iterator over the objects that match this select
     * @param mapper    the mapper to generate objects from ResultSet
     * @return          the iterator of resulting objects
     */
    public Iterator<T> iterator(DatabaseMapping.Mapper<T> mapper) {
        Connection conn = null;
        PreparedStatement stmt = null;
        final String sql = this.resolveSql();
        try {
            conn = db.getDataSource().getConnection();
            stmt = DatabaseMapping.bindArgs(conn.prepareStatement(sql), timeZone, args);
            final ResultSet rs = stmt.executeQuery();
            return iterator(rs, mapper, rs, stmt, conn);
        } catch (Exception ex) {
            IO.close(stmt, conn);
            throw new DatabaseException("Failed to load records for sql: " + sql, ex);
        }
    }


    /**
     * Returns an Iterator over a database ResultSet
     * @param rs        the result set to iterate over
     * @param mapper    the mapper to map objects
     * @return          the newly created iterator
     */
    private Iterator<T> iterator(ResultSet rs, DatabaseMapping.Mapper<T> mapper, AutoCloseable... closeables) {
        return new Iterator<>() {
            private Boolean next;
            private Calendar calendar = Calendar.getInstance(timeZone);
            @Override
            public boolean hasNext() {
                try {
                    if (next != null) {
                        return next;
                    } else {
                        this.next = rs.next();
                        if (!next) {
                            IO.close(closeables);
                        }
                        return next;
                    }
                } catch (Throwable t) {
                    IO.close(closeables);
                    throw new DatabaseException(t.getMessage(), t);
                }
            }
            @Override
            public T next() {
                try {
                    if (!hasNext()) {
                        throw new NoSuchElementException("Database Iterator has been exhausted");
                    } else {
                        final T record = mapper.map(rs, calendar);
                        this.next = null;
                        return record;
                    }
                } catch (Throwable t) {
                    IO.close(closeables);
                    throw new DatabaseException(t.getMessage(), t);
                }
            }
        };
    }
}
