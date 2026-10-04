package com.example.attendance.repository;

import com.example.attendance.entity.FaceDetection;
import com.example.attendance.entity.SessionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FaceDetectionRepository extends JpaRepository<FaceDetection, Long> {
    List<FaceDetection> findBySessionPhotoIn(List<SessionPhoto> sessionPhotos);
    
    @Transactional
    void deleteBySessionPhotoIn(List<SessionPhoto> sessionPhotos);
}
