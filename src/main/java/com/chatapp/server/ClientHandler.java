package com.chatapp.server;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.net.Socket;
import java.io.*;
import java.sql.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private String username;
    private boolean isAuthenticated = false;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    @Override
    public void run() {
        try {
            // Спроба аутентифікації
            if (!processAuthentication()) {
                return; // Завершуємо роботу, якщо аутентифікація не вдалася
            }

            // Обробка повідомлень чату тільки після успішної аутентифікації
            String message;
            while ((message = reader.readLine()) != null) {
                if (isAuthenticated) {
                    saveMessage(username, message);
                    Server.broadcastMessage(username + ": " + message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error in client handler: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean processAuthentication() throws IOException {
        String action = reader.readLine(); // Читаємо тип дії (LOGIN або REGISTER)
        String inputUsername = reader.readLine();
        String password = reader.readLine();

        if ("REGISTER".equals(action)) {
            if (registerUser(inputUsername, password)) {
                this.username = inputUsername;
                this.isAuthenticated = true;
                writer.println("Registration successful");
                Server.addClient(username, this);
                return true;
            } else {
                writer.println("Registration failed - Username already exists");
                return false;
            }
        } else if ("LOGIN".equals(action)) {
            if (validateUser(inputUsername, password)) {
                this.username = inputUsername;
                this.isAuthenticated = true;
                writer.println("Authentication successful");
                Server.addClient(username, this);
                return true;
            } else {
                writer.println("Authentication failed");
                return false;
            }
        }
        return false;
    }

    private boolean validateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(Server.DB_URL)) {
            String query = "SELECT password_hash FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                    return encoder.matches(password, storedHash);
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during validation: " + e.getMessage());
        }
        return false;
    }

    private boolean registerUser(String username, String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(password);

        try (Connection conn = DriverManager.getConnection(Server.DB_URL)) {
            // Перевіряємо, чи існує користувач
            String checkQuery = "SELECT username FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    return false; // Користувач вже існує
                }
            }

            // Створюємо нового користувача
            String insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            return false;
        }
    }

    private void saveMessage(String sender, String content) {
        try (Connection conn = DriverManager.getConnection(Server.DB_URL)) {
            String query = "INSERT INTO messages (sender, content) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, sender);
                pstmt.setString(2, content);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                Server.removeClient(username);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}