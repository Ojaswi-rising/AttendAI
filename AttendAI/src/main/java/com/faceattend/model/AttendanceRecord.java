package com.faceattend.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A single attendance entry: one user, marked present on one date/time,
 * with the confidence score returned by the LBPH recognizer.
 */
public class AttendanceRecord {

    private int id;
    private int userId;
    private String userName;
    private LocalDate date;
    private LocalTime time;
    private double confidence;
    private String status; // "PRESENT", "UNKNOWN", "LOW_CONFIDENCE"

    public AttendanceRecord() {
    }

    public AttendanceRecord(int userId, String userName, LocalDate date, LocalTime time,
                             double confidence, String status) {
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.time = time;
        this.confidence = confidence;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
