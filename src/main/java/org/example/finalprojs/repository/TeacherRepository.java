package org.example.finalprojs.repository;

import org.example.finalprojs.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    // Method needed for looking up the recipient when sending a message
    Optional<Teacher> findByEmail(String email);
}