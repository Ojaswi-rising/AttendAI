package com.faceattend.service;

import com.faceattend.dao.AttendanceDAO;
import com.faceattend.dao.UserDAO;
import com.faceattend.model.AttendanceRecord;
import com.faceattend.model.User;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

public class AttendanceMarkingService {

    private final FaceDetectionService detectionService;
    private final FaceRecognitionService recognitionService;
    private final AttendanceDAO attendanceDAO;
    private final Map<String, User> usersByCode;

    public AttendanceMarkingService(FaceDetectionService detectionService,
                                    FaceRecognitionService recognitionService,
                                    AttendanceDAO attendanceDAO,
                                    UserDAO userDAO) throws Exception {
        this.detectionService = Objects.requireNonNull(detectionService);
        this.recognitionService = Objects.requireNonNull(recognitionService);
        this.attendanceDAO = Objects.requireNonNull(attendanceDAO);
        usersByCode = new HashMap<>();
        for (User user : userDAO.getAllUsers()) {
            usersByCode.put(user.getUserCode(), user);
        }
    }

    public void monitor(Consumer<Mat> frameConsumer, BooleanSupplier running) throws Exception {
        try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
             OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat()) {
            grabber.start();
            while (running.getAsBoolean()) {
                Frame frame = grabber.grab();
                Mat image = converter.convert(frame);
                if (image == null || image.empty()) {
                    continue;
                }
                processFrame(image);
                frameConsumer.accept(image);
            }
        }
    }

    public void processFrame(Mat frame) throws Exception {
        RectVector faces = detectionService.detectFaces(frame);
        try {
            for (long index = 0; index < faces.size(); index++) {
                Rect face = faces.get(index);
                Mat crop = new Mat(frame, face);
                Mat grayCrop = new Mat();
                try {
                    if (crop.channels() == 1) {
                        crop.copyTo(grayCrop);
                    } else {
                        cvtColor(crop, grayCrop, COLOR_BGR2GRAY);
                    }
                    FaceRecognitionService.Prediction prediction = recognitionService.predictWithConfidence(grayCrop);
                    if (prediction.label() < 0) {
                        continue;
                    }
                    User user = usersByCode.get(recognitionService.getUserCode(prediction.label()));
                    if (user != null) {
                        AttendanceRecord record = new AttendanceRecord(user.getId(), user.getName(),
                                LocalDate.now(), LocalTime.now(), prediction.confidence(), "PRESENT");
                        attendanceDAO.markAttendanceIfNotAlready(record);
                    }
                } finally {
                    grayCrop.close();
                    crop.close();
                }
            }
        } finally {
            faces.close();
        }
    }

    @FunctionalInterface
    public interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
