package com.faceattend.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Application entry point. Launches the admin/login screen first.
 *
 * TODO (Copilot):
 *  1. Replace this placeholder scene with a proper login screen (fxml/LoginView.fxml).
 *  2. On successful login, open the main dashboard with tabs for:
 *     - Live Monitoring (webcam feed + bounding boxes, per synopsis)
 *     - User Registration (capture face samples via webcam)
 *     - Reports & Export (CSV/PDF, filter by date/class)
 *  3. Wire each screen to the corresponding service/DAO classes already stubbed
 *     under com.faceattend.service and com.faceattend.dao.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        showLogin(primaryStage);
    }

    private void showLogin(Stage primaryStage) {
        BorderPane root = new BorderPane();
        LoginView loginView = new LoginView((username, password) -> {
            if ("admin".equals(username) && "admin".equals(password)) {
                primaryStage.setScene(new Scene(new DashboardView(), 1000, 700));
            }
        });
        root.setCenter(loginView);
        BorderPane.setAlignment(loginView, Pos.CENTER);
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("FaceAttend");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
