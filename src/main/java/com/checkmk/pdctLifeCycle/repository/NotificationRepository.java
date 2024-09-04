package com.checkmk.pdctLifeCycle.repository;

import com.checkmk.pdctLifeCycle.model.HostNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<HostNotification, Long> {

    List<HostNotification> findByHostUserEmailAndReadFalse(String hostUserEmail);

    List<HostNotification> findByHostUserEmailOrderByCreatedAtDesc(String hostUserEmail);

    List<HostNotification> findAllByOrderByCreatedAtDesc();


    List<HostNotification> findByHostUserEmailInOrderByCreatedAtDesc(List<String> hostUserEmails);

}


