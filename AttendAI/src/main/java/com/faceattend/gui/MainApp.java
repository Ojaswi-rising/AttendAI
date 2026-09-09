package com.faceattend.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
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
        Label placeholder = new Label("FaceAttend - Smart Face Recognition Attendance System\n(scaffold ready - build the UI here)");
        StackPane root = new StackPane(placeholder);
        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("FaceAttend");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
