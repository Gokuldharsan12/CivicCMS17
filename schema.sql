-- ═══════════════════════════════════════════════════════════════
--  CivicCMS — MySQL Schema (matches JPA Entities exactly)
--  Use this for manual DB setup. With ddl-auto=update Hibernate
--  will auto-create/alter tables — this file is for reference.
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS civic_cms
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE civic_cms;

-- ── Users ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(120)  NOT NULL,
  email         VARCHAR(180)  NOT NULL UNIQUE,
  password_hash VARCHAR(255)  NULL,
  role          ENUM('CITIZEN','ADMIN','DEPT_HEAD') NOT NULL DEFAULT 'CITIZEN',
  provider      VARCHAR(20)   NULL,
  provider_id   VARCHAR(255)  NULL,
  department_id BIGINT        NULL,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ── Departments ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(120) NOT NULL UNIQUE,
  code         VARCHAR(40)  NOT NULL UNIQUE,
  keywords_csv TEXT,
  head_email   VARCHAR(180)
) ENGINE=InnoDB;

-- ── Complaints (master table - all complaints) ─────────────────
CREATE TABLE IF NOT EXISTS complaints (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  title               VARCHAR(255)  NOT NULL,
  description         TEXT          NOT NULL,
  category            VARCHAR(60)   NOT NULL,
  status              ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE')
                      NOT NULL DEFAULT 'SUBMITTED',
  priority            ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  lat                 DOUBLE,
  lng                 DOUBLE,
  address             VARCHAR(300),
  photo_url           VARCHAR(500),
  submitted_by_id     BIGINT,
  assigned_dept_id    BIGINT,
  due_date            DATETIME,
  resolved_at         DATETIME,
  resolution_note     TEXT,
  is_duplicate        TINYINT(1) NOT NULL DEFAULT 0,
  parent_id           BIGINT,
  duplicate_count     INT NOT NULL DEFAULT 0,
  sentiment_score     DOUBLE,
  extracted_keywords  TEXT,
  ai_stage            INT NOT NULL DEFAULT 1,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_complaint_user FOREIGN KEY (submitted_by_id)
    REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_complaint_dept FOREIGN KEY (assigned_dept_id)
    REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ── Department-Wise Complaint Tables ──────────────────────────
-- Each department has its own table for isolated complaint storage.
-- These are populated by a trigger/service when a complaint is assigned.

CREATE TABLE IF NOT EXISTS dept_complaints_water (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dw_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_electricity (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_de_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_road (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dr_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_garbage (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dg_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_drainage (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ddr_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_streetlight (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dsl_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_park (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dp_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_noise (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_dn_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_encroachment (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_denc_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_animal (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_da_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dept_complaints_other (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id      BIGINT NOT NULL UNIQUE,
  title             VARCHAR(255) NOT NULL,
  description       TEXT         NOT NULL,
  status            ENUM('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','ESCALATED','DUPLICATE') NOT NULL DEFAULT 'ASSIGNED',
  priority          ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'MEDIUM',
  address           VARCHAR(300),
  photo_url         VARCHAR(500),
  submitted_by_name VARCHAR(180),
  resolution_note   TEXT,
  resolved_at       DATETIME,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_do_complaint FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Ratings ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ratings (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id         BIGINT NULL UNIQUE,
  submitter_name       VARCHAR(255) DEFAULT 'Anonymous',
  speed_score          INT    NOT NULL,
  quality_score        INT    NOT NULL,
  communication_score  INT    NOT NULL,
  comments             TEXT,
  submitted_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rating_complaint FOREIGN KEY (complaint_id)
    REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Notifications ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  complaint_id    BIGINT,
  recipient_email VARCHAR(180) NOT NULL,
  type            VARCHAR(60)  NOT NULL,
  sent_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notif_complaint FOREIGN KEY (complaint_id)
    REFERENCES complaints(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Chaos Events ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS chaos_events (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  zone_key        VARCHAR(60)  NOT NULL,
  complaint_count INT          NOT NULL,
  top_category    VARCHAR(60),
  level           ENUM('WARNING','CRITICAL') NOT NULL,
  detected_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ── Site Content (CMS) ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS site_content (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  page         VARCHAR(80)  NOT NULL,
  content_key  VARCHAR(120) NOT NULL,
  content_type ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT',
  value        TEXT,
  label        VARCHAR(200),
  updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_page_key (page, content_key)
) ENGINE=InnoDB;
