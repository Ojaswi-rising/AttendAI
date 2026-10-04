package com.example.attendance.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "face_detections")
public class FaceDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_photo_id", nullable = false)
    private SessionPhoto sessionPhoto;

    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "matched_roll_no")
    private String matchedRollNo;

    private Double confidence;
    private Boolean matched;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "manual_override")
    private Boolean manualOverride;

    @Column(name = "second_confidence")
    private Double secondConfidence;

    private Boolean ambiguous;

    @Column(name = "second_best_roll_no")
    private String secondBestRollNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SessionPhoto getSessionPhoto() {
        return sessionPhoto;
    }

    public void setSessionPhoto(SessionPhoto sessionPhoto) {
        this.sessionPhoto = sessionPhoto;
    }

    public Integer getX1() {
        return x1;
    }

    public void setX1(Integer x1) {
        this.x1 = x1;
    }

    public Integer getY1() {
        return y1;
    }

    public void setY1(Integer y1) {
        this.y1 = y1;
    }

    public Integer getX2() {
        return x2;
    }

    public void setX2(Integer x2) {
        this.x2 = x2;
    }

    public Integer getY2() {
        return y2;
    }

    public void setY2(Integer y2) {
        this.y2 = y2;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getMatchedRollNo() {
        return matchedRollNo;
    }

    public void setMatchedRollNo(String matchedRollNo) {
        this.matchedRollNo = matchedRollNo;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Boolean getManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(Boolean manualOverride) {
        this.manualOverride = manualOverride;
    }

    public Double getSecondConfidence() {
        return secondConfidence;
    }

    public void setSecondConfidence(Double secondConfidence) {
        this.secondConfidence = secondConfidence;
    }

    public Boolean getAmbiguous() {
        return ambiguous;
    }

    public void setAmbiguous(Boolean ambiguous) {
        this.ambiguous = ambiguous;
    }

    public String getSecondBestRollNo() {
        return secondBestRollNo;
    }

    public void setSecondBestRollNo(String secondBestRollNo) {
        this.secondBestRollNo = secondBestRollNo;
    }
}
