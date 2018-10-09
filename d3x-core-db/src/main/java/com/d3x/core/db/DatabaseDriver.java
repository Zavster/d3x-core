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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A class that represents a JDBC driver
 *
 * @author Xavier Witdouck
 */
@lombok.ToString()
@lombok.EqualsAndHashCode(of={"driverClassName"})
public class DatabaseDriver {

    private static final Map<String,DatabaseDriver> driverMap = new HashMap<>();

    public enum Type {
        H2, MYSQL, MARIADB, MSSQL, SYBASE, ORACLE, GENERIC
    }


    public static final DatabaseDriver H2 = new DatabaseDriver(Type.H2, "org.h2.Driver");
    public static final DatabaseDriver MYSQL = new DatabaseDriver(Type.MSSQL, "com.mysql.jdbc.Driver");
    public static final DatabaseDriver MARIADB = new DatabaseDriver(Type.MARIADB, "com.mariadb.jdbc.Driver");
    public static final DatabaseDriver MSSQL = new DatabaseDriver(Type.MSSQL, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    public static final DatabaseDriver SYBASE = new DatabaseDriver(Type.SYBASE, "net.sourceforge.jtds.jdbc.Driver");
    public static final DatabaseDriver ORACLE = new DatabaseDriver(Type.ORACLE, "oracle.jdbc.OracleDriver");

    /** The database type */
    @lombok.Getter @lombok.NonNull private Type type;
    /** The driver class name */
    @lombok.Getter @lombok.NonNull private String driverClassName;


    /**
     * Constructor
     * @param type              the database type
     * @param driverClassName   the driver class name
     */
    public DatabaseDriver(Type type, String driverClassName) {
        Objects.requireNonNull(type, "The database type cannot be null");
        Objects.requireNonNull(driverClassName, "The driver class name cannot be null");
        this.type = type;
        this.driverClassName = driverClassName;
        driverMap.put(driverClassName, this);
    }


    /**
     * Returns the driver for the driver class name provided
     * @param driverClassName   the JDBC driver class name
     * @return                  the driver
     */
    public static DatabaseDriver of(String driverClassName) {
        return driverMap.computeIfAbsent(driverClassName, (key) -> new DatabaseDriver(Type.GENERIC, key));
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
