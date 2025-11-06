package org.example.finalprojs.repository;

import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface GradeReportRepository extends JpaRepository<GradeReport, Long> {

    // Find the single grade report for a user and a specific subject
    Optional<GradeReport> findByUserAndSubject(User user, String subject);

    // Find all grade reports for a user
    List<GradeReport> findByUser(User user);
}