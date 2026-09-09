package com.faceattend.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

public class LoginView extends VBox {

    public LoginView(BiConsumer<String, String> onLogin) {
        setSpacing(12);
        setPadding(new Insets(32));
        setMaxWidth(360);

        Label title = new Label("FaceAttend Admin Login");
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label status = new Label();
        Button login = new Button("Login");
        login.setDefaultButton(true);
        login.setOnAction(event -> {
            if (username.getText().isBlank() || password.getText().isBlank()) {
                status.setText("Username and password are required.");
            } else {
                onLogin.accept(username.getText(), password.getText());
            }
        });
        getChildren().addAll(title, username, password, login, status);
    }
}
