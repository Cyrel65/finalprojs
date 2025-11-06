package org.example.finalprojs.repository;

import org.example.finalprojs.model.Score;
import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    /**
     * Finds all Score records associated with a specific User,
     * ordered alphabetically by subject name. This is used by your controller.
     */
    List<Score> findByUserOrderBySubjectAsc(User user);

    List<Score> findByUserAndSubject(User user, String subject);
}