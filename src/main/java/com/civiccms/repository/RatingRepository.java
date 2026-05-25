package com.civiccms.repository;

import com.civiccms.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByComplaintId(Long complaintId);
    boolean existsByComplaintId(Long complaintId);
}
