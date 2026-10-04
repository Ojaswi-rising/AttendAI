package com.example.attendance.repository;

import com.example.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRollNoAndCourseAndBranchAndYearAndDivision(String rollNo, String course, String branch, String year, String division);
    java.util.List<Student> findByCourseAndBranchAndYearAndDivision(String course, String branch, String year, String division);
    java.util.List<Student> findByCourseAndYearAndDivision(String course, String year, String division);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.course FROM Student s WHERE s.course IS NOT NULL ORDER BY s.course")
    java.util.List<String> findDistinctCourses();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.year FROM Student s WHERE s.year IS NOT NULL ORDER BY s.year")
    java.util.List<String> findDistinctYears();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s.division FROM Student s WHERE s.division IS NOT NULL ORDER BY s.division")
    java.util.List<String> findDistinctDivisions();
}
