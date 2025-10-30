// src/main/java/org/example/finalprojs/repository/RedeemTransactionRepository.java

package org.example.finalprojs.repository;

import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RedeemTransactionRepository extends JpaRepository<RedeemTransaction, Long> {

    // Custom method to fetch history for a specific user, ordered by date
    List<RedeemTransaction> findByUserOrderByRedeemDateDesc(User user);
}