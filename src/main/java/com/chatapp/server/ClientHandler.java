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
            processAuthentication();
            String message;
            while ((message = reader.readLine()) != null) {
                saveMessage(username, message);
                Server.broadcastMessage(username + ": " + message);
            }
        } catch (IOException e) {
            System.err.println("Error in client handler: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void processAuthentication() throws IOException {
        writer.println("Enter username:");
        String inputUsername = reader.readLine();
        writer.println("Enter password:");
        String password = reader.readLine();

        if (validateUser(inputUsername, password)) {
            this.username = inputUsername;
            writer.println("Authentication successful");
            Server.addClient(username, this);
        } else {
            writer.println("Authentication failed");
            throw new IOException("Authentication failed");
        }
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
