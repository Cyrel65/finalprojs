package org.example.finalprojs.repository;

import org.example.finalprojs.model.RedeemTransaction;
import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedeemTransactionRepository extends JpaRepository<RedeemTransaction, Long> {

    List<RedeemTransaction> findByUserOrderByRedeemDateDesc(User user);

    List<RedeemTransaction> findAllByUserOrderByRedeemDateDesc(User user);

    // Added for cascade delete in StudentApiController
    List<RedeemTransaction> findByUser(User user);
}