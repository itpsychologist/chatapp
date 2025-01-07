module com.example.chatapplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires spring.security.crypto;

    requires javafx.graphics;

    opens com.example.chatapplication to javafx.fxml;
    exports com.example.chatapplication;
    exports com.chatapp.client to javafx.graphics;
}