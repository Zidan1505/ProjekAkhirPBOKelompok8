package org.example.hotelsystem.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/hotelsystem";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin123";

    private static Connection connection = null;

    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                try {
                    Class.forName("org.postgresql.Driver");
                    connection = DriverManager.getConnection(URL, USER, PASSWORD);
                    System.out.println("Koneksi ke database PostgreSQL berhasil!");
                } catch (ClassNotFoundException e) {
                    System.err.println("Driver PostgreSQL tidak ditemukan!");
                    e.printStackTrace();
                } catch (SQLException e) {
                    System.err.println("Gagal terhubung ke database!");
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            System.err.println("Gagal memeriksa status koneksi!");
            e.printStackTrace();
        }
        return connection;
    }
}
