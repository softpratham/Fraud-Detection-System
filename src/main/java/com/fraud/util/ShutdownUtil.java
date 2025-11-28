package com.fraud.util;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public final class ShutdownUtil {
    private ShutdownUtil() { }

    public static void closeDataSource(Object ds) {
        // close Hikari if present
        try {
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
            }
        } catch (Exception ignored) {}

        // deregister JDBC drivers to avoid JVM thread leak warnings
        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
