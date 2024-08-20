package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<HostNotification, Long> {

    // Find notifications by hostUserEmail and where read is false
    List<HostNotification> findByHostUserEmailAndReadFalse(String hostUserEmail);

    // Find all notifications by hostUserEmail
    List<HostNotification> findByHostUserEmail(String hostUserEmail);
}

