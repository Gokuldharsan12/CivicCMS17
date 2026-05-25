package com.civiccms.service;

import com.civiccms.entity.Complaint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DeptComplaintService — department-wise complaint routing.
 *
 * Every complaint saved in the master complaints table is also
 * mirrored into dept_complaints_<CODE> (e.g. dept_complaints_water).
 * Dept heads query only their own table.
 */
@Service
public class DeptComplaintService {

    private static final Logger log = LoggerFactory.getLogger(DeptComplaintService.class);
    private final JdbcTemplate jdbc;

    private static final Set<String> VALID_CODES = Set.of(
        "WATER", "ELECTRICITY", "ROAD", "GARBAGE", "DRAINAGE",
        "STREETLIGHT", "PARK", "NOISE", "ENCROACHMENT", "ANIMAL", "OTHER"
    );

    public DeptComplaintService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String tableFor(String code) {
        if (code == null || !VALID_CODES.contains(code.toUpperCase())) code = "OTHER";
        return "dept_complaints_" + code.toLowerCase();
    }

    /** Insert/update complaint in its department-specific table. */
    public void routeComplaintToDept(Complaint saved) {
        if (saved.getAssignedDept() == null) return;
        String table = tableFor(saved.getAssignedDept().getCode());
        String submitterName = saved.getSubmittedBy() != null
            ? saved.getSubmittedBy().getName() : "Anonymous";
        try {
            String sql =
                "INSERT INTO " + table +
                " (complaint_id,title,description,status,priority,address," +
                "  photo_url,submitted_by_name,resolution_note,resolved_at)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?)" +
                " ON DUPLICATE KEY UPDATE" +
                "  status=VALUES(status)," +
                "  resolution_note=VALUES(resolution_note)," +
                "  resolved_at=VALUES(resolved_at)," +
                "  updated_at=CURRENT_TIMESTAMP";
            jdbc.update(sql,
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStatus() != null ? saved.getStatus().name() : "ASSIGNED",
                saved.getPriority() != null ? saved.getPriority().name() : "MEDIUM",
                saved.getAddress() != null ? saved.getAddress() : "",
                saved.getPhotoUrl() != null ? saved.getPhotoUrl() : "",
                submitterName,
                saved.getResolutionNote() != null ? saved.getResolutionNote() : "",
                saved.getResolvedAt()
            );
            log.info("Complaint #{} routed to {}", saved.getId(), table);
        } catch (Exception e) {
            log.error("Failed to route complaint #{} to {}: {}", saved.getId(), table, e.getMessage());
        }
    }

    /** Sync status change to the dept-specific table. */
    public void syncStatusUpdate(Complaint updated) {
        if (updated.getAssignedDept() == null) return;
        String table = tableFor(updated.getAssignedDept().getCode());
        try {
            String sql =
                "UPDATE " + table +
                " SET status=?,resolution_note=?,resolved_at=?,updated_at=CURRENT_TIMESTAMP" +
                " WHERE complaint_id=?";
            int rows = jdbc.update(sql,
                updated.getStatus() != null ? updated.getStatus().name() : "ASSIGNED",
                updated.getResolutionNote() != null ? updated.getResolutionNote() : "",
                updated.getResolvedAt(),
                updated.getId()
            );
            if (rows == 0) {
                routeComplaintToDept(updated);
            }
        } catch (Exception e) {
            log.error("Failed to sync status for #{} in {}: {}", updated.getId(), table, e.getMessage());
        }
    }

    /** Fetch complaints from a dept-specific table with optional filters.
     *  Falls back to querying the master complaints table if the dept table is empty,
     *  ensuring dept heads always see their data even before routing runs. */
    public List<Map<String, Object>> getComplaintsForDept(String deptCode, String status, String priority) {
        String table = tableFor(deptCode);
        StringBuilder sql = new StringBuilder(
            "SELECT d.id,d.complaint_id,d.title,d.description,d.status,d.priority," +
            "d.address,d.photo_url,d.submitted_by_name,d.resolution_note," +
            "d.resolved_at,d.created_at,d.updated_at,c.lat,c.lng,c.category,c.ai_stage" +
            " FROM " + table + " d LEFT JOIN complaints c ON c.id=d.complaint_id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND d.status=?");
            params.add(status.toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND d.priority=?");
            params.add(priority.toUpperCase());
        }
        sql.append(" ORDER BY FIELD(d.priority,'CRITICAL','HIGH','MEDIUM','LOW'),d.created_at DESC");
        try {
            List<Map<String, Object>> results = jdbc.queryForList(sql.toString(), params.toArray());

            // ── FALLBACK FIX: If dept table is empty, query master complaints table directly ──
            // This handles the case where complaints exist but weren't routed to the dept table
            // (e.g. category code mismatch or migration hasn't run yet).
            if (results.isEmpty()) {
                log.info("Dept table {} is empty — falling back to master complaints table for code={}", table, deptCode);
                results = queryMasterComplaintsForDept(deptCode, status, priority);
                // Opportunistically back-fill dept table with the found complaints
                backFillDeptTable(deptCode, results);
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to fetch from {}: {}", table, e.getMessage());
            // Even if the dept table fails completely, try the master table
            try {
                return queryMasterComplaintsForDept(deptCode, status, priority);
            } catch (Exception ex) {
                log.error("Master table fallback also failed: {}", ex.getMessage());
                return new ArrayList<>();
            }
        }
    }

    /**
     * Fallback: query the master complaints table filtered by department code.
     * Matches on departments.code so even if routing failed, dept heads see their data.
     */
    private List<Map<String, Object>> queryMasterComplaintsForDept(String deptCode, String status, String priority) {
        String normalizedCode = (deptCode == null) ? "OTHER" : deptCode.toUpperCase();
        StringBuilder sql = new StringBuilder(
            "SELECT c.id AS complaint_id, c.id, c.title, c.description, c.status, c.priority," +
            " c.address, c.photo_url, u.name AS submitted_by_name, c.resolution_note," +
            " c.resolved_at, c.created_at, c.updated_at, c.lat, c.lng, c.category, c.ai_stage" +
            " FROM complaints c" +
            " LEFT JOIN departments d ON d.id = c.assigned_dept_id" +
            " LEFT JOIN users u ON u.id = c.submitted_by_id" +
            " WHERE (d.code = ? OR c.category = ?)"
        );
        List<Object> params = new ArrayList<>();
        params.add(normalizedCode);
        params.add(normalizedCode);
        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status = ?");
            params.add(status.toUpperCase());
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND c.priority = ?");
            params.add(priority.toUpperCase());
        }
        sql.append(" ORDER BY FIELD(c.priority,'CRITICAL','HIGH','MEDIUM','LOW'), c.created_at DESC");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Back-fill the dept table with complaints found in master table.
     * Runs silently — errors are logged but do not fail the response.
     */
    private void backFillDeptTable(String deptCode, List<Map<String, Object>> masterRows) {
        if (masterRows.isEmpty()) return;
        String table = tableFor(deptCode);
        int filled = 0;
        for (Map<String, Object> row : masterRows) {
            try {
                Object complaintId = row.get("complaint_id");
                if (complaintId == null) complaintId = row.get("id");
                jdbc.update(
                    "INSERT IGNORE INTO " + table +
                    " (complaint_id,title,description,status,priority,address," +
                    "  photo_url,submitted_by_name,resolution_note,resolved_at,created_at)" +
                    " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    complaintId,
                    row.getOrDefault("title", ""),
                    row.getOrDefault("description", ""),
                    row.getOrDefault("status", "ASSIGNED"),
                    row.getOrDefault("priority", "MEDIUM"),
                    row.getOrDefault("address", ""),
                    row.getOrDefault("photo_url", ""),
                    row.getOrDefault("submitted_by_name", "Anonymous"),
                    row.getOrDefault("resolution_note", ""),
                    row.get("resolved_at"),
                    row.get("created_at")
                );
                filled++;
            } catch (Exception e) {
                log.debug("Back-fill skipped for row: {}", e.getMessage());
            }
        }
        if (filled > 0) log.info("Back-filled {} complaints into {}", filled, table);
    }

    /** Stats summary (total/pending/inProgress/resolved/critical) for a dept table.
     *  Falls back to master complaints table if dept-specific table is empty. */
    public Map<String, Object> getStatsForDept(String deptCode) {
        String table = tableFor(deptCode);
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            // Check if dept table has any rows first
            Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
            if (count != null && count > 0) {
                String sql =
                    "SELECT COUNT(*) AS total," +
                    " SUM(status IN ('SUBMITTED','ASSIGNED')) AS pending," +
                    " SUM(status='IN_PROGRESS') AS in_progress," +
                    " SUM(status='RESOLVED') AS resolved," +
                    " SUM(priority='CRITICAL') AS critical" +
                    " FROM " + table;
                Map<String, Object> row = jdbc.queryForMap(sql);
                stats.put("total",      row.getOrDefault("total", 0L));
                stats.put("pending",    row.getOrDefault("pending", 0L));
                stats.put("inProgress", row.getOrDefault("in_progress", 0L));
                stats.put("resolved",   row.getOrDefault("resolved", 0L));
                stats.put("critical",   row.getOrDefault("critical", 0L));
            } else {
                // Fallback: count from master complaints table
                String normalizedCode = (deptCode == null) ? "OTHER" : deptCode.toUpperCase();
                String fallbackSql =
                    "SELECT COUNT(*) AS total," +
                    " SUM(c.status IN ('SUBMITTED','ASSIGNED')) AS pending," +
                    " SUM(c.status='IN_PROGRESS') AS in_progress," +
                    " SUM(c.status='RESOLVED') AS resolved," +
                    " SUM(c.priority='CRITICAL') AS critical" +
                    " FROM complaints c" +
                    " LEFT JOIN departments d ON d.id = c.assigned_dept_id" +
                    " WHERE d.code = ? OR c.category = ?";
                Map<String, Object> row = jdbc.queryForMap(fallbackSql, normalizedCode, normalizedCode);
                stats.put("total",      row.getOrDefault("total", 0L));
                stats.put("pending",    row.getOrDefault("pending", 0L));
                stats.put("inProgress", row.getOrDefault("in_progress", 0L));
                stats.put("resolved",   row.getOrDefault("resolved", 0L));
                stats.put("critical",   row.getOrDefault("critical", 0L));
            }
        } catch (Exception e) {
            log.error("Failed to get stats for {}: {}", table, e.getMessage());
            stats.put("total", 0L); stats.put("pending", 0L);
            stats.put("inProgress", 0L); stats.put("resolved", 0L); stats.put("critical", 0L);
        }
        return stats;
    }

    /** Update status in dept-specific table (called by dept head). */
    public boolean updateStatusInDeptTable(String deptCode, Long complaintId, String status, String note) {
        String table = tableFor(deptCode);
        try {
            LocalDateTime resolvedAt = "RESOLVED".equalsIgnoreCase(status) ? LocalDateTime.now() : null;
            int rows = jdbc.update(
                "UPDATE " + table +
                " SET status=?,resolution_note=?,resolved_at=?,updated_at=CURRENT_TIMESTAMP" +
                " WHERE complaint_id=?",
                status != null ? status.toUpperCase() : "ASSIGNED",
                note != null ? note : "",
                resolvedAt,
                complaintId
            );
            return rows > 0;
        } catch (Exception e) {
            log.error("updateStatusInDeptTable failed for #{} in {}: {}", complaintId, table, e.getMessage());
            return false;
        }
    }

    /** Update the master complaints table (called alongside dept table update). */
    public boolean updateMasterComplaint(Long complaintId, String status, String note) {
        try {
            LocalDateTime resolvedAt = "RESOLVED".equalsIgnoreCase(status) ? LocalDateTime.now() : null;
            int rows = jdbc.update(
                "UPDATE complaints SET status=?,resolution_note=?,resolved_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                status != null ? status.toUpperCase() : "ASSIGNED",
                note != null ? note : "",
                resolvedAt,
                complaintId
            );
            return rows > 0;
        } catch (Exception e) {
            log.error("updateMasterComplaint failed for #{}: {}", complaintId, e.getMessage());
            return false;
        }
    }

    /**
     * Run on startup — creates dept tables and back-fills existing complaints.
     * Fully idempotent.
     * FIX: Also routes complaints where assigned_dept_id IS NULL but category matches a dept code.
     */
    public void migrateExistingComplaints() {
        try {
            ensureDeptTablesExist();

            // ── Step 1: Fix complaints with NULL assigned_dept_id but matching category ──
            // This repairs complaints submitted before department routing was set up.
            try {
                int fixed = jdbc.update(
                    "UPDATE complaints c " +
                    "JOIN departments d ON d.code = c.category " +
                    "SET c.assigned_dept_id = d.id, c.status = CASE WHEN c.status = 'SUBMITTED' THEN 'ASSIGNED' ELSE c.status END " +
                    "WHERE c.assigned_dept_id IS NULL AND c.category IS NOT NULL"
                );
                if (fixed > 0) log.info("Fixed {} complaints: linked to department by category code.", fixed);
            } catch (Exception e) {
                log.warn("Could not auto-fix unassigned complaints: {}", e.getMessage());
            }

            // ── Step 2: Back-fill dept tables from master complaints ──
            String sql =
                "SELECT c.id,c.title,c.description,c.status,c.priority," +
                "c.address,c.photo_url,c.resolution_note,c.resolved_at,c.created_at," +
                "d.code AS dept_code,u.name AS submitter_name" +
                " FROM complaints c" +
                " LEFT JOIN departments d ON d.id=c.assigned_dept_id" +
                " LEFT JOIN users u ON u.id=c.submitted_by_id" +
                " WHERE c.assigned_dept_id IS NOT NULL";
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            int migrated = 0;
            for (Map<String, Object> row : rows) {
                String table = tableFor((String) row.get("dept_code"));
                try {
                    jdbc.update(
                        "INSERT IGNORE INTO " + table +
                        " (complaint_id,title,description,status,priority,address," +
                        "  photo_url,submitted_by_name,resolution_note,resolved_at,created_at)" +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                        row.get("id"), row.get("title"), row.get("description"),
                        row.getOrDefault("status", "ASSIGNED"),
                        row.getOrDefault("priority", "MEDIUM"),
                        row.getOrDefault("address", ""),
                        row.getOrDefault("photo_url", ""),
                        row.getOrDefault("submitter_name", "Anonymous"),
                        row.getOrDefault("resolution_note", ""),
                        row.get("resolved_at"), row.get("created_at")
                    );
                    migrated++;
                } catch (Exception ex) {
                    log.warn("Skipped complaint #{}: {}", row.get("id"), ex.getMessage());
                }
            }
            log.info("Migrated {} existing complaints into dept-specific tables.", migrated);
        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage());
        }
    }

    private void ensureDeptTablesExist() {
        String[] codes = {
            "water","electricity","road","garbage","drainage",
            "streetlight","park","noise","encroachment","animal","other"
        };
        for (String code : codes) {
            String tbl = "dept_complaints_" + code;
            try {
                jdbc.execute(
                    "CREATE TABLE IF NOT EXISTS " + tbl + " (" +
                    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  complaint_id BIGINT NOT NULL UNIQUE," +
                    "  title VARCHAR(255) NOT NULL," +
                    "  description TEXT NOT NULL," +
                    "  status ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED'," +
                    "  priority ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM'," +
                    "  address VARCHAR(300)," +
                    "  photo_url VARCHAR(500)," +
                    "  submitted_by_name VARCHAR(180)," +
                    "  resolution_note TEXT," +
                    "  resolved_at DATETIME," +
                    "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB"
                );
            } catch (Exception e) {
                log.debug("Table {} already exists or creation skipped: {}", tbl, e.getMessage());
            }
        }
    }
}
