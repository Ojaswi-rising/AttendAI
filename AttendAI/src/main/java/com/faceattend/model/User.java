package com.faceattend.model;

/**
 * Represents a person enrolled in the attendance system
 * (a student or an employee, depending on deployment context).
 */
public class User {

    private int id;
    private String userCode;   // roll no. / employee id
    private String name;
    private String classOrDept;
    private String photoSamplePath; // folder where enrolled face samples are stored

    public User() {
    }

    public User(int id, String userCode, String name, String classOrDept, String photoSamplePath) {
        this.id = id;
        this.userCode = userCode;
        this.name = name;
        this.classOrDept = classOrDept;
        this.photoSamplePath = photoSamplePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassOrDept() {
        return classOrDept;
    }

    public void setClassOrDept(String classOrDept) {
        this.classOrDept = classOrDept;
    }

    public String getPhotoSamplePath() {
        return photoSamplePath;
    }

    public void setPhotoSamplePath(String photoSamplePath) {
        this.photoSamplePath = photoSamplePath;
    }

    @Override
    public String toString() {
        return name + " (" + userCode + ")";
    }
}
