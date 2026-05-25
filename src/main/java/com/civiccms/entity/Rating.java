package com.civiccms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@Data
@NoArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", unique = true, nullable = true)
    private Complaint complaint;

    @Column(name = "submitter_name")
    private String submitterName;

    @Column(name = "speed_score", nullable = false)
    private Integer speedScore;

    @Column(name = "quality_score", nullable = false)
    private Integer qualityScore;

    @Column(name = "communication_score", nullable = false)
    private Integer communicationScore;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
