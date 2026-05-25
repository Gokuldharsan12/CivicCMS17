package com.civiccms.service;

import com.civiccms.entity.Complaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@civiccms.in}")
    private String fromEmail;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Complaint submitted confirmation ──────────────────────────────
    public void sendSubmissionConfirmation(Complaint complaint, String toEmail) {
        String subject = "CivicCMS: Complaint #" + complaint.getId() + " Submitted";
        String body = String.format(
            "Dear Citizen,\n\n" +
            "Your complaint has been successfully submitted.\n\n" +
            "Complaint ID : #%d\n" +
            "Title        : %s\n" +
            "Category     : %s\n" +
            "Status       : %s\n\n" +
            "We will notify you as the status changes.\n\n" +
            "CivicCMS Team",
            complaint.getId(), complaint.getTitle(),
            complaint.getCategory(), complaint.getStatus()
        );
        sendSafe(toEmail, subject, body);
    }

    // ── Status update notification ─────────────────────────────────────
    public void sendStatusUpdate(Complaint complaint, String toEmail) {
        String subject = "CivicCMS: Complaint #" + complaint.getId() + " — Status Updated";
        String body = String.format(
            "Dear Citizen,\n\n" +
            "The status of your complaint has been updated.\n\n" +
            "Complaint ID : #%d\n" +
            "Title        : %s\n" +
            "New Status   : %s\n\n" +
            "Thank you for using CivicCMS.\n\nCivicCMS Team",
            complaint.getId(), complaint.getTitle(), complaint.getStatus()
        );
        sendSafe(toEmail, subject, body);
    }

    // ── SLA escalation notice ──────────────────────────────────────────
    public void sendEscalated(Complaint complaint, String toEmail) {
        String subject = "CivicCMS: Complaint #" + complaint.getId() + " Escalated";
        String body = String.format(
            "Dear Citizen,\n\n" +
            "Complaint #%d ('%s') has been escalated because it exceeded the SLA deadline.\n\n" +
            "Priority has been raised to: %s\n\n" +
            "Our team is working to resolve this urgently.\n\nCivicCMS Team",
            complaint.getId(), complaint.getTitle(), complaint.getPriority()
        );
        sendSafe(toEmail, subject, body);
    }

    // ── Chaos / critical zone alert ────────────────────────────────────
    public void sendChaosAlert(String adminEmail, String zone, int count,
                               String category, String level) {
        String subject = "CivicCMS ALERT: " + level + " Zone Detected — " + zone;
        String body = String.format(
            "CHAOS DETECTION ALERT\n\n" +
            "Level    : %s\n" +
            "Zone     : %s\n" +
            "Complaints (last 1 hr): %d\n" +
            "Top Category: %s\n\n" +
            "Please review the Admin Dashboard immediately.\n\nCivicCMS System",
            level, zone, count, category
        );
        sendSafe(adminEmail, subject, body);
    }

    // ── Resolution confirmed ───────────────────────────────────────────
    public void sendResolutionNotice(Complaint complaint, String toEmail) {
        String subject = "CivicCMS: Complaint #" + complaint.getId() + " Resolved ✓";
        String body = String.format(
            "Dear Citizen,\n\n" +
            "We are glad to inform you that complaint #%d ('%s') has been resolved.\n\n" +
            "Resolution Note: %s\n\n" +
            "Please rate our service at: http://localhost:8080/rate.html?id=%d\n\n" +
            "Thank you for helping us improve the city!\n\nCivicCMS Team",
            complaint.getId(), complaint.getTitle(),
            complaint.getResolutionNote() != null ? complaint.getResolutionNote() : "N/A",
            complaint.getId()
        );
        sendSafe(toEmail, subject, body);
    }

    // ── Internal safe send (logs instead of crashing if mail not configured) ──
    private void sendSafe(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent to {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.warn("Email not sent to {} (mail not configured?): {}", to, e.getMessage());
        }
    }
}
