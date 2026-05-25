package com.civiccms.controller;

import com.civiccms.entity.Complaint;
import com.civiccms.entity.Complaint.ComplaintStatus;
import com.civiccms.entity.Department;
import com.civiccms.entity.User;
import com.civiccms.repository.ComplaintRepository;
import com.civiccms.repository.DepartmentRepository;
import com.civiccms.repository.UserRepository;
import com.civiccms.service.DeptComplaintService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintRepository  complaintRepo;
    private final UserRepository       userRepo;
    private final DepartmentRepository deptRepo;
    private final DeptComplaintService deptComplaintService;

    private static final String UPLOAD_DIR = "uploads/photos/";

    public ComplaintController(ComplaintRepository complaintRepo,
                               UserRepository userRepo,
                               DepartmentRepository deptRepo,
                               DeptComplaintService deptComplaintService) {
        this.complaintRepo        = complaintRepo;
        this.userRepo             = userRepo;
        this.deptRepo             = deptRepo;
        this.deptComplaintService = deptComplaintService;
        try { Files.createDirectories(Paths.get(UPLOAD_DIR)); } catch (IOException ignored) {}
    }

    // ── GET /api/v1/complaints/mine ──────────────────────────────────
    @GetMapping("/mine")
    public ResponseEntity<?> myComplaints() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            Optional<User> userOpt = userRepo.findByEmail(auth.getName());
            if (userOpt.isEmpty()) return ResponseEntity.ok(List.of());
            List<Map<String, Object>> result = complaintRepo
                .findBySubmittedById(userOpt.get().getId())
                .stream().map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/complaints?page=0&size=20 ───────────────────────
    @GetMapping
    public ResponseEntity<?> listComplaints(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Complaint> p = complaintRepo.findAll(pr);
            return ResponseEntity.ok(Map.of(
                "content",       p.getContent().stream().map(this::toMap).collect(Collectors.toList()),
                "totalElements", p.getTotalElements(),
                "totalPages",    p.getTotalPages(),
                "number",        p.getNumber(),
                "size",          p.getSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/complaints/{id} ──────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaint(@PathVariable Long id) {
        Optional<Complaint> opt = complaintRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Complaint not found"));
        }
        Complaint c = opt.get();

        // If a user is authenticated, enforce ownership for non-privileged roles
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            Optional<User> userOpt = userRepo.findByEmail(auth.getName());
            if (userOpt.isPresent()) {
                User currentUser = userOpt.get();
                boolean isPrivileged = currentUser.getRole() == User.Role.ADMIN
                                    || currentUser.getRole() == User.Role.DEPT_HEAD;
                if (!isPrivileged) {
                    if (c.getSubmittedBy() == null
                            || !c.getSubmittedBy().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("error", "Access denied: this complaint does not belong to you"));
                    }
                }
            }
        }
        return ResponseEntity.ok(toMap(c));
    }

    // ── GET /api/v1/complaints/track/{id} ───────────────────────────
    // Security: authenticated users may only track their own complaints.
    // Admins and dept-heads may track any complaint.
    @GetMapping("/track/{id}")
    public ResponseEntity<?> trackComplaint(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Optional<Complaint> opt = complaintRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Complaint not found"));
        }
        Complaint c = opt.get();

        Optional<User> userOpt = userRepo.findByEmail(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "User not found"));
        }
        User currentUser = userOpt.get();

        // Admins and dept-heads can see all complaints
        boolean isPrivileged = currentUser.getRole() == User.Role.ADMIN
                            || currentUser.getRole() == User.Role.DEPT_HEAD;
        if (!isPrivileged) {
            // Regular users may only track complaints they submitted
            if (c.getSubmittedBy() == null
                    || !c.getSubmittedBy().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied: this complaint does not belong to you"));
            }
        }

        return ResponseEntity.ok(toTrackMap(c));
    }

    // ── POST /api/v1/complaints (multipart/form-data) ────────────────
    @PostMapping(consumes = {"multipart/form-data", "application/x-www-form-urlencoded"})
    public ResponseEntity<?> createComplaintMultipart(
            @RequestParam("title")                                     String title,
            @RequestParam("description")                               String description,
            @RequestParam(value = "category",  defaultValue = "OTHER") String category,
            @RequestParam(value = "lat",       defaultValue = "11.0")  String latStr,
            @RequestParam(value = "lng",       defaultValue = "77.0")  String lngStr,
            @RequestParam(value = "address",   required = false)       String address,
            @RequestParam(value = "priority",  defaultValue = "MEDIUM") String priority,
            @RequestParam(value = "photo",     required = false)       MultipartFile photo) {
        try {
            Complaint c = new Complaint();
            c.setTitle(title);
            c.setDescription(description);
            c.setCategory(category.toUpperCase().trim());
            c.setStatus(ComplaintStatus.SUBMITTED);
            c.setPriority(Complaint.ComplaintPriority.valueOf(priority.toUpperCase()));
            c.setAddress(address != null ? address : "");

            // Link authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                userRepo.findByEmail(auth.getName()).ifPresent(c::setSubmittedBy);
            }

            // Parse lat/lng
            try { c.setLat(Double.parseDouble(latStr)); } catch (Exception e) { c.setLat(11.0); }
            try { c.setLng(Double.parseDouble(lngStr)); } catch (Exception e) { c.setLng(77.0); }

            // Auto-assign department by category code
            String catCode = c.getCategory();
            deptRepo.findByCode(catCode).ifPresentOrElse(
                dept -> { c.setAssignedDept(dept); c.setStatus(ComplaintStatus.ASSIGNED); },
                () -> deptRepo.findByCode("OTHER").ifPresent(c::setAssignedDept)
            );

            // Handle photo
            if (photo != null && !photo.isEmpty()) {
                String origName = photo.getOriginalFilename();
                String ext = (origName != null && origName.contains("."))
                    ? origName.substring(origName.lastIndexOf('.')) : ".jpg";
                String filename = UUID.randomUUID() + ext;
                Path dest = Paths.get(UPLOAD_DIR + filename);
                photo.transferTo(dest.toFile());
                c.setPhotoUrl("/uploads/photos/" + filename);
            }

            Complaint saved = complaintRepo.save(c);
            // Route to department-specific table
            deptComplaintService.routeComplaintToDept(saved);
            return ResponseEntity.ok(toMap(saved));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── PATCH /api/v1/complaints/{id}/status ────────────────────────
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return complaintRepo.findById(id).map(c -> {
            try {
                String newStatus = str(body, "status");
                if (newStatus != null) {
                    c.setStatus(ComplaintStatus.valueOf(newStatus.toUpperCase()));
                }
                String note = str(body, "resolutionNote");
                if (note != null) c.setResolutionNote(note);
                if (c.getStatus() == ComplaintStatus.RESOLVED && c.getResolvedAt() == null) {
                    c.setResolvedAt(LocalDateTime.now());
                }
                complaintRepo.save(c);
                // Sync to dept-specific table
                deptComplaintService.syncStatusUpdate(c);
                return ResponseEntity.<Object>ok(toMap(c));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.<Object>status(400)
                    .body(Map.of("error", "Invalid status: " + body.get("status")));
            }
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "Complaint not found")));
    }

    // ── GET /api/v1/complaints/by-dept/{deptId} ─────────────────────
    @GetMapping("/by-dept/{deptId}")
    public ResponseEntity<?> byDepartment(
            @PathVariable Long deptId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        try {
            ComplaintStatus cs = parseStatus(status);
            Complaint.ComplaintPriority cp = parsePriority(priority);
            List<Complaint> complaints = (cs == null && cp == null)
                ? complaintRepo.findByAssignedDeptId(deptId)
                : complaintRepo.filterComplaints(deptId, cs, cp);
            List<Map<String, Object>> result = complaints.stream()
                .map(this::toMap).collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("content", result, "total", result.size(), "deptId", deptId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/complaints/dept-head/mine ───────────────────────
    @GetMapping("/dept-head/mine")
    public ResponseEntity<?> deptHeadComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            User user = userRepo.findByEmail(auth.getName()).orElse(null);
            if (user == null || user.getRole() != User.Role.DEPT_HEAD
                    || user.getDepartmentId() == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            Long deptId = user.getDepartmentId();
            ComplaintStatus cs = parseStatus(status);
            Complaint.ComplaintPriority cp = parsePriority(priority);
            List<Complaint> complaints = (cs == null && cp == null)
                ? complaintRepo.findByAssignedDeptId(deptId)
                : complaintRepo.filterComplaints(deptId, cs, cp);

            long pending  = complaints.stream().filter(c ->
                c.getStatus() == ComplaintStatus.SUBMITTED ||
                c.getStatus() == ComplaintStatus.ASSIGNED).count();
            long inProg   = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
            long resolved = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
            long critical = complaints.stream().filter(c -> c.getPriority() == Complaint.ComplaintPriority.CRITICAL).count();

            List<Map<String, Object>> result = complaints.stream()
                .sorted((a, b) -> {
                    int pa = priorityOrder(a.getPriority()), pb = priorityOrder(b.getPriority());
                    if (pa != pb) return pa - pb;
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null)
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    return 0;
                })
                .map(this::toMap).collect(Collectors.toList());

            Department dept = deptRepo.findById(deptId).orElse(null);
            return ResponseEntity.ok(Map.of(
                "content",  result,
                "total",    result.size(),
                "deptId",   deptId,
                "deptName", dept != null ? dept.getName() : "",
                "stats", Map.of(
                    "pending", pending, "inProgress", inProg,
                    "resolved", resolved, "critical", critical)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── PATCH /api/v1/complaints/{id}/assign-dept ───────────────────
    @PatchMapping("/{id}/assign-dept")
    public ResponseEntity<?> assignDepartment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return complaintRepo.findById(id).map(c -> {
            try {
                Object deptIdObj = body.get("deptId");
                if (deptIdObj == null) {
                    return ResponseEntity.<Object>status(400).body(Map.of("error", "deptId is required"));
                }
                Long deptId = Long.parseLong(deptIdObj.toString());
                Department dept = deptRepo.findById(deptId).orElse(null);
                if (dept == null) {
                    return ResponseEntity.<Object>status(404).body(Map.of("error", "Department not found"));
                }
                c.setAssignedDept(dept);
                if (c.getStatus() == ComplaintStatus.SUBMITTED) {
                    c.setStatus(ComplaintStatus.ASSIGNED);
                }
                complaintRepo.save(c);
                // Route to dept-specific table on manual assignment too
                deptComplaintService.routeComplaintToDept(c);
                return ResponseEntity.<Object>ok(toMap(c));
            } catch (NumberFormatException e) {
                return ResponseEntity.<Object>status(400).body(Map.of("error", "Invalid deptId"));
            } catch (Exception e) {
                return ResponseEntity.<Object>status(500).body(Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.status(404).body(Map.of("error", "Complaint not found")));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private ComplaintStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return ComplaintStatus.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }

    private Complaint.ComplaintPriority parsePriority(String p) {
        if (p == null || p.isBlank()) return null;
        try { return Complaint.ComplaintPriority.valueOf(p.toUpperCase()); } catch (Exception e) { return null; }
    }

    private int priorityOrder(Complaint.ComplaintPriority p) {
        if (p == null) return 3;
        switch (p) {
            case CRITICAL: return 0;
            case HIGH:     return 1;
            case MEDIUM:   return 2;
            default:       return 3;
        }
    }

    private Map<String, Object> toMap(Complaint c) {
        return Map.ofEntries(
            Map.entry("id",               c.getId()),
            Map.entry("title",            c.getTitle() != null ? c.getTitle() : ""),
            Map.entry("description",      c.getDescription() != null ? c.getDescription() : ""),
            Map.entry("category",         c.getCategory() != null ? c.getCategory() : "OTHER"),
            Map.entry("status",           c.getStatus() != null ? c.getStatus().name() : "SUBMITTED"),
            Map.entry("priority",         c.getPriority() != null ? c.getPriority().name() : "MEDIUM"),
            Map.entry("lat",              c.getLat() != null ? c.getLat() : 0.0),
            Map.entry("lng",              c.getLng() != null ? c.getLng() : 0.0),
            Map.entry("address",          c.getAddress() != null ? c.getAddress() : ""),
            Map.entry("photoUrl",         c.getPhotoUrl() != null ? c.getPhotoUrl() : ""),
            Map.entry("isDuplicate",      c.getIsDuplicate() != null && c.getIsDuplicate()),
            Map.entry("duplicateCount",   c.getDuplicateCount() != null ? c.getDuplicateCount() : 0),
            Map.entry("resolutionNote",   c.getResolutionNote() != null ? c.getResolutionNote() : ""),
            Map.entry("assignedDeptName", c.getAssignedDept() != null ? c.getAssignedDept().getName() : ""),
            Map.entry("assignedDeptId",   c.getAssignedDept() != null ? c.getAssignedDept().getId() : 0L),
            Map.entry("submittedByName",  c.getSubmittedBy() != null ? c.getSubmittedBy().getName() : ""),
            Map.entry("createdAt",        c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""),
            Map.entry("updatedAt",        c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : ""),
            Map.entry("resolvedAt",       c.getResolvedAt() != null ? c.getResolvedAt().toString() : "")
        );
    }

    private Map<String, Object> toTrackMap(Complaint c) {
        return Map.ofEntries(
            Map.entry("id",               c.getId()),
            Map.entry("title",            c.getTitle() != null ? c.getTitle() : ""),
            Map.entry("description",      c.getDescription() != null ? c.getDescription() : ""),
            Map.entry("category",         c.getCategory() != null ? c.getCategory() : "OTHER"),
            Map.entry("status",           c.getStatus() != null ? c.getStatus().name() : "SUBMITTED"),
            Map.entry("priority",         c.getPriority() != null ? c.getPriority().name() : "MEDIUM"),
            Map.entry("lat",              c.getLat() != null ? c.getLat() : 0.0),
            Map.entry("lng",              c.getLng() != null ? c.getLng() : 0.0),
            Map.entry("address",          c.getAddress() != null ? c.getAddress() : ""),
            Map.entry("photoUrl",         c.getPhotoUrl() != null ? c.getPhotoUrl() : ""),
            Map.entry("isDuplicate",      c.getIsDuplicate() != null && c.getIsDuplicate()),
            Map.entry("duplicateCount",   c.getDuplicateCount() != null ? c.getDuplicateCount() : 0),
            Map.entry("resolutionNote",   c.getResolutionNote() != null ? c.getResolutionNote() : ""),
            Map.entry("assignedDeptName", c.getAssignedDept() != null ? c.getAssignedDept().getName() : ""),
            Map.entry("assignedDeptId",   c.getAssignedDept() != null ? c.getAssignedDept().getId() : 0L),
            Map.entry("submittedByName",  c.getSubmittedBy() != null ? c.getSubmittedBy().getName() : ""),
            Map.entry("createdAt",        c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""),
            Map.entry("updatedAt",        c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : ""),
            Map.entry("resolvedAt",       c.getResolvedAt() != null ? c.getResolvedAt().toString() : ""),
            Map.entry("dueDate",          c.getDueDate() != null ? c.getDueDate().toString() : ""),
            Map.entry("currentStage",     computeStage(c))
        );
    }

    private int computeStage(Complaint c) {
        if (c.getStatus() == null) return 1;
        int ai = c.getAiStage() != null ? c.getAiStage() : 1;
        switch (c.getStatus()) {
            case SUBMITTED:   return Math.max(1, Math.min(ai, 2));
            case ASSIGNED:    return 3;
            case IN_PROGRESS: return 4;
            case RESOLVED:    return 5;
            case ESCALATED:   return 4;
            case DUPLICATE:   return 2;
            default:          return 1;
        }
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
