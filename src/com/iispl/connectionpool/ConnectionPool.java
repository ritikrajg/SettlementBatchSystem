package com.iispl.connectionpool;

import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ConnectionPool {
	private static ComboPooledDataSource dataSource;
	static {
		try {
			dataSource = new ComboPooledDataSource(); //creating obj of combopool
			InputStream inputStream = new FileInputStream("resources/db.properties");
			//reading data from db.properties
			Properties properties = new Properties(); //creating obj
			properties.load(inputStream); //it is loading inputstream
			//loads data and setting values to datasource obj by fetching  values from db.properties 
			dataSource.setDriverClass(properties.getProperty("DRIVER_CLASS"));
			dataSource.setJdbcUrl(properties.getProperty("CONNECTION_STRING"));
			dataSource.setUser(properties.getProperty("USERNAME"));
			dataSource.setPassword(properties.getProperty("PASSWORD"));
			
			//the setting below are optional
			//c3p0 can work with defaults
			dataSource.setInitialPoolSize(5);
			dataSource.setMinPoolSize(5);
			dataSource.setAcquireIncrement(5);
			dataSource.setMaxPoolSize(20);
			
			
		} catch(IOException | PropertyVetoException e) {
			e.printStackTrace();
		}
	}
	public static javax.sql.DataSource getDataSource(){
		return dataSource;
	}
}

