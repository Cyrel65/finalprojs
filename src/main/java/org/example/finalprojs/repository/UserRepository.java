package org.example.finalprojs.repository;

import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // 👈 Import Optional

@Repository
// JpaRepository provides automatic CRUD operations for the User entity (Long is the type of the Primary Key)
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA implements this method to find a User by email.
    Optional<User> findByEmail(String email);
}