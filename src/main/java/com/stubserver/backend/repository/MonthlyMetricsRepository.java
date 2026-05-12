package com.stubserver.backend.repository;

import com.stubserver.backend.entity.MonthlyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyMetricsRepository extends JpaRepository<MonthlyMetrics, Long> {
}
