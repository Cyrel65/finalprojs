package org.example.finalprojs.repository;

import org.example.finalprojs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Optional: get all users by section
    List<User> findBySection(String section);

    // Optional: find a specific user from a section
    Optional<User> findByEmailAndSection(String email, String section);
}
