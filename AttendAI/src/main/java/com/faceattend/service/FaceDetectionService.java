package com.faceattend.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

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
    }

    /**
     * @param frame a BGR video frame captured from the webcam
     * @return bounding boxes of all detected faces in this frame
     */
    public RectVector detectFaces(Mat frame) {
        RectVector faces = new RectVector();
        // TODO: convert frame to grayscale, equalizeHist, then:
        // faceCascade.detectMultiScale(grayFrame, faces);
        return faces;
    }
}
