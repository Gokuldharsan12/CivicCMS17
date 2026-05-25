package com.civiccms.controller;

import com.civiccms.entity.Complaint;
import com.civiccms.entity.Department;
import com.civiccms.repository.ComplaintRepository;
import com.civiccms.repository.DepartmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics endpoints:
 *   GET /api/v1/analytics/summary   — full dashboard analytics
 *   GET /api/heatmap                — lat/lng grouped heatmap data
 *   GET /api/complaints/stats       — compact stats for api-test page
 */
@RestController
public class AnalyticsController {

    private final ComplaintRepository  complaintRepo;
    private final DepartmentRepository deptRepo;

    public AnalyticsController(ComplaintRepository complaintRepo,
                               DepartmentRepository deptRepo) {
        this.complaintRepo = complaintRepo;
        this.deptRepo      = deptRepo;
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/analytics/summary
    //  Full analytics used by admin/dashboard.html
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/api/v1/analytics/summary")
    public ResponseEntity<?> summary() {
        try {
            List<Complaint> all = complaintRepo.findAll();
            long total = all.size();

            // By Priority
            Map<String, Long> byPriority = new LinkedHashMap<>();
            byPriority.put("CRITICAL", 0L);
            byPriority.put("HIGH",     0L);
            byPriority.put("MEDIUM",   0L);
            byPriority.put("LOW",      0L);
            for (Complaint c : all) {
                String p = c.getPriority() != null ? c.getPriority().name() : "MEDIUM";
                byPriority.merge(p, 1L, Long::sum);
            }

            // By Status
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (Complaint.ComplaintStatus s : Complaint.ComplaintStatus.values()) {
                byStatus.put(s.name(), 0L);
            }
            for (Complaint c : all) {
                String s = c.getStatus() != null ? c.getStatus().name() : "SUBMITTED";
                byStatus.merge(s, 1L, Long::sum);
            }

            // By Department — list of {name, count}
            Map<String, Long> deptCounts = new LinkedHashMap<>();
            for (Complaint c : all) {
                String deptName = (c.getAssignedDept() != null)
                        ? c.getAssignedDept().getName()
                        : "Unassigned";
                deptCounts.merge(deptName, 1L, Long::sum);
            }
            List<Map<String, Object>> byDept = deptCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name",  e.getKey());
                        m.put("count", e.getValue());
                        return m;
                    })
                    .collect(Collectors.toList());

            // By Category
            Map<String, Long> byCategoryRaw = new LinkedHashMap<>();
            for (Complaint c : all) {
                String cat = c.getCategory() != null ? c.getCategory() : "OTHER";
                byCategoryRaw.merge(cat, 1L, Long::sum);
            }
            List<Map<String, Object>> byCategory = byCategoryRaw.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("name",  e.getKey());
                        m.put("count", e.getValue());
                        return m;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalAll",   total);
            result.put("byPriority", byPriority);
            result.put("byStatus",   byStatus);
            result.put("byDept",     byDept);
            result.put("byCategory", byCategory);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/heatmap
    //  Called by api-test.html and admin/heatmap.html
    //  Returns: [ { "lat": 11.12, "lng": 78.65, "count": 3, "category": "WATER" }, … ]
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/api/heatmap")
    public ResponseEntity<?> heatmap() {
        try {
            List<Complaint> all = complaintRepo.findAll();

            // Group by rounded lat/lng (2 decimal places ≈ 1 km grid)
            Map<String, Map<String, Object>> grid = new LinkedHashMap<>();
            for (Complaint c : all) {
                if (c.getLat() == null || c.getLng() == null) continue;
                double lat = Math.round(c.getLat() * 100.0) / 100.0;
                double lng = Math.round(c.getLng() * 100.0) / 100.0;
                String key = lat + "," + lng;
                grid.computeIfAbsent(key, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("lat",      lat);
                    m.put("lng",      lng);
                    m.put("count",    0L);
                    m.put("category", c.getCategory() != null ? c.getCategory() : "OTHER");
                    return m;
                });
                Map<String, Object> cell = grid.get(key);
                cell.put("count", ((Long) cell.get("count")) + 1L);
            }

            return ResponseEntity.ok(new ArrayList<>(grid.values()));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/complaints/stats
    //  Called by api-test.html — compact stats summary
    //  Returns: { "total": N, "resolved": N, "pending": N, "byCategory": {…}, "byUrgency": {…} }
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/api/complaints/stats")
    public ResponseEntity<?> complaintStats() {
        try {
            List<Complaint> all = complaintRepo.findAll();

            long total    = all.size();
            long resolved = all.stream()
                .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED)
                .count();
            long pending  = all.stream()
                .filter(c -> c.getStatus() != Complaint.ComplaintStatus.RESOLVED)
                .count();

            Map<String, Long> byCategory = new LinkedHashMap<>();
            for (Complaint c : all) {
                String cat = c.getCategory() != null ? c.getCategory() : "OTHER";
                byCategory.merge(cat, 1L, Long::sum);
            }

            Map<String, Long> byUrgency = new LinkedHashMap<>();
            byUrgency.put("CRITICAL", 0L);
            byUrgency.put("HIGH",     0L);
            byUrgency.put("MEDIUM",   0L);
            byUrgency.put("LOW",      0L);
            for (Complaint c : all) {
                String p = c.getPriority() != null ? c.getPriority().name() : "MEDIUM";
                byUrgency.merge(p, 1L, Long::sum);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total",      total);
            result.put("resolved",   resolved);
            result.put("pending",    pending);
            result.put("byCategory", byCategory);
            result.put("byUrgency",  byUrgency);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /api/v1/analytics/dept-category-matrix
    //  Returns each department with a count per category inside it.
    //  Used by the admin dashboard "Department Breakdown" table.
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/api/v1/analytics/dept-category-matrix")
    public ResponseEntity<?> deptCategoryMatrix() {
        try {
            List<Department> depts = deptRepo.findAll();
            List<Complaint>  all   = complaintRepo.findAll();

            // Categories in display order
            List<String> cats = List.of(
                "WATER","ELECTRICITY","ROAD","GARBAGE","DRAINAGE",
                "STREETLIGHT","PARK","NOISE","ENCROACHMENT","ANIMAL","OTHER"
            );

            List<Map<String, Object>> rows = depts.stream()
                .sorted(Comparator.comparing(Department::getId))
                .map(dept -> {
                    List<Complaint> dc = all.stream()
                        .filter(c -> c.getAssignedDept() != null &&
                                     c.getAssignedDept().getId().equals(dept.getId()))
                        .collect(Collectors.toList());

                    Map<String, Long> byCat = new LinkedHashMap<>();
                    for (String cat : cats) {
                        final String fc = cat;
                        byCat.put(cat, dc.stream()
                            .filter(c -> fc.equalsIgnoreCase(c.getCategory()))
                            .count());
                    }

                    long total    = dc.size();
                    long pending  = dc.stream().filter(c ->
                        c.getStatus() == Complaint.ComplaintStatus.SUBMITTED ||
                        c.getStatus() == Complaint.ComplaintStatus.ASSIGNED).count();
                    long resolved = dc.stream().filter(c ->
                        c.getStatus() == Complaint.ComplaintStatus.RESOLVED).count();
                    long critical = dc.stream().filter(c ->
                        c.getPriority() == Complaint.ComplaintPriority.CRITICAL).count();
                    long high     = dc.stream().filter(c ->
                        c.getPriority() == Complaint.ComplaintPriority.HIGH).count();

                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("deptId",   dept.getId());
                    row.put("deptName", dept.getName());
                    row.put("deptCode", dept.getCode());
                    row.put("total",    total);
                    row.put("pending",  pending);
                    row.put("resolved", resolved);
                    row.put("critical", critical);
                    row.put("high",     high);
                    row.put("resolutionRate", total > 0 ? Math.round(resolved * 100.0 / total) : 0);
                    row.put("byCategory", byCat);
                    return row;
                })
                .collect(Collectors.toList());

            // Unassigned complaints
            long unassigned = all.stream().filter(c -> c.getAssignedDept() == null).count();

            // Overall by-category totals
            Map<String, Long> totalByCat = new LinkedHashMap<>();
            for (String cat : cats) {
                final String fc = cat;
                totalByCat.put(cat, all.stream()
                    .filter(c -> fc.equalsIgnoreCase(c.getCategory()))
                    .count());
            }

            return ResponseEntity.ok(Map.of(
                "departments",  rows,
                "unassigned",   unassigned,
                "total",        all.size(),
                "categories",   cats,
                "totalByCat",   totalByCat
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    //  Returns per-department complaint counts broken down by status
    //  Used by admin dashboard department stats section
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/api/v1/analytics/by-department")
    public ResponseEntity<?> byDepartment() {
        try {
            List<Department> depts = deptRepo.findAll();
            List<Complaint>  all   = complaintRepo.findAll();

            List<Map<String, Object>> result = depts.stream()
                .sorted(Comparator.comparing(Department::getId))
                .map(dept -> {
                    List<Complaint> deptComplaints = all.stream()
                        .filter(c -> c.getAssignedDept() != null &&
                                     c.getAssignedDept().getId().equals(dept.getId()))
                        .collect(Collectors.toList());

                    long total     = deptComplaints.size();
                    long submitted = deptComplaints.stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.SUBMITTED).count();
                    long assigned  = deptComplaints.stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.ASSIGNED).count();
                    long inProg    = deptComplaints.stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.IN_PROGRESS).count();
                    long resolved  = deptComplaints.stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED).count();
                    long escalated = deptComplaints.stream().filter(c -> c.getStatus() == Complaint.ComplaintStatus.ESCALATED).count();
                    long critical  = deptComplaints.stream().filter(c -> c.getPriority() == Complaint.ComplaintPriority.CRITICAL).count();
                    long high      = deptComplaints.stream().filter(c -> c.getPriority() == Complaint.ComplaintPriority.HIGH).count();
                    long pending   = submitted + assigned;

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("deptId",    dept.getId());
                    m.put("deptName",  dept.getName());
                    m.put("deptCode",  dept.getCode());
                    m.put("total",     total);
                    m.put("pending",   pending);
                    m.put("inProgress", inProg);
                    m.put("resolved",  resolved);
                    m.put("escalated", escalated);
                    m.put("critical",  critical);
                    m.put("high",      high);
                    m.put("resolutionRate", total > 0 ? Math.round((resolved * 100.0) / total) : 0);
                    return m;
                })
                .collect(Collectors.toList());

            // Also include "Unassigned" count
            long unassigned = all.stream().filter(c -> c.getAssignedDept() == null).count();

            return ResponseEntity.ok(Map.of(
                "departments", result,
                "unassigned",  unassigned,
                "total",       all.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
