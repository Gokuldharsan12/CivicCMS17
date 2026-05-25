package com.civiccms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    // Location
    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    private String address;

    // Media
    @Column(name = "photo_url")
    private String photoUrl;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id")
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_dept_id")
    private Department assignedDept;

    // SLA
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    // AI fields
    @Column(name = "is_duplicate")
    private Boolean isDuplicate = false;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "duplicate_count")
    private Integer duplicateCount = 0;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "extracted_keywords", columnDefinition = "TEXT")
    private String extractedKeywords;

    // AI processing stage (1-6)
    @Column(name = "ai_stage")
    private Integer aiStage = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ComplaintStatus {
        SUBMITTED, ASSIGNED, IN_PROGRESS, RESOLVED, ESCALATED, DUPLICATE
    }

    public enum ComplaintPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
