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
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import com.d3x.core.util.IO;
import com.d3x.core.util.Option;

/**
 * A class used to setup and execute a database insert or update operation
 *
 * @author  Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class DatabaseUpdate<T> {

    enum Type { INSERT, UPDATE, DELETE }

    private Type type;
    private Database db;
    private Class<T> dataType;
    private Option<String> sql;
    private int batchSize = 1000;
    private Option<DatabaseMapping.Binder<T>> binder;
    private TimeZone timeZone = TimeZone.getDefault();

    /**
     * Constructor
     * @param db        the database reference
     * @param dataType  the data type for this update
     * @param type      the update type (INSERT, UPDATE, DELETE)
     */
    DatabaseUpdate(Database db, Class<T> dataType, Type type) {
        this.db = db;
        this.type = type;
        this.dataType = dataType;
        this.sql = Option.empty();
        this.binder = Option.empty();
    }


    /**
     * Applies a custom SQL expression to this update
     * @param sql   the SQL expression or classpath resource
     * @return      this update
     */
    public DatabaseUpdate<T> sql(String sql) {
        this.sql = Option.of(sql);
        return this;
    }


    /**
     * Applies a custom binder to this update
     * @param binder    the binder to apply
     * @return          this update
     */
    public DatabaseUpdate<T> binder(DatabaseMapping.Binder<T> binder) {
        this.binder = Option.of(binder);
        return this;
    }


    /**
     * Configures the batch size for this operation
     * @param batchSize the batch size
     * @return          this operation
     */
    public DatabaseUpdate<T> batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }


    /**
     * Configures the time zone for this operation
     * @param timeZone  the time zone
     * @return          this operation
     */
    public DatabaseUpdate<T> timeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }


    /**
     * Configures the time zone for this operation
     * @param zoneId    the time zone
     * @return          this operation
     */
    public DatabaseUpdate<T> timeZone(ZoneId zoneId) {
        return timeZone(TimeZone.getTimeZone(zoneId.getId()));
    }


    /**
     * Applies this operation to the record provided
     * @param record    the record instance
     */
    public void apply(T record) {
        this.apply(Stream.of(record));
    }


    /**
     * Applies this database operation the collection of records
     * @param records   the collection of records
     * @return          the number of records processed
     */
    public int apply(Collection<T> records) {
        return execute(records.stream(), resolveSql(), resolveBinder());
    }


    /**
     * Applies this database operation the stream of records
     * @param records   the stream of records
     * @return          the number of records processed
     */
    public int apply(Stream<T> records) {
        return execute(records, resolveSql(), resolveBinder());
    }


    /**
     * Applies this operation asynchronously to the stream of records
     * @param record   the stream of records
     * @return          the Future which exposes the record count
     */
    public Future<Integer> applyAsync(T record) {
        return applyAsync(Stream.of(record));
    }


    /**
     * Applies this operation asynchronously to the stream of records
     * @param records   the stream of records
     * @return          the Future which exposes the record count
     */
    public Future<Integer> applyAsync(Stream<T> records) {
        return db.submit(() -> {
            try {
                return execute(records, resolveSql(), resolveBinder());
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                throw ex;
            }
        });
    }


    /**
     * Returns the binder for this update
     * @return  the binder for update
     */
    private DatabaseMapping.Binder<T> resolveBinder() {
        return binder.orElse(() -> {
            final DatabaseMapping<T> mapping = db.mapping(dataType);
            switch (type) {
                case INSERT:    return mapping.insert();
                case UPDATE:    return mapping.update();
                case DELETE:    return mapping.delete();
                default:    throw new DatabaseException("Unsupported update type: " + type);
            }
        });
    }


    /**
     * Returns the SQL for this update
     * @return      the SQL for this update
     */
    private String resolveSql() {
        return sql.map(Database::sql).orElse(() -> {
            final DatabaseMapping<T> mapping = db.mapping(dataType);
            switch (type) {
                case INSERT:    return DatabaseMapping.getInsertSql(mapping);
                case UPDATE:    return DatabaseMapping.getUpdateSql(mapping);
                case DELETE:    return DatabaseMapping.getDeleteSql(mapping);
                default:    throw new DatabaseException("Unsupported update type: " + type);
            }
        });
    }



    /**
     * Executes this operation over the stream of records
     * @param stream   the stream of records
     * @param sql       the SQL expression
     * @param binder    the binder to bind args to a prepared statement
     * @return          the number of records processed
     */
    private int execute(Stream<T> stream, String sql, DatabaseMapping.Binder<T> binder) {
        int count = 0;
        Connection conn = null;
        PreparedStatement stmt = null;
        final Calendar calendar = Calendar.getInstance(timeZone);
        if (log.isDebugEnabled()) log.debug("SQL: " + sql);
        try {
            final long t1 = System.currentTimeMillis();
            final Iterator<T> iterator = stream.iterator();
            conn = db.getDataSource().getConnection();
            stmt = conn.prepareStatement(sql);
            while (iterator.hasNext()) {
                count++;
                final T record = iterator.next();
                binder.bind(record, stmt, calendar);
                stmt.addBatch();
                if (count % batchSize == 0) {
                    stmt.executeBatch();
                    final long t2 = System.currentTimeMillis();
                    log.info("Put " + count + " records into DB in " + (t2 - t1) + " millis");
                }
            }
            if (count > 0 && count % batchSize != 0) {
                stmt.executeBatch();
                if (count > 10) {
                    final long t2 = System.currentTimeMillis();
                    log.info("Put " + count + " records into DB in " + (t2 - t1) + " millis");
                }
            }
            return count;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to put one or records in database", ex);
        } finally {
            IO.close(stmt, conn);
        }
    }
}
