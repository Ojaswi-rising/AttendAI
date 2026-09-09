package com.faceattend.service;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wraps OpenCV's Haar Cascade Classifier (via JavaCV) to locate faces
 * in a video frame in real time.
 *
 * TODO (Copilot):
 *  1. Load "haarcascade_frontalface_default.xml" from src/main/resources/haarcascades
 *     (download it from the OpenCV GitHub repo's data/haarcascades folder).
 *  2. Implement detectFaces(Mat frame) to return bounding boxes for every face found.
 *  3. Convert the frame to grayscale and equalize the histogram before detection
 *     for better accuracy under varying lighting (as mentioned in the synopsis).
 */
public class FaceDetectionService {

    private final CascadeClassifier faceCascade;

    public FaceDetectionService(String cascadePath) {
        this.faceCascade = new CascadeClassifier(cascadePath);
        if (faceCascade.empty()) {
            throw new IllegalArgumentException("Unable to load face cascade: " + cascadePath);
        }
    }

    public FaceDetectionService() {
        this(resolveCascadePath());
    }

    /**
     * @param frame a BGR video frame captured from the webcam
     * @return bounding boxes of all detected faces in this frame
     */
    public RectVector detectFaces(Mat frame) {
        if (frame == null || frame.empty()) {
            return new RectVector();
        }

        Mat grayFrame = new Mat();
        RectVector faces = new RectVector();
        try {
            if (frame.channels() == 1) {
                frame.copyTo(grayFrame);
            } else {
                opencv_imgproc.cvtColor(frame, grayFrame, opencv_imgproc.COLOR_BGR2GRAY);
            }
            opencv_imgproc.equalizeHist(grayFrame, grayFrame);
            faceCascade.detectMultiScale(grayFrame, faces);
        } finally {
            grayFrame.close();
        }
        return faces;
    }

    private static String resolveCascadePath() {
        String resourceName = "/haarcascades/haarcascade_frontalface_default.xml";
        try (InputStream stream = FaceDetectionService.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new IllegalStateException("Missing cascade resource: " + resourceName);
            }
            Path tempFile = Files.createTempFile("faceattend-haarcascade-", ".xml");
            Files.copy(stream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare face cascade resource", exception);
        }
    }
}
