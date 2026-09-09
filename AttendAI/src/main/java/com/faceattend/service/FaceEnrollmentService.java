package com.faceattend.service;

import com.faceattend.dao.UserDAO;
import com.faceattend.model.User;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMWRITE_PNG_COMPRESSION;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

public class FaceEnrollmentService {

    private static final int DEFAULT_SAMPLE_COUNT = 25;
    private final FaceDetectionService detectionService;
    private final UserDAO userDAO;
    private final Path facesDirectory;

    public FaceEnrollmentService(FaceDetectionService detectionService, UserDAO userDAO, Path facesDirectory) {
        this.detectionService = Objects.requireNonNull(detectionService);
        this.userDAO = Objects.requireNonNull(userDAO);
        this.facesDirectory = Objects.requireNonNull(facesDirectory);
    }

    public int captureSamples(User user) throws Exception {
        return captureSamples(user, DEFAULT_SAMPLE_COUNT);
    }

    public int captureSamples(User user, int sampleCount) throws Exception {
        validateUser(user, sampleCount);
        Path userDirectory = facesDirectory.resolve(user.getUserCode());
        Files.createDirectories(userDirectory);
        int captured = 0;

        try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
             OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat()) {
            grabber.start();
            while (captured < sampleCount) {
                Frame frame = grabber.grab();
                Mat image = converter.convert(frame);
                if (image == null || image.empty()) {
                    continue;
                }

                RectVector faces = detectionService.detectFaces(image);
                if (faces.size() == 0) {
                    continue;
                }

                Rect face = faces.get(0);
                Mat crop = new Mat(image, face);
                Mat grayCrop = new Mat();
                try {
                    if (crop.channels() == 1) {
                        crop.copyTo(grayCrop);
                    } else {
                        cvtColor(crop, grayCrop, COLOR_BGR2GRAY);
                    }
                    Path samplePath = userDirectory.resolve(String.format("sample-%03d.png", captured));
                    if (imwrite(samplePath.toString(), grayCrop, new int[]{IMWRITE_PNG_COMPRESSION, 3})) {
                        captured++;
                    }
                } finally {
                    grayCrop.close();
                    crop.close();
                    faces.close();
                }
            }
        }

        user.setPhotoSamplePath(userDirectory.toString());
        userDAO.addUser(user);
        return captured;
    }

    private void validateUser(User user, int sampleCount) {
        if (user == null || isBlank(user.getUserCode()) || isBlank(user.getName())) {
            throw new IllegalArgumentException("User name and user code are required");
        }
        if (sampleCount < 1) {
            throw new IllegalArgumentException("Sample count must be positive");
        }
        if (!user.getUserCode().matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("User code may contain only letters, numbers, '_' and '-'");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
