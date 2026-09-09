package com.faceattend.gui;

import com.faceattend.dao.AttendanceDAO;
import com.faceattend.dao.UserDAO;
import com.faceattend.model.AttendanceRecord;
import com.faceattend.model.User;
import com.faceattend.service.FaceDetectionService;
import com.faceattend.service.FaceEnrollmentService;
import com.faceattend.service.AttendanceMarkingService;
import com.faceattend.service.FaceRecognitionService;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import com.opencsv.CSVWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;

import java.io.File;
import java.io.FileWriter;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DashboardView extends BorderPane {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final UserDAO userDAO = new UserDAO();

    public DashboardView() {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Live Monitoring", liveMonitoringView()));
        tabs.getTabs().add(new Tab("Register User", registerUserView()));
        tabs.getTabs().add(new Tab("Reports", reportsView()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        setCenter(tabs);
        setPadding(new Insets(16));
    }

    private Node liveMonitoringView() {
        ImageView preview = new ImageView();
        preview.setFitWidth(640);
        preview.setFitHeight(420);
        preview.setPreserveRatio(true);
        Label status = new Label("Camera is idle.");
        Button start = new Button("Start Monitoring");
        Button stop = new Button("Stop Monitoring");
        stop.setDisable(true);
        AtomicBoolean running = new AtomicBoolean(false);
        start.setOnAction(event -> {
            try {
                FaceRecognitionService recognition = new FaceRecognitionService();
                Path faces = Path.of("data", "faces");
                recognition.train(faces);
                recognition.save(Path.of("data", "model.xml").toString());
                AttendanceMarkingService marking = new AttendanceMarkingService(
                        new FaceDetectionService(), recognition, attendanceDAO, userDAO);
                running.set(true);
                start.setDisable(true);
                stop.setDisable(false);
                status.setText("Monitoring. Look at the camera.");
                Thread monitorThread = new Thread(() -> {
                    try (Java2DFrameConverter converter = new Java2DFrameConverter();
                         OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat()) {
                        marking.monitor(frame -> {
                            BufferedImage image = converter.convert(matConverter.convert(frame));
                            if (image != null) {
                                javafx.application.Platform.runLater(() -> preview.setImage(
                                        SwingFXUtils.toFXImage(image, null)));
                            }
                        }, running::get);
                    } catch (Exception exception) {
                        javafx.application.Platform.runLater(() -> status.setText(exception.getMessage()));
                    } finally {
                        javafx.application.Platform.runLater(() -> {
                            start.setDisable(false);
                            stop.setDisable(true);
                        });
                    }
                }, "faceattend-monitor");
                monitorThread.setDaemon(true);
                monitorThread.start();
            } catch (Exception exception) {
                status.setText(exception.getMessage());
            }
        });
        stop.setOnAction(event -> {
            running.set(false);
            status.setText("Camera stopped.");
        });
        return new VBox(12, preview, status, new javafx.scene.layout.HBox(8, start, stop));
    }

    private Node registerUserView() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        TextField name = new TextField();
        TextField code = new TextField();
        TextField classOrDept = new TextField();
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("User code"), code);
        form.addRow(2, new Label("Class / department"), classOrDept);
        Label status = new Label();
        Button capture = new Button("Capture Samples");
        capture.setOnAction(event -> {
            try {
                FaceEnrollmentService enrollment = new FaceEnrollmentService(
                        new FaceDetectionService(), userDAO, Path.of("data", "faces"));
                User user = new User();
                user.setName(name.getText());
                user.setUserCode(code.getText());
                user.setClassOrDept(classOrDept.getText());
                int count = enrollment.captureSamples(user);
                status.setText("Captured " + count + " samples for " + user.getName());
            } catch (Exception exception) {
                status.setText(exception.getMessage());
            }
        });
        return new VBox(14, form, capture, status);
    }

    private Node reportsView() {
        DatePicker from = new DatePicker(LocalDate.now());
        DatePicker to = new DatePicker(LocalDate.now());
        TableView<AttendanceRecord> table = new TableView<>();
        table.getColumns().add(column("Name", "userName"));
        table.getColumns().add(column("Date", "date"));
        table.getColumns().add(column("Time", "time"));
        table.getColumns().add(column("Confidence", "confidence"));
        table.getColumns().add(column("Status", "status"));
        Label status = new Label();
        Button filter = new Button("Load");
        filter.setOnAction(event -> {
            try {
                List<AttendanceRecord> records = attendanceDAO.getRecordsByDateRange(from.getValue(), to.getValue());
                table.setItems(FXCollections.observableArrayList(records));
                status.setText(records.size() + " records");
            } catch (Exception exception) {
                status.setText(exception.getMessage());
            }
        });
        Button export = new Button("Export CSV");
        export.setOnAction(event -> exportCsv(table));
        Button exportPdf = new Button("Export PDF");
        exportPdf.setOnAction(event -> exportPdf(table));
        return new VBox(10, new javafx.scene.layout.HBox(10, new Label("From"), from, new Label("To"), to, filter, export, exportPdf), table, status);
    }

    private <T> TableColumn<AttendanceRecord, T> column(String title, String property) {
        TableColumn<AttendanceRecord, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    private void exportCsv(TableView<AttendanceRecord> table) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("attendance.csv");
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(new String[]{"Name", "Date", "Time", "Confidence", "Status"});
            for (AttendanceRecord record : table.getItems()) {
                writer.writeNext(new String[]{record.getUserName(), String.valueOf(record.getDate()),
                        String.valueOf(record.getTime()), String.valueOf(record.getConfidence()), record.getStatus()});
            }
        } catch (Exception exception) {
            showMessage(Alert.AlertType.ERROR, "Export failed", exception.getMessage());
        }
    }

    private void exportPdf(TableView<AttendanceRecord> table) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("attendance.pdf");
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.add(new Paragraph("FaceAttend Attendance Report"));
            Table report = new Table(5);
            for (String heading : new String[]{"Name", "Date", "Time", "Confidence", "Status"}) {
                report.addCell(new Cell().add(new Paragraph(heading)));
            }
            for (AttendanceRecord record : table.getItems()) {
                report.addCell(record.getUserName());
                report.addCell(String.valueOf(record.getDate()));
                report.addCell(String.valueOf(record.getTime()));
                report.addCell(String.valueOf(record.getConfidence()));
                report.addCell(record.getStatus());
            }
            document.add(report);
        } catch (Exception exception) {
            showMessage(Alert.AlertType.ERROR, "Export failed", exception.getMessage());
        }
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
