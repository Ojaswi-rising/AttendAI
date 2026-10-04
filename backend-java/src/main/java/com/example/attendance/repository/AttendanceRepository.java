package com.example.attendance.repository;

import com.example.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<Attendance> findBySessionId(Long sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT s.sessionDate AS sessionDate, s.subject AS subject, st.course AS course, st.year AS year, st.division AS division, " +
           "st.rollNo AS rollNo, st.name AS studentName, a.status AS status, a.confidence AS confidence, a.method AS method " +
           "FROM Attendance a " +
           "JOIN AttendanceSession s ON a.sessionId = s.id " +
           "JOIN Student st ON a.studentId = st.id " +
           "WHERE s.status = 'FINALIZED' " +
           "AND (:course IS NULL OR st.course = :course) " +
           "AND (:year IS NULL OR st.year = :year) " +
           "AND (:division IS NULL OR st.division = :division) " +
           "AND (:subject IS NULL OR LOWER(s.subject) LIKE LOWER(CONCAT('%', :subject, '%'))) " +
           "AND (cast(:fromDate as java.time.LocalDate) IS NULL OR s.sessionDate >= :fromDate) " +
           "AND (cast(:toDate as java.time.LocalDate) IS NULL OR s.sessionDate <= :toDate) " +
           "ORDER BY s.sessionDate DESC, st.rollNo ASC")
    List<com.example.attendance.dto.RecordDTO> findAttendanceRecords(
            @org.springframework.data.repository.query.Param("course") String course,
            @org.springframework.data.repository.query.Param("year") String year,
            @org.springframework.data.repository.query.Param("division") String division,
            @org.springframework.data.repository.query.Param("subject") String subject,
            @org.springframework.data.repository.query.Param("fromDate") java.time.LocalDate fromDate,
            @org.springframework.data.repository.query.Param("toDate") java.time.LocalDate toDate);
}
