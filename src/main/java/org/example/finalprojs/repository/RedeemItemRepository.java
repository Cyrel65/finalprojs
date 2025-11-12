package org.example.finalprojs.repository;

import org.example.finalprojs.model.RedeemItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RedeemItemRepository extends JpaRepository<RedeemItem, Long> {
    // Find all redeemable items specific to one subject
    List<RedeemItem> findBySubject(String subject);
}