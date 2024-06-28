package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<HostNotification, String > {
}
