package com.chatapp.server;

import java.net.ServerSocket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 8080;
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public static final String DB_URL = "jdbc:sqlite:chatapp.db";

    public static void addClient(String username, ClientHandler client) {
        clients.put(username, client);
    }

    public static void removeClient(String username) {
        clients.remove(username);
    }

    public static void broadcastMessage(String message) {
        clients.values().forEach(client -> client.sendMessage(message));
    }

    public static void main(String[] args) {
        initDatabase();

        try (var serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server is running on port " + PORT);

            while (true) {
                var socket = serverSocket.accept();
                System.out.println("New client connected");

                var clientHandler = new ClientHandler(socket);
                var thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initDatabase() {
        try (var conn = DriverManager.getConnection(DB_URL)) {
            var createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL
                )
            """;

            var createMessagesTable = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """;

            try (var stmt = conn.createStatement()) {
                stmt.execute(createUsersTable);
                stmt.execute(createMessagesTable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
