package com.faceattend.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

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
    private final Map<Integer, String> labelToUserCode = new LinkedHashMap<>();
    private static final double CONFIDENCE_THRESHOLD = 70.0; // tune during testing

    public FaceRecognitionService() {
        this.recognizer = LBPHFaceRecognizer.create();
    }

    public void train(MatVector faceSamples, Mat labels) {
        recognizer.train(faceSamples, labels);
    }

    public void train(Path facesDirectory) throws IOException {
        List<Path> userDirectories = Files.list(facesDirectory)
                .filter(Files::isDirectory)
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        List<Mat> samples = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();
        labelToUserCode.clear();

        for (int label = 0; label < userDirectories.size(); label++) {
            Path userDirectory = userDirectories.get(label);
            int currentLabel = label;
            labelToUserCode.put(label, userDirectory.getFileName().toString());
            try (var images = Files.list(userDirectory)) {
                images.filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                        .sorted()
                        .forEach(path -> {
                            Mat image = imread(path.toString(), IMREAD_GRAYSCALE);
                            if (!image.empty()) {
                                samples.add(image);
                                labels.add(currentLabel);
                            } else {
                                image.close();
                            }
                        });
            }
        }

        if (samples.isEmpty()) {
            throw new IllegalStateException("No face samples found in " + facesDirectory);
        }

        MatVector sampleVector = new MatVector(samples.toArray(Mat[]::new));
        Mat labelMat = new Mat(labels.size(), 1, CV_32SC1);
        IntBuffer labelBuffer = (IntBuffer) labelMat.createBuffer();
        labels.forEach(labelBuffer::put);
        recognizer.train(sampleVector, labelMat);
        samples.forEach(Mat::close);
        sampleVector.close();
        labelMat.close();
    }

    /**
     * @return the predicted user label, or -1 if confidence is above threshold (unknown/low confidence)
     */
    public int predict(Mat face) {
        return predictWithConfidence(face).label();
    }

    public Prediction predictWithConfidence(Mat face) {
        int[] label = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(face, label, confidence);

        if (confidence[0] > CONFIDENCE_THRESHOLD) {
            return new Prediction(-1, confidence[0]);
        }
        return new Prediction(label[0], confidence[0]);
    }

    public String getUserCode(int label) {
        return labelToUserCode.get(label);
    }

    public void save(String path) {
        recognizer.save(path);
        try {
            Path mappingPath = Path.of(path + ".labels");
            Files.write(mappingPath, labelToUserCode.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save recognition label mapping", exception);
        }
    }

    public void load(String path) {
        recognizer.read(path);
        labelToUserCode.clear();
        Path mappingPath = Path.of(path + ".labels");
        if (Files.exists(mappingPath)) {
            try {
                for (String line : Files.readAllLines(mappingPath)) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        labelToUserCode.put(Integer.parseInt(parts[0]), parts[1]);
                    }
                }
            } catch (IOException | NumberFormatException exception) {
                throw new IllegalStateException("Unable to load recognition label mapping", exception);
            }
        }
    }

    public record Prediction(int label, double confidence) {
    }
}
