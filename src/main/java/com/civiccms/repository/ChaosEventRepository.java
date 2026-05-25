package com.civiccms.repository;

import com.civiccms.entity.ChaosEvent;
import com.civiccms.entity.ChaosEvent.ChaosLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChaosEventRepository extends JpaRepository<ChaosEvent, Long> {

    List<ChaosEvent> findByDetectedAtAfterOrderByDetectedAtDesc(LocalDateTime since);

    List<ChaosEvent> findByLevelOrderByDetectedAtDesc(ChaosLevel level);

    List<ChaosEvent> findTop20ByOrderByDetectedAtDesc();
}
