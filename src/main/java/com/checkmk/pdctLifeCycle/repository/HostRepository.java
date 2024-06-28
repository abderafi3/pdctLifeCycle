package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HostRepository extends JpaRepository<Host, String> {
}
