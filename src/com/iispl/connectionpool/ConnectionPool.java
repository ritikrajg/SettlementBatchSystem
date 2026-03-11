package com.iispl.connectionpool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class ConnectionPool {
    private static final DataSource dataSource;

    static {
        try {
            InputStream inputStream = new FileInputStream("resources/db.properties");
            Properties properties = new Properties();
            properties.load(inputStream);

            String driverClass = properties.getProperty("DRIVER_CLASS");
            String connectionString = properties.getProperty("CONNECTION_STRING");
            String username = properties.getProperty("USERNAME");
            String password = properties.getProperty("PASSWORD");

            if (driverClass != null && !driverClass.isBlank()) {
                Class.forName(driverClass);
            }

            dataSource = new BasicDriverManagerDataSource(connectionString, username, password);
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Failed to initialize datasource: " + e.getMessage());
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    private static final class BasicDriverManagerDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String pass;

        private BasicDriverManagerDataSource(String url, String user, String pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, pass);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return DriverManager.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            DriverManager.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return DriverManager.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("Logging hierarchy is not supported.");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper.");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
