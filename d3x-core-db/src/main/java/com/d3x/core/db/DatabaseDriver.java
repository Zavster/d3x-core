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

import java.util.Objects;

/**
 * A class that represents a JDBC driver
 *
 * @author Xavier Witdouck
 */
@lombok.ToString()
@lombok.EqualsAndHashCode(of={"driverClassName"})
public class DatabaseDriver {

    public static final DatabaseDriver H2 = new DatabaseDriver("org.h2.Driver");
    public static final DatabaseDriver MYSQL = new DatabaseDriver("com.mysql.jdbc.Driver");
    public static final DatabaseDriver MARIADB = new DatabaseDriver("com.mariadb.jdbc.Driver");
    public static final DatabaseDriver MSSQL = new DatabaseDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    public static final DatabaseDriver SYBASE = new DatabaseDriver("net.sourceforge.jtds.jdbc.Driver");
    public static final DatabaseDriver ORACLE = new DatabaseDriver("oracle.jdbc.OracleDriver");

    @lombok.Getter @lombok.NonNull
    private String driverClassName;

    /**
     * Constructor
     * @param driverClassName   the driver class name
     */
    public DatabaseDriver(String driverClassName) {
        Objects.requireNonNull(driverClassName, "The class name cannot be null");
        this.driverClassName = driverClassName;
    }

    /**
     * Returns true if the driver is available on the classpath
     * @return      true if the driver is available
     */
    public boolean isAvailable() {
        try {
            Class.forName(driverClassName);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
