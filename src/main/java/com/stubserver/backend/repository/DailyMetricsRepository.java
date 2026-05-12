package com.stubserver.backend.repository;

import com.stubserver.backend.entity.DailyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyMetricsRepository extends JpaRepository<DailyMetrics, Long> {
}
