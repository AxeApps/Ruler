package com.Axe.Ruler;

import java.io.File;
import java.sql.*;

public class PlayerDatabase {

    private static String DB_URL;
    private Connection connection;

    public PlayerDatabase(File dataFolder) {
        this.DB_URL = "jdbc:sqlite:" + dataFolder.toPath().resolve("data.db");
        connect();
        createTable();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS playerdata (
                    ai INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    uuid TEXT UNIQUE NOT NULL
                );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addPlayer(String username, String uuid) {
        String sql = "INSERT INTO playerdata(username, uuid) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error inserting player: " + e.getMessage());
            return false;
        }
    }

    public boolean playerExists(String username, String uuid) {
        String sql = "SELECT 1 FROM playerdata WHERE username = ? OR uuid = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, uuid);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // true if at least one row exists
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
            System.out.println("Connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}