# FaceAttend

**Smart Face Recognition-Based Attendance System** — a Java desktop application that marks attendance automatically using a webcam, face detection (Haar Cascade via JavaCV/OpenCV), and face recognition (LBPH).

> T.Y. BCA Final Year Project — built to remove manual roll-call, RFID cards, and contact-based fingerprint scanners.

## Features

- **Face Enrollment** — register a user by capturing multiple face samples via webcam
- **Real-Time Face Detection** — Haar Cascade Classifier detects faces in the live feed
- **Face Recognition** — LBPH matches a detected face against the enrolled dataset with a confidence score
- **Automatic Attendance Marking** — logs date/time once confidence clears a threshold, blocks duplicate same-day entries
- **Admin Dashboard** — login-gated screen to add/remove users and browse records
- **Live Monitoring View** — webcam preview with bounding boxes + name labels
- **Unknown/Low-Confidence Handling** — prompts manual verification instead of guessing
- **Reports & Export** — CSV/PDF export filtered by date, class, or individual

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Computer Vision | OpenCV via JavaCV/JavaCPP |
| Recognition Algorithm | LBPH (Local Binary Pattern Histogram) |
| GUI | JavaFX |
| Database | SQLite (default) / MySQL |
| Build Tool | Maven |
| Export | OpenCSV, iText7 (PDF) |

## Project Structure

```
FaceAttend/
├── pom.xml
├── db/
│   └── schema.sql              # run this once to create tables
├── data/faces/                 # enrolled face samples land here (gitignored)
└── src/main/
    ├── java/com/faceattend/
    │   ├── model/               # User, AttendanceRecord
    │   ├── dao/                 # UserDAO, AttendanceDAO  (JDBC)
    │   ├── service/             # FaceDetectionService, FaceRecognitionService
    │   ├── gui/                 # MainApp + JavaFX screens
    │   └── util/                # DBConnection
    └── resources/
        ├── haarcascades/        # put haarcascade_frontalface_default.xml here
        └── fxml/                # JavaFX layout files
```

## Getting Started

1. **Prerequisites**: JDK 17+, Maven, a working webcam.
2. Download `haarcascade_frontalface_default.xml` from the [OpenCV GitHub repo](https://github.com/opencv/opencv/tree/master/data/haarcascades) into `src/main/resources/haarcascades/`.
3. Initialize the database:
   ```bash
   sqlite3 db/face_attend.db < db/schema.sql
   ```
4. Run the app:
   ```bash
   mvn clean javafx:run
   ```

## Roadmap / TODOs

This repo currently contains the **project scaffold**: package structure, data models, DAO layer, and stubbed services with `TODO (Copilot)` comments marking exactly what to implement next (face detection logic, LBPH training/prediction, JavaFX screens). See `COPILOT_PROMPT.md` for a ready-to-use prompt to drive that implementation.

## Author

Ojaswi Asalkar — T.Y. BCA, Division A, Roll No. 02
Project Guide: Prof. Sandeep Vishwakarma
