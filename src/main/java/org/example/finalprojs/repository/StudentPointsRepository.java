package org.example.finalprojs.repository;

import org.example.finalprojs.model.StudentPoints;
import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentPointsRepository extends JpaRepository<StudentPoints, Long> {

    // Find a student's points for a specific subject + section
    Optional<StudentPoints> findByUserAndSubjectAndSection(User user, String subject, String section);

    // Find all point records for a student (used for cascade delete)
    List<StudentPoints> findByUser(User user);
}