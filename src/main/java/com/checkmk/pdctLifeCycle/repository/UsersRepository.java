package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.HostUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<HostUser, String > {
}
