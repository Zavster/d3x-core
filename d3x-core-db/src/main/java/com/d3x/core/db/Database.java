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

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.d3x.core.util.Consumers;
import com.d3x.core.util.IO;
import com.d3x.core.util.Lazy;
import com.d3x.core.util.LifeCycle;
import com.d3x.core.util.Option;

/**
 * A light weight SQL database abstraction layer using an Object / Functional approach to JDBC
 *
 * @author Xavier Witdouck
 */
@lombok.extern.slf4j.Slf4j
public class Database extends LifeCycle.Base {

    private static final Map<String,String> sqlMap = new HashMap<>();
    private static DataSourceAdapter dataSourceAdapter = new DataSourceAdapter.Apache();
    private static Lazy<ExecutorService> executor = Lazy.of(() -> Executors.newFixedThreadPool(10));

    private DatabaseConfig config;
    private DataSource dataSource;
    private Consumers<Database> onStart = new Consumers<>();
    private Map<Class<?>,DatabaseMapping<?>> mappingsMap = new HashMap<>();


    /**
     * Constructor
     * @param config    the config supplier
     */
    public Database(DatabaseConfig config) {
        Objects.requireNonNull(config, "The config supplier cannot be null");
        this.config = config;
    }


    /**
     * Returns a newly created Database instance based on args provided
     * @param config    the database config
     * @param setup     the consumer to perform additional setup
     * @return          the newly created database
     */
    public static Database of(DatabaseConfig config, Consumer<Database> setup) {
        final Database database = new Database(config);
        if (setup != null) {
            setup.accept(database);
        }
        return database;
    }


    /**
     * Returns the Executor thread pool for Database
     * @return      the Executor thread pool
     */
    static Executor getExecutor() {
        return executor.get();
    }


    /**
     * Sets the default executor for running async database operations
     * @param executor  the executor reference
     */
    public static void setExecutor(ExecutorService executor) {
        Database.executor = Lazy.of(() -> executor);
    }


    /**
     * Sets the data source adapter used to create DataSource objects
     * @param dataSourceAdapter    the data source adapter
     */
    public static void setDataSourceAdapter(DataSourceAdapter dataSourceAdapter) {
        Database.dataSourceAdapter = dataSourceAdapter;
    }


    /**
     * Returns the configuration for this database
     * @return  the database configuration
     */
    public DatabaseConfig getConfig() {
        return config;
    }


    /**
     * Returns a reference to the DataSource
     * @return  the DataSource reference
     */
    public DataSource getDataSource() {
        return Option.of(dataSource).orThrow("The database has not been started for: " + config.getUrl());
    }


    /**
     * Adds a onStart consumer to be called when the Database starts up
     * @param onStart   the on-start consumer to call before startup
     * @return          this database
     */
    public Database onStart(Consumer<Database> onStart) {
        this.onStart.attach(onStart);
        return this;
    }


    @Override
    protected void doStart() throws RuntimeException {
        try {
            log.info("Starting database named " + config.getUrl());
            this.dataSource = dataSourceAdapter.create(config);
            this.onStart.accept(this);
        } catch (Exception ex) {
            throw new DatabaseException("Failed to start database component", ex);
        }
    }


    @Override
    protected void doStop() throws RuntimeException {
        try {
            log.info("Stopping database named " + config.getUrl());
            dataSourceAdapter.close(dataSource);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    /**
     * Returns true if a mapping for the type exists
     * @param type  the data type
     * @return      true if a mapping exists
     */
    public boolean supports(Class<?> type) {
        return mappingsMap.containsKey(type);
    }


    /**
     * Registers a database mapping for a data type
     * @param mapping   the mapping reference
     * @param <T>       the type for mapping
     */
    public <T> void register(DatabaseMapping<T> mapping) {
        Objects.requireNonNull(mapping, "The mapper cannot be null");
        this.mappingsMap.put(mapping.type(), mapping);
    }


    /**
     * Returns true if the specified table exists
     * @param tableName the table name
     * @return          true if the table exists
     */
    public boolean tableExists(String tableName) {
        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = getDataSource().getConnection();
            final DatabaseMetaData metaData = conn.getMetaData();
            final Set<String> nameSet = Set.of(tableName, tableName.toLowerCase(), tableName.toUpperCase());
            for (String name : nameSet) {
                rs = metaData.getTables(null, null, name, null);
                if (rs.next()) {
                    return true;
                }
            }
            return false;
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        } finally {
            IO.close(rs, conn);
        }
    }


    /**
     * Returns the record count from a select count(*) type query
     * @param sql       the SQL statement or path to classpath resource with SQL
     * @param args      the args if the SQL is a parameterized statement
     * @return          the record count
     */
    public int count(String sql, Object... args) {
        ResultSet rs = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        final TimeZone timeZone = TimeZone.getDefault();
        final String sqlExpression = sql.startsWith("/") ? sql(sql) : sql;
        try {
            conn = getDataSource().getConnection();
            stmt = conn.prepareStatement(sqlExpression);
            DatabaseMapping.bindArgs(stmt, timeZone, Arrays.asList(args));
            rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception ex) {
            throw new DatabaseException("Failed to execute sql: " + sqlExpression, ex);
        } finally {
            IO.close(rs, stmt, conn);
        }
    }


    /**
     * Executes the sql definition using java.sql.Statement.execute()
     * @param sql       the SQL statement or path to classpath resource with SQL
     * @return          the update count if applicable
     * @throws DatabaseException    if fails to execute sql
     */
    public Option<Integer> execute(String sql) throws DatabaseException {
        return execute(sql, Option.empty());
    }

    /**
     * Executes the sql definition using java.sql.Statement.execute()
     * @param sql       the SQL statement or path to classpath resource with SQL
     * @param handler   the optional handler for any ResultSets that may be produced
     * @return          the update count if applicable
     * @throws DatabaseException    if fails to execute sql
     */
    public Option<Integer> execute(String sql, Option<Consumer<ResultSet>> handler) throws DatabaseException {
        Connection conn = null;
        Statement stmt = null;
        final String sqlExpression = sql.startsWith("/") ? sql(sql) : sql;
        try {
            conn = getDataSource().getConnection();
            stmt = conn.createStatement();
            final boolean results = stmt.execute(sqlExpression);
            if (results && handler.isPresent()) {
                handler.get().accept(stmt.getResultSet());
                while (stmt.getMoreResults()) {
                    handler.get().accept(stmt.getResultSet());
                }
            }
            final int count = stmt.getUpdateCount();
            return count < 0 ? Option.empty() : Option.of(count);
        } catch (Exception ex) {
            throw new DatabaseException("Faile d to execute sql: " + sqlExpression, ex);
        } finally {
            IO.close(stmt, conn);
        }
    }


    /**
     * Performs a SQL executeUpdate() given the parameterized SQL and arguments
     * @param sql       the SQL statement or path to classpath resource with SQL
     * @param args      the arguments to apply to the expression
     * @return          the number of records affected
     */
    public int executeUpdate(String sql, Object... args) {
        return executeUpdate(sql, TimeZone.getDefault(), args);
    }


    /**
     * Performs a SQL executeUpdate() given the parameterized SQL and arguments
     * @param sql       the SQL statement or path to classpath resource with SQL
     * @param timeZone  the time zone to use to store timestamp related fields
     * @param args      the arguments to apply to the expression
     * @return          the number of records affected
     */
    public int executeUpdate(String sql, TimeZone timeZone, Object... args) {
        Connection conn = null;
        PreparedStatement stmt = null;
        final String sqlExpression = sql.startsWith("/") ? sql(sql) : sql;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sqlExpression);
            DatabaseMapping.bindArgs(stmt, timeZone, Arrays.asList(args));
            return stmt.executeUpdate();
        } catch (Exception ex) {
            throw new DatabaseException("Failed to execute sql: " + sqlExpression, ex);
        } finally {
            IO.close(stmt, conn);
        }
    }


    /**
     * Returns a new select operation for the type specified
     * @param type  the data type for operation
     * @return      the database operation
     */
    public <T> DatabaseSelect<T> select(Class<T> type) {
        return new DatabaseSelect<>(this, type);
    }


    /**
     * Returns a new insert operation for the type specified
     * @param type  the data type for operation
     * @return      the database operation
     */
    public <T> DatabaseUpdate<T> insert(Class<T> type) {
        return new DatabaseUpdate<>(this, type, DatabaseUpdate.Type.INSERT);
    }


    /**
     * Returns a new update operation for the type specified
     * @param type  the data type for operation
     * @return      the database operation
     */
    public <T> DatabaseUpdate<T> update(Class<T> type) {
        return new DatabaseUpdate<>(this, type, DatabaseUpdate.Type.UPDATE);
    }


    /**
     * Returns a new delete operation for the type specified
     * @param type  the data type for operation
     * @return      the database operation
     */
    public <T> DatabaseUpdate<T> delete(Class<T> type) {
        return new DatabaseUpdate<>(this, type, DatabaseUpdate.Type.DELETE);
    }


    /**
     * Returns the SQL loaded from a classpath resource if arg starts with "/"
     * @param resource  the classpath resource to load SQL from
     * @return          the SQL expression
     */
    static String sql(String resource) {
        try {
            if (!resource.startsWith("/")) {
                return resource;
            } else if (sqlMap.containsKey(resource)) {
                final String sql = sqlMap.get(resource);
                return sql != null ? sql : resource;
            } else {
                log.debug("Loading SQL expression from " + resource);
                final URL url = Database.class.getResource(resource);
                if (url == null) {
                    throw new RuntimeException("No classpath resource located for path: " + resource);
                } else {
                    final String sql = IO.readText(url.openStream());
                    sqlMap.put(resource, sql);
                    return sql;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load SQL from resource: " + resource, ex);
        }
    }


    /**
     * Returns the mapping for the type specified
     * @param type  the data type for mapping
     * @return      the mapping
     * @throws DatabaseException    if no mapping for type
     */
    @SuppressWarnings("unchecked")
    <T> DatabaseMapping<T> mapping(Class<T> type) {
        final DatabaseMapping<T> mapping = (DatabaseMapping<T>)mappingsMap.get(type);
        if (mapping == null) {
            throw new DatabaseException("No mapping registered for type: " + type);
        } else {
            return mapping;
        }
    }


    /**
     * Submits the Callable to the Database assigned ExecutorService
     * @param callable  the callable to execute
     * @param <R>       the type for callable
     * @return          the future returned by ExecutorService
     */
    <R> Future<R> submit(Callable<R> callable) {
        return executor.get().submit(callable);
    }

}
