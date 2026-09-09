package com.faceattend.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

/**
 * Trains and applies the LBPH (Local Binary Pattern Histogram) recognizer
 * to identify a detected face against the enrolled dataset.
 *
 * TODO (Copilot):
 *  1. Implement train(MatVector faceSamples, Mat labels) which builds/updates
 *     the LBPH model from every enrolled user's stored face images.
 *  2. Implement predict(Mat face) -> returns {label, confidence}. Lower LBPH
 *     confidence values mean a *better* match; pick and document a threshold
 *     (e.g. < 70) below which you accept the match as PRESENT.
 *  3. Implement save()/load() so the trained model persists between runs
 *     instead of retraining on every app start.
 */
public class FaceRecognitionService {

    private final LBPHFaceRecognizer recognizer;
    private static final double CONFIDENCE_THRESHOLD = 70.0; // tune during testing

    public FaceRecognitionService() {
        this.recognizer = LBPHFaceRecognizer.create();
    }

    public void train(MatVector faceSamples, Mat labels) {
        recognizer.train(faceSamples, labels);
    }

    /**
     * @return the predicted user label, or -1 if confidence is above threshold (unknown/low confidence)
     */
    public int predict(Mat face) {
        int[] label = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(face, label, confidence);

        if (confidence[0] > CONFIDENCE_THRESHOLD) {
            return -1; // treat as unknown -> triggers manual verification per synopsis
        }
        return label[0];
    }

    public void save(String path) {
        recognizer.save(path);
    }

    public void load(String path) {
        recognizer.read(path);
    }
}
