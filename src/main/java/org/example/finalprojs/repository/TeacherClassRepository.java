package org.example.finalprojs.repository;

import org.example.finalprojs.model.TeacherClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {
    List<TeacherClass> findByTeacherId(Long teacherId);
    List<TeacherClass> findBySection(String section);

    Optional<TeacherClass> findBySubjectAndSection(String subject, String section);
}