package org.example.finalprojs.repository;

import org.example.finalprojs.model.GradeReport;
import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ROOT CAUSE FIX: The original file contained TWO public interface declarations,
// which caused Spring to register two beans of the same type and broke every
// class that tried to @Autowire it. Java only allows one top-level type per file.
// The first interface also referenced fields (classId, studentEmail, type) that
// do not exist on GradeReport. Both old interfaces are removed.
// This is now the single source of truth for GradeReportRepository.
@Repository
public interface GradeReportRepository extends JpaRepository<GradeReport, Long> {

    // Used by ClassService and GradeService to load one student's report
    Optional<GradeReport> findByUserAndSubject(User user, String subject);

    // Used by ClassService.deleteClass() to cascade-delete all grades for a subject
    void deleteBySubject(String subject);

    // Used by GradeService.getRankingsBySection() to fetch all reports for a subject
    List<GradeReport> findBySubject(String subject);

    // Used to fetch all reports belonging to one student
    List<GradeReport> findByUser(User user);
}