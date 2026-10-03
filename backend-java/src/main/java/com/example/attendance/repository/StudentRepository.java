package com.example.attendance.repository;

import com.example.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRollNoAndCourseAndBranchAndYearAndDivision(String rollNo, String course, String branch, String year, String division);
    java.util.List<Student> findByCourseAndBranchAndYearAndDivision(String course, String branch, String year, String division);
    java.util.List<Student> findByCourseAndYearAndDivision(String course, String year, String division);
}
