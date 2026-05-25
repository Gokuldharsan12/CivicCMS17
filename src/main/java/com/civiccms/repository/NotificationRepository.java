package com.civiccms.repository;

import com.civiccms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderBySentAtDesc(String email);
    List<Notification> findByComplaintId(Long complaintId);
}
