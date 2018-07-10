package com.pablo67340.SQLiteLib.Database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import com.pablo67340.guishop.main.Main;

public class SQLite extends Database {

	private String dbname;

	private String createTestTable = "CREATE TABLE IF NOT EXISTS test (" + "`test` varchar(32) NOT NULL,"
			+ "PRIMARY KEY (`test`)" + ");";
	
	private String customCreateString;
	
	private File dataFolder;

	public SQLite(String databaseName, String createStatement, File folder) {
		dbname = databaseName;
		customCreateString = createStatement;
		dataFolder = folder;
	}

	public Connection getSQLConnection() {
		File folder = new File(dataFolder, dbname + ".db");
		if (!folder.exists()) {
			try {
				folder.createNewFile();
			} catch (IOException e) {
				Main.getInstance().getLogger().log(Level.SEVERE, "File write error: " + dbname + ".db");
			}
		}
		try {
			if (connection != null && !connection.isClosed()) {
				return connection;
			}
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:" + folder);
			return connection;
		} catch (SQLException ex) {
			Main.getInstance().getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
		} catch (ClassNotFoundException ex) {
			Main.getInstance().getLogger().log(Level.SEVERE,
					"You need the SQLite JBDC library. Google it. Put it in /lib folder.");
		}
		return null;
	}

	public void load() {
		connection = getSQLConnection();
		try {
			Statement s = connection.createStatement();
			s.executeUpdate(createTestTable);
			s.executeUpdate(customCreateString);
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		initialize();
	}
	
	public File getDataFolder() {
		return dataFolder;
	}
}
