package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.HostUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<HostUser, String> {
    Optional<HostUser> findByEmail(String email);  // Custom query method to find by email
}
