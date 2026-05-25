package com.civiccms.scheduler;

import com.civiccms.entity.ChaosEvent;
import com.civiccms.entity.ChaosEvent.ChaosLevel;
import com.civiccms.entity.Complaint;
import com.civiccms.repository.ChaosEventRepository;
import com.civiccms.repository.ComplaintRepository;
import com.civiccms.service.EmailNotificationService;
import com.civiccms.sse.SsePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChaosDetectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChaosDetectorScheduler.class);

    @Value("${chaos.warning.threshold:5}")
    private int warningThreshold;

    @Value("${chaos.critical.threshold:15}")
    private int criticalThreshold;

    @Value("${chaos.zone.radius.degrees:0.005}")
    private double zoneRadius;

    // Admin email for CRITICAL alerts (use admin from DB in real app)
    private static final String ADMIN_EMAIL = "admin@civic.gov.in";

    private final ComplaintRepository    complaintRepository;
    private final ChaosEventRepository   chaosEventRepository;
    private final SsePublisher           ssePublisher;
    private final EmailNotificationService emailService;

    public ChaosDetectorScheduler(ComplaintRepository complaintRepository,
                                  ChaosEventRepository chaosEventRepository,
                                  SsePublisher ssePublisher,
                                  EmailNotificationService emailService) {
        this.complaintRepository  = complaintRepository;
        this.chaosEventRepository = chaosEventRepository;
        this.ssePublisher         = ssePublisher;
        this.emailService         = emailService;
    }

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    @Transactional
    public void detectChaos() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Complaint> recent = complaintRepository.findByCreatedAtAfter(oneHourAgo);
        if (recent.size() < 2) return;

        // Group complaints by 500m grid zone
        Map<String, List<Complaint>> zones = new HashMap<>();
        for (Complaint c : recent) {
            if (c.getLat() == null || c.getLng() == null) continue;
            String key = String.format("%.3f,%.3f",
                Math.round(c.getLat() / zoneRadius) * zoneRadius,
                Math.round(c.getLng() / zoneRadius) * zoneRadius);
            zones.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<Complaint>> entry : zones.entrySet()) {
            String zone = entry.getKey();
            List<Complaint> zoneComplaints = entry.getValue();
            int count = zoneComplaints.size();
            if (count < 2) continue;

            // Top category
            String topCategory = zoneComplaints.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("General");

            // Duplicate rate
            long dupes = zoneComplaints.stream().filter(c -> Boolean.TRUE.equals(c.getIsDuplicate())).count();
            double dupeRate = (double) dupes / count;

            // Determine level
            ChaosLevel level;
            if (count >= criticalThreshold || dupeRate > 0.5) {
                level = ChaosLevel.CRITICAL;
            } else if (count >= warningThreshold || dupeRate > 0.2) {
                level = ChaosLevel.WARNING;
            } else {
                continue; // below threshold
            }

            // Save event
            ChaosEvent event = new ChaosEvent();
            event.setZoneKey(zone);
            event.setComplaintCount(count);
            event.setTopCategory(topCategory);
            event.setLevel(level);
            chaosEventRepository.save(event);

            // SSE broadcast
            ssePublisher.broadcast("chaos_alert", event.toJson());

            // Email on CRITICAL
            if (level == ChaosLevel.CRITICAL) {
                emailService.sendChaosAlert(ADMIN_EMAIL, zone, count, topCategory, "CRITICAL");
            }

            log.warn("CHAOS {}: zone={} count={} category={}", level, zone, count, topCategory);
        }
    }
}
