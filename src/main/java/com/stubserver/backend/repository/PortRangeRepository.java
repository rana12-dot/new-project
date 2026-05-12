package com.stubserver.backend.repository;

import com.stubserver.backend.entity.PortRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PortRangeRepository extends JpaRepository<PortRange, Long> {
    boolean existsByAppName(String appName);

    @Query("SELECT COALESCE(MAX(p.portId), 0) FROM PortRange p")
    Long findMaxPortId();
}
