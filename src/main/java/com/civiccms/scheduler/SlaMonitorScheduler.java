package com.civiccms.scheduler;

import com.civiccms.entity.Complaint;
import com.civiccms.entity.Complaint.ComplaintStatus;
import com.civiccms.entity.Complaint.ComplaintPriority;
import com.civiccms.repository.ComplaintRepository;
import com.civiccms.service.EmailNotificationService;
import com.civiccms.sse.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SlaMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitorScheduler.class);

    private final ComplaintRepository    complaintRepository;
    private final EmailNotificationService emailService;
    private final SsePublisher           ssePublisher;

    public SlaMonitorScheduler(ComplaintRepository complaintRepository,
                               EmailNotificationService emailService,
                               SsePublisher ssePublisher) {
        this.complaintRepository = complaintRepository;
        this.emailService        = emailService;
        this.ssePublisher        = ssePublisher;
    }

    @Scheduled(fixedRate = 3_600_000) // every hour
    @Transactional
    public void checkSlaBreaches() {
        List<Complaint> overdue = complaintRepository.findOverdueComplaints(LocalDateTime.now());
        if (overdue.isEmpty()) return;

        log.warn("SLA Monitor: {} overdue complaint(s) found", overdue.size());

        for (Complaint c : overdue) {
            c.setStatus(ComplaintStatus.ESCALATED);

            // If overdue by more than 2x the SLA — bump priority
            if (isDoublyOverdue(c)) {
                c.setPriority(boostPriority(c.getPriority()));
                log.warn("Complaint #{} priority bumped to {}", c.getId(), c.getPriority());
            }

            complaintRepository.save(c);

            // Notify citizen and admin
            String citizenEmail = c.getSubmittedBy() != null
                    ? c.getSubmittedBy().getEmail() : null;
            if (citizenEmail != null) {
                emailService.sendEscalated(c, citizenEmail);
            }
        }

        ssePublisher.broadcast("sla_breach",
            String.format("{\"count\":%d}", overdue.size()));

        log.info("SLA Monitor: escalated {} complaint(s)", overdue.size());
    }

    private boolean isDoublyOverdue(Complaint c) {
        if (c.getDueDate() == null) return false;
        long slaDuration = switch (c.getPriority()) {
            case CRITICAL -> 24;
            case HIGH     -> 48;
            case MEDIUM   -> 72;
            case LOW      -> 168; // 7 days
        };
        LocalDateTime doubleDeadline = c.getDueDate().minusHours(slaDuration);
        return LocalDateTime.now().isAfter(doubleDeadline);
    }

    private ComplaintPriority boostPriority(ComplaintPriority p) {
        return switch (p) {
            case LOW    -> ComplaintPriority.MEDIUM;
            case MEDIUM -> ComplaintPriority.HIGH;
            case HIGH, CRITICAL -> ComplaintPriority.CRITICAL;
        };
    }
}
