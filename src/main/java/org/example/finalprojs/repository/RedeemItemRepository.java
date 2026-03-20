package org.example.finalprojs.repository;

import org.example.finalprojs.model.RedeemItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RedeemItemRepository extends JpaRepository<RedeemItem, Long> {

    // OLD: used for subject-only filtering (kept for compatibility)
    List<RedeemItem> findBySubject(String subject);

    // NEW: filters by both subject AND section
    // Only returns rewards the student's section is allowed to claim
    List<RedeemItem> findBySubjectAndSection(String subject, String section);
}