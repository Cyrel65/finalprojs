package org.example.finalprojs.repository;

import org.example.finalprojs.model.RedeemItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedeemItemRepository extends JpaRepository<RedeemItem, Long> {
    // add custom query methods if needed
}
