# Project Requirement Document (PRD)
**Project Name:** Automated Facial Recognition Attendance System  

---

## 1. Executive Summary
The Automated Facial Recognition Attendance System is designed to modernize and streamline the process of taking attendance in educational institutions. By leveraging advanced facial recognition technology, the system replaces manual roll calls, saving valuable instruction time and reducing errors. Teachers can upload images of their classrooms, and the system automatically identifies students and marks their attendance status.

## 2. Project Objectives
*   **Automate Attendance Tracking:** Eliminate manual attendance procedures using AI-driven facial recognition.
*   **Improve Accuracy:** Reduce human errors and proxy attendance.
*   **Time Efficiency:** Save teachers' time during classes by enabling batch processing of classroom photos.
*   **Data Insights:** Provide detailed attendance records and insights to faculty and administration.

## 3. Scope of Work
### 3.1 In-Scope
*   A web-based interface for Teachers and Administrators.
*   Student profile creation and face embedding enrollment.
*   Creation and management of class sessions.
*   Uploading classroom photos for automated processing.
*   Manual review and override interface for ambiguous or undetected faces.
*   Reporting and viewing of attendance records.

### 3.2 Out-of-Scope
*   Real-time video stream processing (attendance is based on uploaded photos).
*   Mobile application (currently a responsive web app).

## 4. User Personas
*   **Administrator (Admin):** Manages the overall system, teacher accounts, and core configuration settings.
*   **Teacher:** Creates attendance sessions, uploads classroom photos, reviews automated attendance results, and finalizes records.
*   **Student (Passive User):** Their data and facial embeddings are stored in the system, but they do not actively log in or interact with the platform.

## 5. Functional Requirements

### 5.1 User & Role Management
*   **FR-1.1:** The system shall support role-based access control (Admin, Teacher).
*   **FR-1.2:** Secure login authentication for all users.

### 5.2 Student Enrollment & Management
*   **FR-2.1:** The system shall allow the addition of student records including Roll Number, Name, Course, Branch, Year, and Division.
*   **FR-2.2:** The system shall support uploading base photos for students to generate and store facial embeddings for future recognition.

### 5.3 Session Management
*   **FR-3.1:** Teachers shall be able to create a new attendance session specifying the Course, Branch, Year, Division, Subject, Date, and Time.
*   **FR-3.2:** Sessions shall have statuses such as `OPEN` and `FINALIZED`.

### 5.4 Face Detection & Attendance Processing
*   **FR-4.1:** Teachers shall be able to upload one or multiple photos to an `OPEN` session.
*   **FR-4.2:** The system shall process uploaded photos to detect faces.
*   **FR-4.3:** The system shall compare detected faces against stored student embeddings.
*   **FR-4.4:** The system shall automatically mark a student as `PRESENT` if a positive match meets a predefined confidence threshold.

### 5.5 Manual Override and Review
*   **FR-5.1:** The system shall provide an interface for teachers to review processed images with bounding boxes around detected faces.
*   **FR-5.2:** Teachers shall be able to manually assign a student to an unrecognized or misidentified face.
*   **FR-5.3:** The system shall flag "ambiguous" matches (low confidence) for mandatory manual review.

### 5.6 Insights and Reporting
*   **FR-6.1:** The system shall generate tabular attendance reports for finalized sessions.
*   **FR-6.2:** The system shall provide visual insights and statistics (e.g., overall attendance percentages).

## 6. Non-Functional Requirements
*   **Performance:** The face recognition service must process an image and return results within 5-10 seconds to ensure a smooth user experience.
*   **Security:** Face embeddings and student data must be stored securely. User passwords must be hashed.
*   **Scalability:** The system must be capable of storing thousands of student embeddings and handling concurrent photo uploads.
*   **Usability:** The UI must be intuitive, particularly the manual review interface for face bounding boxes.

## 7. Assumptions
*   Classroom photos uploaded by teachers will be of sufficient lighting and quality for the AI models to detect faces.
*   Every student will have at least one clear baseline photo enrolled in the system.
