package com.example.attendance.dto;

import java.time.LocalDate;

public interface RecordDTO {
    LocalDate getSessionDate();
    String getSubject();
    String getCourse();
    String getYear();
    String getDivision();
    String getRollNo();
    String getStudentName();
    String getStatus();
    Double getConfidence();
    String getMethod();
}
