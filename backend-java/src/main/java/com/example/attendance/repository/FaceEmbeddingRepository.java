package com.example.attendance.repository;

import com.example.attendance.entity.FaceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaceEmbeddingRepository extends JpaRepository<FaceEmbedding, Long> {
    java.util.List<FaceEmbedding> findByStudentIdIn(java.util.List<Long> studentIds);
    FaceEmbedding findByStudentId(Long studentId);
}
