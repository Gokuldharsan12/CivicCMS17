package com.civiccms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chaos_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChaosEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_key", nullable = false)
    private String zoneKey;

    @Column(name = "complaint_count", nullable = false)
    private Integer complaintCount;

    @Column(name = "top_category")
    private String topCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChaosLevel level;

    @CreationTimestamp
    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt;

    public enum ChaosLevel {
        WARNING, CRITICAL
    }

    public String toJson() {
        return String.format(
            "{\"zone\":\"%s\",\"count\":%d,\"category\":\"%s\",\"level\":\"%s\"}",
            zoneKey, complaintCount, topCategory, level.name()
        );
    }
}
