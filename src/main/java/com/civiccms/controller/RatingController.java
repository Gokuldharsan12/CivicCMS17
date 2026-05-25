package com.civiccms.controller;

import com.civiccms.entity.Complaint;
import com.civiccms.entity.Rating;
import com.civiccms.repository.ComplaintRepository;
import com.civiccms.repository.RatingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RatingController
 *
 *  POST /api/v1/ratings/{complaintId}   — citizen submits a rating for a resolved complaint
 *  GET  /api/v1/ratings                 — public: list all ratings (feedback wall)
 *  GET  /api/v1/ratings/stats           — public: aggregate stats (avg scores, total count)
 *  GET  /api/v1/ratings/{complaintId}   — public: get rating for a specific complaint
 */
@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController {

    private final RatingRepository    ratingRepo;
    private final ComplaintRepository complaintRepo;

    public RatingController(RatingRepository ratingRepo,
                            ComplaintRepository complaintRepo) {
        this.ratingRepo    = ratingRepo;
        this.complaintRepo = complaintRepo;
    }

    // ── POST /api/v1/ratings/general — general feedback (no complaint) ──
    @PostMapping("/general")
    public ResponseEntity<?> submitGeneralFeedback(
            @RequestBody Map<String, Object> body) {
        try {
            Rating r = new Rating();
            r.setComplaint(null);
            r.setSubmitterName(strVal(body, "submitterName", "Anonymous"));
            r.setSpeedScore(        intVal(body, "speedRating",         intVal(body, "speed",   3)));
            r.setQualityScore(      intVal(body, "qualityRating",       intVal(body, "quality", 3)));
            r.setCommunicationScore(intVal(body, "communicationRating", intVal(body, "comm",    3)));
            r.setComments(strVal(body, "comments", ""));

            Rating saved = ratingRepo.save(r);
            return ResponseEntity.ok(toMap(saved));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/v1/ratings/{complaintId} ────────────────────────────
    @PostMapping("/{complaintId}")
    public ResponseEntity<?> submitRating(
            @PathVariable Long complaintId,
            @RequestBody Map<String, Object> body) {
        try {
            // Prevent double-rating
            if (ratingRepo.existsByComplaintId(complaintId)) {
                return ResponseEntity.status(409)
                        .body(Map.of("error", "You have already rated this complaint."));
            }

            Complaint complaint = complaintRepo.findById(complaintId)
                    .orElse(null);
            if (complaint == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "Complaint not found."));
            }
            if (complaint.getStatus() != Complaint.ComplaintStatus.RESOLVED) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Only resolved complaints can be rated."));
            }

            Rating r = new Rating();
            r.setComplaint(complaint);
            r.setSpeedScore(        intVal(body, "speedRating",         intVal(body, "speed",   3)));
            r.setQualityScore(      intVal(body, "qualityRating",       intVal(body, "quality", 3)));
            r.setCommunicationScore(intVal(body, "communicationRating", intVal(body, "comm",    3)));
            r.setComments(strVal(body, "comments", ""));

            Rating saved = ratingRepo.save(r);
            return ResponseEntity.ok(toMap(saved));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/ratings ───────────────────────────────────────────
    // Public feedback wall — all ratings with complaint info & comments
    @GetMapping
    public ResponseEntity<?> listRatings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Rating> all = ratingRepo.findAll();

            // Sort newest first
            all.sort(Comparator.comparing(Rating::getSubmittedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));

            // Simple client-side pagination slice
            int from  = Math.min(page * size, all.size());
            int to    = Math.min(from + size, all.size());
            List<Map<String, Object>> content = all.subList(from, to)
                    .stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "content",       content,
                "totalElements", all.size(),
                "totalPages",    (int) Math.ceil((double) all.size() / size),
                "number",        page,
                "size",          size
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/ratings/stats ──────────────────────────────────────
    // Aggregate stats for the feedback wall header
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        try {
            List<Rating> all = ratingRepo.findAll();
            if (all.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "totalRatings", 0,
                    "avgOverall",   0.0,
                    "avgSpeed",     0.0,
                    "avgQuality",   0.0,
                    "avgComm",      0.0,
                    "distribution", Map.of("1",0,"2",0,"3",0,"4",0,"5",0)
                ));
            }

            double avgSpeed   = all.stream().mapToInt(Rating::getSpeedScore).average().orElse(0);
            double avgQuality = all.stream().mapToInt(Rating::getQualityScore).average().orElse(0);
            double avgComm    = all.stream().mapToInt(Rating::getCommunicationScore).average().orElse(0);
            double avgOverall = (avgSpeed + avgQuality + avgComm) / 3.0;

            // Star distribution based on overall average per rating
            Map<String, Long> dist = new LinkedHashMap<>();
            for (int i = 1; i <= 5; i++) {
                final int star = i;
                long count = all.stream().filter(r -> {
                    double avg = (r.getSpeedScore() + r.getQualityScore() + r.getCommunicationScore()) / 3.0;
                    return Math.round(avg) == star;
                }).count();
                dist.put(String.valueOf(i), count);
            }

            return ResponseEntity.ok(Map.of(
                "totalRatings", all.size(),
                "avgOverall",   Math.round(avgOverall * 10.0) / 10.0,
                "avgSpeed",     Math.round(avgSpeed   * 10.0) / 10.0,
                "avgQuality",   Math.round(avgQuality * 10.0) / 10.0,
                "avgComm",      Math.round(avgComm    * 10.0) / 10.0,
                "distribution", dist
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/ratings/{complaintId} ─────────────────────────────
    @GetMapping("/{complaintId}")
    public ResponseEntity<?> getRating(@PathVariable Long complaintId) {
        return ratingRepo.findByComplaintId(complaintId)
                .<ResponseEntity<?>>map(r -> ResponseEntity.ok(toMap(r)))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "No rating found.")));
    }

    // ── Helper: entity → map ──────────────────────────────────────────
    private Map<String, Object> toMap(Rating r) {
        Complaint c = r.getComplaint();
        double avgScore = (r.getSpeedScore() + r.getQualityScore() + r.getCommunicationScore()) / 3.0;

        String displayName = "Anonymous";
        if (c != null && c.getSubmittedBy() != null) {
            displayName = c.getSubmittedBy().getName();
        } else if (r.getSubmitterName() != null && !r.getSubmitterName().isBlank()) {
            displayName = r.getSubmitterName();
        }

        return Map.ofEntries(
            Map.entry("id",                 r.getId()),
            Map.entry("speedScore",         r.getSpeedScore()),
            Map.entry("qualityScore",       r.getQualityScore()),
            Map.entry("communicationScore", r.getCommunicationScore()),
            Map.entry("avgScore",           Math.round(avgScore * 10.0) / 10.0),
            Map.entry("comments",           r.getComments() != null ? r.getComments() : ""),
            Map.entry("submittedAt",        r.getSubmittedAt() != null ? r.getSubmittedAt().toString() : ""),
            Map.entry("complaintId",        c != null ? c.getId()       : 0L),
            Map.entry("complaintTitle",     c != null ? c.getTitle()    : ""),
            Map.entry("complaintCategory",  c != null ? c.getCategory() : ""),
            Map.entry("complaintDept",      c != null && c.getAssignedDept() != null
                                              ? c.getAssignedDept().getName() : ""),
            Map.entry("submittedByName",    displayName)
        );
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
    private String strVal(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }
}
