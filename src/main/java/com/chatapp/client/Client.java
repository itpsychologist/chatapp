package com.chatapp.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

public class Client extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private TextArea chatArea;
    private TextField messageField;
    private Stage primaryStage;
    private boolean isAuthenticated = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Chat Client");

        initializeUI(primaryStage);

        try {
            connectToServer();
            authenticateUser();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not connect to server");
            Platform.exit();
        }
    }

    private void initializeUI(Stage primaryStage) {
        // Створюємо головний контейнер з відступами тільки з боків та зверху
        VBox root = new VBox(5); // Зменшуємо відступ між елементами
        root.setPadding(new Insets(10, 10, 5, 10));

        // Налаштовуємо область чату
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefRowCount(20); // Встановлюємо бажану кількість рядків

        // Створюємо горизонтальний контейнер для поля введення та кнопки
        HBox inputBox = new HBox(5); // Невеликий відступ між елементами

        // Налаштовуємо поле введення повідомлення
        messageField = new TextField();
        HBox.setHgrow(messageField, Priority.ALWAYS); // Поле введення займає весь доступний простір

        // Налаштовуємо кнопку відправки
        Button sendButton = new Button("Send");
        sendButton.setPrefWidth(60); // Фіксована ширина кнопки
        sendButton.setMinWidth(60);
        sendButton.setMaxWidth(60);

        // Додаємо елементи до горизонтального контейнера
        inputBox.getChildren().addAll(messageField, sendButton);

        // Додаємо всі елементи до головного контейнера
        root.getChildren().addAll(chatArea, inputBox);
        VBox.setVgrow(chatArea, Priority.ALWAYS); // Область чату розтягується на весь доступний простір

        // Налаштовуємо обробники подій
        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());

        // Створюємо та налаштовуємо сцену
        Scene scene = new Scene(root, 400, 600);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(300); // Мінімальна ширина вікна
        primaryStage.setMinHeight(400); // Мінімальна висота вікна
        primaryStage.show();
    }

    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    private void authenticateUser() {
        while (!isAuthenticated) {
            Optional<Pair<String, String>> credentials = showLoginDialog();

            if (!credentials.isPresent()) {
                Platform.exit();
                return;
            }

            try {
                String response = processAuthentication(credentials.get());
                if (response.contains("successful")) {
                    isAuthenticated = true;
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Success");
                        alert.setContentText(response);
                        alert.showAndWait();
                    });
                    // Запускаємо отримання повідомлень тільки після успішної аутентифікації
                    new Thread(this::receiveMessages).start();
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setContentText(response);
                        alert.showAndWait();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                showError("Error during authentication");
                Platform.exit();
                return;
            }
        }
    }

    private String processAuthentication(Pair<String, String> credentials) throws IOException {
        String[] parts = credentials.getKey().split(":");
        String action = parts[0];
        String username = parts[1];

        writer.println(action);
        writer.println(username);
        writer.println(credentials.getValue());

        return reader.readLine();
    }

    private Optional<Pair<String, String>> showLoginDialog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login / Register");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButtonType = new ButtonType("Register", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>("LOGIN:" + username.getText(), password.getText());
            } else if (dialogButton == registerButtonType) {
                return new Pair<>("REGISTER:" + username.getText(), password.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> chatArea.appendText(finalMessage + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("Lost connection to server"));
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty() && isAuthenticated) {
            writer.println(message);
            messageField.clear();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}