package com.example.attendance.dto;

public class StudentInsightDTO {
    private String rollNo;
    private String name;
    private double attendancePercentage;
    private String trend; // "Improving", "Declining", "Stable"
    private int engagementScore;
    private String riskStatus; // "SAFE", "WARNING", "CRITICAL"

    // Getters and setters
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }

    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }

    public int getEngagementScore() { return engagementScore; }
    public void setEngagementScore(int engagementScore) { this.engagementScore = engagementScore; }

    public String getRiskStatus() { return riskStatus; }
    public void setRiskStatus(String riskStatus) { this.riskStatus = riskStatus; }
}
