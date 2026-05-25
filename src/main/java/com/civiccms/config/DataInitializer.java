package com.civiccms.config;

import com.civiccms.entity.User;
import com.civiccms.repository.UserRepository;
import com.civiccms.service.DeptComplaintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Runs once on startup:
 *  1. Seed default admin user
 *  2. Seed demo citizen user
 *  3. Make users.password_hash nullable
 *  4. Make ratings.complaint_id nullable + add submitter_name column
 *  5. Add users.department_id + expand role ENUM with DEPT_HEAD
 *  6. Create dept_complaints_* tables and back-fill existing data
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_EMAIL    = "sgokuldharsan@gmail.com";
    private static final String ADMIN_PASSWORD = "Gokul@123";
    private static final String ADMIN_NAME     = "Admin";

    private static final String DEMO_EMAIL    = "citizen@civiccms.in";
    private static final String DEMO_PASSWORD = "Test@123";
    private static final String DEMO_NAME     = "Demo Citizen";

    private final UserRepository       userRepo;
    private final PasswordEncoder      passwordEncoder;
    private final DataSource           dataSource;
    private final DeptComplaintService deptComplaintService;

    public DataInitializer(UserRepository userRepo,
                           PasswordEncoder passwordEncoder,
                           DataSource dataSource,
                           DeptComplaintService deptComplaintService) {
        this.userRepo             = userRepo;
        this.passwordEncoder      = passwordEncoder;
        this.dataSource           = dataSource;
        this.deptComplaintService = deptComplaintService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        seedDemoCitizen();
        migratePasswordHashColumn();
        migrateRatingsTable();
        migrateUsersDepartmentIdColumn();
        migrateDepartments();   // rename/merge old verbose dept rows to clean 6
        // Create dept-specific tables and back-fill existing complaints
        deptComplaintService.migrateExistingComplaints();
    }

    private void seedAdmin() {
        if (!userRepo.existsByEmail(ADMIN_EMAIL)) {
            User admin = new User();
            admin.setName(ADMIN_NAME);
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setRole(User.Role.ADMIN);
            admin.setProvider("LOCAL");
            userRepo.save(admin);
            log.info("Default admin created: {}", ADMIN_EMAIL);
        }
    }

    private void seedDemoCitizen() {
        if (!userRepo.existsByEmail(DEMO_EMAIL)) {
            User demo = new User();
            demo.setName(DEMO_NAME);
            demo.setEmail(DEMO_EMAIL);
            demo.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            demo.setRole(User.Role.CITIZEN);
            demo.setProvider("LOCAL");
            userRepo.save(demo);
            log.info("Demo citizen created: {}", DEMO_EMAIL);
        }
    }

    private void migratePasswordHashColumn() {
        try (Connection conn = dataSource.getConnection()) {
            String db = conn.getCatalog();
            boolean needsNullable = false;
            try (ResultSet rs = conn.getMetaData().getColumns(db, null, "users", "password_hash")) {
                if (rs.next()) needsNullable = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
            }
            if (needsNullable) {
                conn.createStatement().executeUpdate(
                    "ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL");
                log.info("users.password_hash is now nullable.");
            }
        } catch (Exception e) {
            log.error("password_hash migration failed: {}", e.getMessage());
        }
    }

    private void migrateRatingsTable() {
        try (Connection conn = dataSource.getConnection()) {
            String db = conn.getCatalog();

            // Make complaint_id nullable
            boolean needsNullable = false;
            try (ResultSet rs = conn.getMetaData().getColumns(db, null, "ratings", "complaint_id")) {
                if (rs.next()) needsNullable = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
            }
            if (needsNullable) {
                // Drop FK first
                String fkName = null;
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA='" + db + "' AND TABLE_NAME='ratings'" +
                        " AND COLUMN_NAME='complaint_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1")) {
                    if (rs.next()) fkName = rs.getString("CONSTRAINT_NAME");
                }
                if (fkName != null) {
                    conn.createStatement().executeUpdate(
                        "ALTER TABLE ratings DROP FOREIGN KEY `" + fkName + "`");
                }
                conn.createStatement().executeUpdate(
                    "ALTER TABLE ratings MODIFY COLUMN complaint_id BIGINT NULL");
                try {
                    conn.createStatement().executeUpdate(
                        "ALTER TABLE ratings ADD CONSTRAINT fk_rating_complaint " +
                        "FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE");
                } catch (Exception ignored) {}
                log.info("ratings.complaint_id is now nullable.");
            }

            // Add submitter_name if missing
            boolean hasCol = false;
            try (ResultSet rs = conn.getMetaData().getColumns(db, null, "ratings", "submitter_name")) {
                hasCol = rs.next();
            }
            if (!hasCol) {
                conn.createStatement().executeUpdate(
                    "ALTER TABLE ratings ADD COLUMN submitter_name VARCHAR(255) DEFAULT 'Anonymous'");
                log.info("Added ratings.submitter_name column.");
            }
        } catch (Exception e) {
            log.error("Ratings table migration failed: {}", e.getMessage());
        }
    }

    private void migrateUsersDepartmentIdColumn() {
        try (Connection conn = dataSource.getConnection()) {
            String db = conn.getCatalog();

            // Add department_id if missing
            boolean hasDeptId = false;
            try (ResultSet rs = conn.getMetaData().getColumns(db, null, "users", "department_id")) {
                hasDeptId = rs.next();
            }
            if (!hasDeptId) {
                conn.createStatement().executeUpdate(
                    "ALTER TABLE users ADD COLUMN department_id BIGINT NULL");
                log.info("Added users.department_id column.");
            }

            // Expand role ENUM to include DEPT_HEAD
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA='" + db + "' AND TABLE_NAME='users' AND COLUMN_NAME='role'")) {
                if (rs.next() && !rs.getString("COLUMN_TYPE").contains("DEPT_HEAD")) {
                    conn.createStatement().executeUpdate(
                        "ALTER TABLE users MODIFY COLUMN role " +
                        "ENUM('CITIZEN','ADMIN','DEPT_HEAD') NOT NULL DEFAULT 'CITIZEN'");
                    log.info("users.role ENUM updated with DEPT_HEAD.");
                }
            }
        } catch (Exception e) {
            log.error("users.department_id migration failed: {}", e.getMessage());
        }
    }
    /**
     * Ensures the departments table contains EXACTLY the 6 project departments.
     * Runs on every startup — safe to re-run (idempotent).
     *
     * Strategy:
     *  1. Rename the 5 surviving old rows to clean names (by CODE — stable key).
     *  2. Re-assign any complaints that point to soon-to-be-deleted dept IDs to
     *     the correct surviving dept (by CODE mapping).
     *  3. Delete the 5 unwanted dept rows (DRAINAGE, STREETLIGHT, PARK, NOISE,
     *     ENCROACHMENT) after re-routing their complaints.
     *  4. Ensure the 6 wanted rows exist with clean names (upsert by CODE).
     */
    /**
     * HARD RESET — wipes all department rows and reinserts exactly 6 clean ones.
     * Runs on every startup. Safe to re-run (idempotent).
     * 1. Temporarily disable FK checks
     * 2. Re-route complaints from old unwanted depts to the 6 valid ones (by keyword)
     * 3. Delete ALL dept_head users linked to unwanted depts
     * 4. Wipe and reinsert departments table with exactly 6 rows
     */
    private void migrateDepartments() {
        try (Connection conn = dataSource.getConnection()) {

            // ── Step 1: Re-route complaints from ALL unwanted dept codes ────────
            String[][] mergeMap = {
                {"DRAINAGE",     "GARBAGE"},
                {"STREETLIGHT",  "ELECTRICITY"},
                {"PARK",         "OTHER"},
                {"NOISE",        "ANIMAL"},
                {"ENCROACHMENT", "ANIMAL"},
            };
            for (String[] m : mergeMap) {
                try {
                    conn.createStatement().executeUpdate(
                        "UPDATE complaints c " +
                        "JOIN departments src  ON src.id = c.assigned_dept_id AND src.code = '" + m[0] + "' " +
                        "JOIN departments dest ON dest.code = '" + m[1] + "' " +
                        "SET c.assigned_dept_id = dest.id");
                } catch (Exception ignored) {}
            }
            log.info("Departments: complaints re-routed.");

            // ── Step 2: Delete dept_head users tied to unwanted departments ─────
            try {
                conn.createStatement().executeUpdate(
                    "DELETE u FROM users u " +
                    "JOIN departments d ON d.id = u.department_id " +
                    "WHERE d.code NOT IN ('ROAD','GARBAGE','ELECTRICITY','WATER','ANIMAL','OTHER') " +
                    "AND u.role = 'DEPT_HEAD'");
            } catch (Exception ignored) {}

            // ── Step 3: Null out assigned_dept_id on complaints pointing to ─────
            //            departments that are about to be deleted
            try {
                conn.createStatement().executeUpdate(
                    "UPDATE complaints c " +
                    "JOIN departments d ON d.id = c.assigned_dept_id " +
                    "SET c.assigned_dept_id = NULL " +
                    "WHERE d.code NOT IN ('ROAD','GARBAGE','ELECTRICITY','WATER','ANIMAL','OTHER')");
            } catch (Exception ignored) {}

            // ── Step 4: Delete ALL rows not in the 6 wanted codes ───────────────
            conn.createStatement().executeUpdate(
                "DELETE FROM departments WHERE code NOT IN " +
                "('ROAD','GARBAGE','ELECTRICITY','WATER','ANIMAL','OTHER')");
            log.info("Departments: unwanted rows deleted.");

            // ── Step 5: Rename remaining 6 rows to clean display names ──────────
            String[][] renames = {
                {"ROAD",        "Roads"},
                {"GARBAGE",     "Sanitation"},
                {"ELECTRICITY", "Electricity"},
                {"WATER",       "Water"},
                {"ANIMAL",      "Public Safety"},
                {"OTHER",       "General"},
            };
            for (String[] r : renames) {
                conn.createStatement().executeUpdate(
                    "UPDATE departments SET name = '" + r[1] + "' WHERE code = '" + r[0] + "'");
            }

            // ── Step 6: Insert any of the 6 that are missing (fresh DB) ─────────
            String[][] wanted = {
                {"Roads",         "ROAD",        "road,pothole,street,pavement,footpath,traffic,signal,speed,bump,accident,crack,construction",                                                                        "roads@civic.gov.in"},
                {"Sanitation",    "GARBAGE",     "garbage,waste,trash,litter,dump,bin,clean,hygiene,smell,stink,overflowing,sweeping,drain,flood,waterlog,overflow,stagnant,gutter,block,clog,canal,storm",           "sanitation@civic.gov.in"},
                {"Electricity",   "ELECTRICITY", "power,electric,electricity,light,voltage,wire,transformer,outage,current,meter,shock,fault,streetlight,lamp,dark,bulb,lighting,pole,night,visibility",              "electricity@civic.gov.in"},
                {"Water",         "WATER",       "water,pipe,leak,burst,supply,tap,drinking,sewage,sewer,contamination,overflow",                                                                                     "water@civic.gov.in"},
                {"Public Safety", "ANIMAL",      "dog,stray,animal,cattle,mosquito,pest,rat,snake,bird,bite,attack,noise,sound,loud,music,horn,encroach,illegal,unauthorized,building,occupy,permit",                 "safety@civic.gov.in"},
                {"General",       "OTHER",       "general,other,misc,complaint,request,suggest,feedback,park,garden,tree,plant,bench,playground,grass",                                                               "general@civic.gov.in"},
            };
            for (String[] d : wanted) {
                conn.createStatement().executeUpdate(
                    "INSERT INTO departments (name, code, keywords_csv, head_email) " +
                    "SELECT '" + d[0] + "','" + d[1] + "','" + d[2] + "','" + d[3] + "' " +
                    "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM departments WHERE code = '" + d[1] + "')");
            }

            log.info("Departments: migration complete — exactly 6 departments active.");

        } catch (Exception e) {
            log.error("Department migration failed: {}", e.getMessage());
        }
    }

}