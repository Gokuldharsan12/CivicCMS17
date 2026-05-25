package com.civiccms.repository;

import com.civiccms.entity.Complaint;
import com.civiccms.entity.Complaint.ComplaintStatus;import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByCreatedAtAfter(LocalDateTime dateTime);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findBySubmittedById(Long userId);

    List<Complaint> findByAssignedDeptId(Long deptId);

    List<Complaint> findByAssignedDeptCode(String code);

    @Query("SELECT c FROM Complaint c WHERE c.assignedDept.id = :deptId AND c.status = :status")
    List<Complaint> findByDeptAndStatus(@Param("deptId") Long deptId,
                                        @Param("status") ComplaintStatus status);

    @Query("SELECT c FROM Complaint c WHERE (:deptId IS NULL OR c.assignedDept.id = :deptId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:priority IS NULL OR c.priority = :priority)")
    List<Complaint> filterComplaints(@Param("deptId")   Long deptId,
                                     @Param("status")   ComplaintStatus status,
                                     @Param("priority") Complaint.ComplaintPriority priority);

    @Query("SELECT c FROM Complaint c WHERE c.dueDate IS NOT NULL " +
           "AND c.dueDate < :now " +
           "AND c.status NOT IN ('RESOLVED', 'DUPLICATE')")
    List<Complaint> findOverdueComplaints(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Complaint c WHERE c.lat BETWEEN :minLat AND :maxLat " +
           "AND c.lng BETWEEN :minLng AND :maxLng " +
           "AND c.status != 'RESOLVED' " +
           "AND c.id != :excludeId")
    List<Complaint> findNearbyOpenComplaints(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng,
            @Param("excludeId") Long excludeId);
}
