package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import com.checkmk.pdctLifeCycle.model.HostUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<HostNotification, Long > {
    List<HostNotification> findByUserAndReadFalse(HostUser user);
    List<HostNotification> findByUser(HostUser user);
}
