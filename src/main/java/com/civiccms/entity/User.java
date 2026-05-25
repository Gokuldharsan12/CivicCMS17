package com.civiccms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Nullable — social/phone users have no password
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CITIZEN;

    // Social / Phone login fields
    @Column(name = "provider")
    private String provider;           // GOOGLE | PHONE | LOCAL

    @Column(name = "provider_id")
    private String providerId;         // Google sub / phone number

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // For DEPT_HEAD role — which department they manage
    @Column(name = "department_id")
    private Long departmentId;

    public enum Role {
        CITIZEN, ADMIN, DEPT_HEAD
    }
}
