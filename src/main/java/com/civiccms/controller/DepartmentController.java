package com.civiccms.controller;

import com.civiccms.entity.Department;
import com.civiccms.entity.User;
import com.civiccms.repository.DepartmentRepository;
import com.civiccms.repository.UserRepository;
import com.civiccms.service.DeptComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST endpoints for departments and dept-head account management.
 *
 *  GET  /api/v1/departments                             — list all departments
 *  GET  /api/v1/departments/{id}                        — single department
 *  GET  /api/v1/departments/{id}/head                   — get dept head user info
 *  POST /api/v1/departments/{id}/head                   — create/update dept head login
 *  GET  /api/v1/departments/heads                       — list all dept head accounts
 *  POST /api/v1/departments/sync-complaints             — admin: re-sync complaints into dept tables
 *  GET  /api/v1/departments/my-complaints               — dept-specific complaints (dept head only)
 *  GET  /api/v1/departments/my-stats                    — dept-specific stats   (dept head only)
 *  PATCH /api/v1/departments/my-complaints/{cid}/status — dept head updates status
 */
@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentRepository  deptRepo;
    private final UserRepository        userRepo;
    private final PasswordEncoder       passwordEncoder;
    private final DeptComplaintService  deptComplaintService;

    public DepartmentController(DepartmentRepository deptRepo,
                                UserRepository userRepo,
                                PasswordEncoder passwordEncoder,
                                DeptComplaintService deptComplaintService) {
        this.deptRepo             = deptRepo;
        this.userRepo             = userRepo;
        this.passwordEncoder      = passwordEncoder;
        this.deptComplaintService = deptComplaintService;
    }

    // ── GET /api/v1/departments ───────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listAll() {
        try {
            List<Department> all = deptRepo.findAll();
            List<Map<String, Object>> result = all.stream()
                .sorted(Comparator.comparing(Department::getId))
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",        d.getId());
                    m.put("name",      d.getName());
                    m.put("code",      d.getCode());
                    m.put("headEmail", d.getHeadEmail());
                    m.put("hasLogin",  userRepo.findByDepartmentId(d.getId()).isPresent());
                    return m;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/departments/{id} ─────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return deptRepo.findById(id)
            .<ResponseEntity<?>>map(d -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        d.getId());
                m.put("name",      d.getName());
                m.put("code",      d.getCode());
                m.put("headEmail", d.getHeadEmail());
                m.put("keywords",  d.getKeywordsCsv());
                m.put("hasLogin",  userRepo.findByDepartmentId(d.getId()).isPresent());
                return ResponseEntity.ok(m);
            })
            .orElse(ResponseEntity.status(404).body(Map.of("error", "Department not found")));
    }

    // ── GET /api/v1/departments/{id}/head ────────────────────────────
    @GetMapping("/{id}/head")
    public ResponseEntity<?> getDeptHead(@PathVariable Long id) {
        return userRepo.findByDepartmentId(id)
            .<ResponseEntity<?>>map(head -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           head.getId());
                m.put("name",         head.getName());
                m.put("email",        head.getEmail());
                m.put("departmentId", head.getDepartmentId());
                m.put("role",         head.getRole().name());
                return ResponseEntity.ok(m);
            })
            .orElse(ResponseEntity.status(404).body(Map.of("error", "No dept head for this department")));
    }

    // ── POST /api/v1/departments/{id}/head ───────────────────────────
    @PostMapping("/{id}/head")
    public ResponseEntity<?> createOrUpdateDeptHead(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Department dept = deptRepo.findById(id).orElse(null);
            if (dept == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Department not found"));
            }
            String name     = str(body, "name");
            String email    = str(body, "email");
            String password = str(body, "password");

            if (email == null || email.isBlank()) {
                return ResponseEntity.status(400).body(Map.of("error", "email is required"));
            }

            Optional<User> existing = userRepo.findByDepartmentId(id);
            User head;
            if (existing.isPresent()) {
                head = existing.get();
            } else {
                Optional<User> byEmail = userRepo.findByEmail(email);
                if (byEmail.isPresent() &&
                    !Objects.equals(byEmail.get().getDepartmentId(), id)) {
                    return ResponseEntity.status(409)
                        .body(Map.of("error", "Email already in use by another account"));
                }
                head = byEmail.orElse(new User());
            }

            if (name != null && !name.isBlank()) head.setName(name);
            head.setEmail(email);
            head.setRole(User.Role.DEPT_HEAD);
            head.setDepartmentId(id);
            head.setProvider("LOCAL");
            if (password != null && !password.isBlank()) {
                head.setPasswordHash(passwordEncoder.encode(password));
            } else if (head.getPasswordHash() == null) {
                head.setPasswordHash(passwordEncoder.encode(dept.getCode() + "@123"));
            }
            userRepo.save(head);

            dept.setHeadEmail(email);
            deptRepo.save(dept);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Department head account created/updated",
                "email",   email,
                "defaultPassword",
                (password == null || password.isBlank()) ? dept.getCode() + "@123" : "(as set)"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/v1/departments/heads ────────────────────────────────
    @GetMapping("/heads")
    public ResponseEntity<?> listAllHeads() {
        try {
            List<User> heads = userRepo.findByRole(User.Role.DEPT_HEAD);
            List<Map<String, Object>> result = heads.stream().map(h -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           h.getId());
                m.put("name",         h.getName());
                m.put("email",        h.getEmail());
                m.put("departmentId", h.getDepartmentId());
                if (h.getDepartmentId() != null) {
                    deptRepo.findById(h.getDepartmentId()).ifPresent(d -> {
                        m.put("deptName", d.getName());
                        m.put("deptCode", d.getCode());
                    });
                }
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/v1/departments/sync-complaints
     * Admin utility: manually re-syncs all complaints into dept-specific tables.
     * Also fixes complaints where assigned_dept_id is NULL but category matches a dept code.
     * Call this once from the admin panel if dept dashboards show no data.
     */
    @PostMapping("/sync-complaints")
    public ResponseEntity<?> syncComplaints() {
        try {
            deptComplaintService.migrateExistingComplaints();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Complaint sync completed. Check server logs for row counts."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    //  DEPARTMENT-SPECIFIC COMPLAINT ENDPOINTS  (dept head only)
    // ════════════════════════════════════════════════════════════════

    /**
     * GET /api/v1/departments/my-complaints?status=&priority=
     * Returns complaints from THIS dept head's department-specific table only.
     */
    @GetMapping("/my-complaints")
    public ResponseEntity<?> myDeptComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        try {
            User user = currentDeptHead();
            if (user == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied — not a department head"));
            }
            Department dept = deptRepo.findById(user.getDepartmentId()).orElse(null);
            if (dept == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Department not found"));
            }
            List<Map<String, Object>> complaints =
                deptComplaintService.getComplaintsForDept(dept.getCode(), status, priority);
            Map<String, Object> stats = deptComplaintService.getStatsForDept(dept.getCode());
            return ResponseEntity.ok(Map.of(
                "content",  complaints,
                "total",    complaints.size(),
                "deptId",   dept.getId(),
                "deptName", dept.getName(),
                "deptCode", dept.getCode(),
                "stats",    stats
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/departments/my-stats
     * Quick stats for the logged-in dept head's department table.
     */
    @GetMapping("/my-stats")
    public ResponseEntity<?> myDeptStats() {
        try {
            User user = currentDeptHead();
            if (user == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            Department dept = deptRepo.findById(user.getDepartmentId()).orElse(null);
            if (dept == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Department not found"));
            }
            Map<String, Object> stats = deptComplaintService.getStatsForDept(dept.getCode());
            stats.put("deptName", dept.getName());
            stats.put("deptCode", dept.getCode());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/v1/departments/my-complaints/{complaintId}/status
     * Dept head updates status — writes to BOTH dept table AND master complaints table.
     */
    @PatchMapping("/my-complaints/{complaintId}/status")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable Long complaintId,
            @RequestBody Map<String, Object> body) {
        try {
            User user = currentDeptHead();
            if (user == null) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            Department dept = deptRepo.findById(user.getDepartmentId()).orElse(null);
            if (dept == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Department not found"));
            }
            String newStatus = str(body, "status");
            String note      = str(body, "resolutionNote");
            if (newStatus == null || newStatus.isBlank()) {
                return ResponseEntity.status(400).body(Map.of("error", "status is required"));
            }
            // Update dept-specific table
            deptComplaintService.updateStatusInDeptTable(dept.getCode(), complaintId, newStatus, note);
            // Also sync master complaints table
            deptComplaintService.updateMasterComplaint(complaintId, newStatus, note);

            return ResponseEntity.ok(Map.of(
                "success",     true,
                "complaintId", complaintId,
                "newStatus",   newStatus,
                "deptCode",    dept.getCode(),
                "message",     "Status updated in " + dept.getName() + " database"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /** Returns the logged-in User if they are a DEPT_HEAD, otherwise null. */
    private User currentDeptHead() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        User user = userRepo.findByEmail(auth.getName()).orElse(null);
        if (user == null || user.getRole() != User.Role.DEPT_HEAD
                || user.getDepartmentId() == null) {
            return null;
        }
        return user;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString().trim() : null;
    }
}
