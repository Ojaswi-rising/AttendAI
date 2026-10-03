package com.example.attendance.repository;

import com.example.attendance.entity.SessionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionPhotoRepository extends JpaRepository<SessionPhoto, Long> {
    List<SessionPhoto> findBySessionId(Long sessionId);
}
