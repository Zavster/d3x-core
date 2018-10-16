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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.TimeZone;
import java.util.function.BiConsumer;

import com.d3x.core.util.Option;

/**
 * A component that defines how a data type is mapped to a database for select, insert, update and delete operations.
 *
 * @param <T>   the type for this mapping
 */
public interface DatabaseMapping<T> {


    /**
     * Returns the data type for this mapping
     * @return  the data type
     */
    Class<T> type();

    /**
     * The mapper to map a row in a result set to some object
     * @return  the mapper to map a row in a result set to some object
     */
    default Mapper<T> select() {
        throw new UnsupportedOperationException("The select operation is not supported for type: " + type());
    }

    /**
     * The binder to bind arguments to a PreparedStatement for affecting inserts
     * @return binder to bind arguments to a PreparedStatement for affecting inserts
     */
    default Binder<T> insert()  {
        throw new UnsupportedOperationException("The insert operation is not supported for type: " + type());
    }

    /**
     * The binder to bind arguments to a PreparedStatement for affecting updates
     * @return binder to bind arguments to a PreparedStatement for affecting updates
     */
    default Binder<T> update() {
        throw new UnsupportedOperationException("The update operation is not supported for type: " + type());
    }

    /**
     * The binder to bind arguments to a PreparedStatement for affecting deletes
     * @return binder to bind arguments to a PreparedStatement for affecting deletes
     */
    default Binder<T> delete() {
        throw new UnsupportedOperationException("The delete operation is not supported for type: " + type());
    }


    /**
     * Returns a command separated list of values for an in clause
     * @param values    the list of values, for example List.of("x", "y", "z")
     * @return          the in clause expression, for example ('x', 'y', 'z')
     */
    static String in(Collection<?> values) {
        return in(values, Option.empty());
    }


    /**
     * Returns a command separated list of values for an in clause
     * @param values    the list of values, for example List.of("x", "y", "z")
     * @param zoneId    the optional zone if for formatting LocalDateTime
     * @return          the in clause expression, for example ('x', 'y', 'z')
     */
    static String in(Collection<?> values, Option<ZoneId> zoneId) {
        if (values.isEmpty()) return "()";
        else {
            final StringBuilder in = new StringBuilder("(");
            for (Object value : values) {
                in.append(in.length() > 1 ? ", " : "");
                if (value instanceof String) {
                    in.append("'").append(values).append("'");
                } else if (value instanceof Number) {
                    in.append(value);
                } else if (value instanceof Boolean) {
                    final boolean entry = (Boolean)value;
                    in.append(entry ? "1" : "0");
                } else if (value instanceof Currency) {
                    final Currency entry = (Currency)value;
                    final String text = entry.getCurrencyCode();
                    in.append("'").append(text).append("'");
                } else if (value instanceof LocalDate) {
                    final LocalDate entry = (LocalDate)value;
                    final String text = DateTimeFormatter.ISO_LOCAL_DATE.format(entry);
                    in.append("'").append(text).append("'");
                } else if (value instanceof LocalTime) {
                    final LocalTime entry = (LocalTime)value;
                    final String text = DateTimeFormatter.ISO_LOCAL_TIME.format(entry);
                    in.append("'").append(text).append("'");
                } else if (value instanceof LocalDateTime) {
                    final LocalDateTime entry = (LocalDateTime)value;
                    final LocalDateTime dateTime = zoneId.map(entry::atZone).map(ZonedDateTime::toLocalDateTime).orElse(entry);
                    final String text = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
                    in.append("'").append(text).append("'");
                } else if (value instanceof ZonedDateTime) {
                    final ZonedDateTime entry = (ZonedDateTime)value;
                    final String text = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(entry);
                    in.append("'").append(text).append("'");
                }
            }
            return in.append(")").toString();
        }
    }


    /**
     * Binds an array of arguments to the prepared statement
     * @param stmt  the prepared statement object to bind args to
     * @param timeZone  the time zone for timestamp related fields
     * @param args      the array of args to bind to statement
     * @return          the statement
     */
    static PreparedStatement bindArgs(PreparedStatement stmt, TimeZone timeZone, List<Object> args) throws DatabaseException {
        try {
            for (int i=0; i<args.size(); ++i) {
                final int paramIndex = i + 1;
                final Object arg = args.get(i);
                if (arg instanceof String) {
                    stmt.setString(paramIndex, (String) arg);
                } else if (arg instanceof Boolean) {
                    stmt.setBoolean(paramIndex, (Boolean)arg);
                } else if (arg instanceof Integer) {
                    stmt.setInt(paramIndex, (Integer)arg);
                } else if (arg instanceof Long) {
                    stmt.setLong(paramIndex, (Long)arg);
                } else if (arg instanceof Double) {
                    stmt.setDouble(paramIndex, (Double) arg);
                } else if (arg instanceof Reader) {
                    stmt.setCharacterStream(paramIndex, (Reader) arg);
                } else if (arg instanceof InputStream) {
                    stmt.setBinaryStream(paramIndex, (InputStream)arg);
                } else if (arg instanceof LocalTime) {
                    stmt.setTime(paramIndex, Time.valueOf((LocalTime)arg));
                } else if (arg instanceof java.util.Date) {
                    stmt.setDate(paramIndex, new java.sql.Date(((java.util.Date) arg).getTime()));
                } else if (arg instanceof LocalDate) {
                    stmt.setDate(paramIndex, java.sql.Date.valueOf((LocalDate) arg));
                } else if (arg instanceof Instant) {
                    stmt.setTimestamp(paramIndex, new Timestamp(((Instant)arg).toEpochMilli()));
                } else if (arg instanceof LocalDateTime) {
                    final ZoneId zoneId = ZoneId.of(timeZone.getID());
                    final Calendar calendar = Calendar.getInstance(timeZone);
                    final LocalDateTime localDateTime = (LocalDateTime)arg;
                    final ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
                    stmt.setTimestamp(paramIndex, new Timestamp(zonedDateTime.toInstant().toEpochMilli()), calendar);
                } else if (arg instanceof ZonedDateTime) {
                    final ZonedDateTime dateTime = (ZonedDateTime)arg;
                    final Calendar calendar = Calendar.getInstance(timeZone);
                    stmt.setTimestamp(paramIndex, new Timestamp(dateTime.toInstant().toEpochMilli()), calendar);
                } else {
                    throw new RuntimeException("Cannot bind arg to PreparedStatement, unsupported arg type:" + arg);
                }
            }
            return stmt;
        } catch (SQLException ex) {
            throw new DatabaseException("Failed to bind SQL args to PreparedStatement", ex);
        }
    }


    /**
     * Returns the SQL resolved from the DatabaseSql annotation on the select method
     * @param mapping       the mapping instance
     * @return              the SQL resolved from method
     * @throws DatabaseException    if no annotation on method
     */
    static <T> String getSelectSql(DatabaseMapping<T> mapping) {
        return getSql(mapping, "select");
    }


    /**
     * Returns the SQL resolved from the DatabaseSql annotation on the insert method
     * @param mapping       the mapping instance
     * @return              the SQL resolved from method
     * @throws DatabaseException    if no annotation on method
     */
    static <T> String getInsertSql(DatabaseMapping<T> mapping) {
        return getSql(mapping, "insert");
    }


    /**
     * Returns the SQL resolved from the DatabaseSql annotation on the update method
     * @param mapping       the mapping instance
     * @return              the SQL resolved from method
     * @throws DatabaseException    if no annotation on method
     */
    static <T> String getUpdateSql(DatabaseMapping<T> mapping) {
        return getSql(mapping, "update");
    }


    /**
     * Returns the SQL resolved from the DatabaseSql annotation on the delete method
     * @param mapping       the mapping instance
     * @return              the SQL resolved from method
     * @throws DatabaseException    if no annotation on method
     */
    static <T> String getDeleteSql(DatabaseMapping<T> mapping) {
        return getSql(mapping, "delete");
    }


    /**
     * Returns the SQL resolved from the DatabaseSql annotation on the named method
     * @param mapping       the mapping instance
     * @param methodName    the method name to inspect for annotation
     * @return              the SQL resolved from method
     * @throws DatabaseException    if no annotation on method
     */
    static String getSql(DatabaseMapping<?> mapping, String methodName) {
        try {
            final Class<?> clazz = mapping.getClass();
            final Method method = clazz.getDeclaredMethod(methodName);
            final DatabaseSql annotation = method.getAnnotation(DatabaseSql.class);
            if (annotation == null) {
                throw new DatabaseException("No 'DatabaseSql' annotation on insert method for: " + clazz);
            } else {
                final String value =  annotation.value();
                return value.startsWith("/") ? Database.sql(value) : value;
            }
        } catch (NoSuchMethodException ex) {
            throw new DatabaseException("Unable to resolve SQL from insert mapping", ex);
        }
    }



    /**
     * A Mapper that can create an Object from the current row in a ResultSet
     * @param <T>   the type produced by this Mapper
     */
    @FunctionalInterface()
    interface Mapper<T> {

        /**
         * Returns an record created from the current row in the ResultSet
         * @param rs        the SQL ResultSet reference
         * @param calendar  the calendar to be used to initialize Date and Timestamp
         * @return          the newly created record
         * @throws SQLException    if there is an error creating record
         */
        T map(ResultSet rs, Calendar calendar) throws SQLException;

    }


    /**
     * A Binder that can bind a record to a PreparedStatement object
     * @param <T>   the record type for this Binder
     */
    @FunctionalInterface()
    interface Binder<T> {

        /**
         * Returns a binder that only consumes the record and statement
         * @param consumer  the consumer to receive record and statement
         * @param <V>       the type for binder
         * @return          the newly created binder
         */
        static <V> Binder<V> of(BiConsumer<V,PreparedStatement> consumer) {
            return (record, stmt, calendar) -> consumer.accept(record, stmt);
        }

        /**
         * Binds the record provided to the PreparedStatement
         * @param record        the record to bind to statement
         * @param stmt          the statement to bind to
         * @param calendar      the calendar with appropriate TZ for TIMESTAMP values
         * @throws SQLException     if fails to bind record
         */
        void bind(T record, PreparedStatement stmt, Calendar calendar) throws SQLException;

    }

}
