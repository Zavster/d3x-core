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

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * An convenience adapter to work with DataSource implementations from different libraries.
 *
 * @author Xavier Witdouck
 */
public interface DataSourceAdapter {

    /**
     * Closes the data source if appropriate
     * @param source    the data source
     */
    void close(DataSource source);

    /**
     * Returns a newly created DataSource based on the config provided
     * @param config    the database configuration
     * @return          the newly created DataSource
     * @throws DatabaseException    if operation fails
     */
    DataSource create(DatabaseConfig config) throws DatabaseException;


    /**
     * A DataSourceFactory that uses Apache Commons DBCP library
     */
    @lombok.extern.slf4j.Slf4j
    class Apache implements DataSourceAdapter {

        @Override
        public DataSource create(DatabaseConfig config) throws DatabaseException {
            final BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(config.getDriver().getDriverClassName());
            dataSource.setUrl(config.getUrl());
            dataSource.setUsername(config.getUser().orNull());
            dataSource.setPassword(config.getPassword().map(p -> new String(p.getValue())).orNull());
            dataSource.setInitialSize(config.getInitialPoolSize().orElse(1));
            dataSource.setMaxTotal(config.getMaxPoolSize().orElse(5));
            dataSource.setMaxIdle(config.getMaxPoolIdleSize().orElse(5));
            dataSource.setDefaultReadOnly(config.getReadOnly().orElse(false));
            dataSource.setDefaultQueryTimeout(config.getQueryTimeOutSeconds().orElse(60));
            dataSource.setMaxWaitMillis(config.getMaxWaitTimeMillis().orElse(5000));
            return dataSource;
        }

        @Override
        public void close(DataSource source) {
            try {
                if (source instanceof BasicDataSource) {
                    ((BasicDataSource)source).close();
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
    }
}
