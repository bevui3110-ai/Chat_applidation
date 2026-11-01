package server.core;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // K\u1EBFt n\u1ED1i t\u1EDBi database java_chat_db (user m\u1EB7c đ\u1ECBnh c\u1EE7a XAMPP là root, pass đ\u1EC3 tr\u1ED1ng)
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/java_chat_db?useSSL=false&serverTimezone=UTC", "root", "");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}