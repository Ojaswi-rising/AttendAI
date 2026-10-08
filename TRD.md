# Technical Requirement Document (TRD)
**Project Name:** Automated Facial Recognition Attendance System  

---

## 1. Introduction
This document outlines the technical architecture, technology stack, database design, and component interactions required to build and deploy the Automated Facial Recognition Attendance System.

## 2. System Architecture
The application employs a decoupled, service-oriented architecture comprising two primary components:
1.  **Core Web Backend:** Handles HTTP requests, user sessions, database interactions, business logic, and UI rendering.
2.  **Face Inference Service:** An independent microservice dedicated to running deep learning models for face detection and embedding extraction.

This separation of concerns allows the resource-intensive AI models to scale independently from the core web service.

## 3. Technology Stack

### 3.1 Backend Application
*   **Language:** Java 17
*   **Framework:** Spring Boot 3.3.4
*   **Web & UI:** Spring Web, Thymeleaf (Server-Side UI Rendering)
*   **Security:** Spring Security (Authentication, Role-Based Access Control)
*   **Data Access:** Spring Data JPA, Hibernate ORM
*   **Build Tool:** Maven

### 3.2 Face Inference Service
*   **Language:** Python 3
*   **Framework:** FastAPI with Uvicorn server
*   **AI/CV Dependencies:**
    *   `insightface`: State-of-the-art 2D and 3D face analysis library.
    *   `onnxruntime`: High-performance inference engine for ML models.
    *   `opencv-python-headless`: Image processing and transformations.
    *   `numpy`: Numerical computations.

### 3.3 Database & Storage
*   **Relational Database:** MySQL 8.x
*   **File Storage:** Local File System (Configured via `upload.dir` for storing session and enrollment images).

## 4. Database Schema
The MySQL database (`attendance_db`) is designed with the following core entities:

*   **`users`**: `id`, `username`, `password_hash`, `role` (ADMIN, TEACHER).
*   **`students`**: `id`, `roll_no`, `name`, `course`, `branch`, `year`, `division`.
*   **`face_embeddings`**: `id`, `student_id` (FK), `embedding` (BLOB), `image_path`.
*   **`attendance_sessions`**: `id`, `course`, `branch`, `year`, `division`, `subject`, `session_date`, `session_time`, `teacher_id` (FK), `status`.
*   **`session_photos`**: `id`, `session_id` (FK), `image_path`.
*   **`face_detections`**: `id`, `session_photo_id` (FK), Bounding Box coords (`x1`, `y1`, `x2`, `y2`), `student_id` (FK, nullable), `confidence`, `matched` (boolean), `manual_override` (boolean), `ambiguous` (boolean).
*   **`attendance`**: `id`, `session_id` (FK), `student_id` (FK), `status` (PRESENT/ABSENT), `confidence`, `method` (AUTO/MANUAL), `marked_at`.

## 5. API & Component Integration

### 5.1 Communication Flow
1.  The Java backend receives an image upload from the user via the Thymeleaf UI.
2.  The Java backend saves the image locally and triggers an HTTP POST request to the Python Face Service.
3.  The Python Face Service analyzes the image and returns a JSON payload containing:
    *   Bounding box coordinates for all detected faces.
    *   A serialized 512-dimensional feature vector (embedding) for each face.
4.  The Java backend parses this JSON, compares the returned embeddings against stored embeddings in the MySQL database (using mathematical distance metrics like Cosine Similarity or Euclidean Distance), and logs the matches.

### 5.2 Face Service API Endpoints
*   `POST /extract_faces`: Accepts a multipart form-data image and returns a list of faces (bounding boxes + embeddings).

## 6. Security Requirements
*   **Password Management:** BCrypt hashing for all user passwords.
*   **Route Protection:** Web routes are protected by Spring Security based on user roles.
*   **Data Validation:** Spring Boot Validation ensures integrity of incoming data (e.g., student details, session parameters).

## 7. Infrastructure & Deployment
*   **Containerization:** The Python Face Service is containerized using Docker (`Dockerfile` provided), ensuring environment consistency for OpenCV and ONNX dependencies.
*   **Upload Size Limits:** Spring Boot is configured to handle large payloads:
    *   `spring.servlet.multipart.max-file-size=20MB`
    *   `spring.servlet.multipart.max-request-size=50MB`
*   **Port Configuration:**
    *   Java Backend: Port 7860 (configurable via `${PORT}`).
    *   Face Service: Configurable via `${FACE_SERVICE_URL}`.
