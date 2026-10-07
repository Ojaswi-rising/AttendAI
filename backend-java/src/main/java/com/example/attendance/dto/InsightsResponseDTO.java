package com.example.attendance.dto;

import java.util.List;

public class InsightsResponseDTO {
    private List<StudentInsightDTO> studentInsights;
    private List<String> textInsights;
    private double overallAverage;
    
    // Getters and Setters
    public List<StudentInsightDTO> getStudentInsights() { return studentInsights; }
    public void setStudentInsights(List<StudentInsightDTO> studentInsights) { this.studentInsights = studentInsights; }

    public List<String> getTextInsights() { return textInsights; }
    public void setTextInsights(List<String> textInsights) { this.textInsights = textInsights; }

    public double getOverallAverage() { return overallAverage; }
    public void setOverallAverage(double overallAverage) { this.overallAverage = overallAverage; }
}
