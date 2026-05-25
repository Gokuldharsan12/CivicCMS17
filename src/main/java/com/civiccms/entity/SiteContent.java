package com.civiccms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Stores every editable piece of site content:
 *   - TEXT  → plain text / HTML snippets (hero headline, subtitles, labels …)
 *   - IMAGE → URL / path to an uploaded image (logo, hero banner, step icons …)
 *
 * Each row is identified by a unique (page, key) pair, e.g.:
 *   page="index"  key="hero.headline"   type=TEXT   value="Smart Civic Issue…"
 *   page="index"  key="hero.image"      type=IMAGE  value="/uploads/hero.jpg"
 */
@Entity
@Table(name = "site_content",
       uniqueConstraints = @UniqueConstraint(columnNames = {"page", "content_key"}))
@Data
@NoArgsConstructor
public class SiteContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The page / section this content belongs to (index, login, admin, global …) */
    @Column(nullable = false, length = 80)
    private String page;

    /** Dot-separated key, e.g. "hero.headline", "navbar.brand", "step1.icon" */
    @Column(name = "content_key", nullable = false, length = 120)
    private String contentKey;

    /** TEXT or IMAGE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContentType contentType = ContentType.TEXT;

    /** The actual content — text string or image URL */
    @Column(columnDefinition = "TEXT")
    private String value;

    /** Human-friendly label shown in the admin editor */
    @Column(length = 200)
    private String label;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ContentType {
        TEXT, IMAGE
    }
}
