package com.stubserver.backend.repository;

import com.stubserver.backend.entity.ExecutionMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionModeRepository extends JpaRepository<ExecutionMode, Long> {
    @Query("SELECT e FROM ExecutionMode e WHERE e.masterId = :masterId AND TRIM(e.virtServer) = :virtServer")
    Optional<ExecutionMode> findByMasterIdAndVirtServer(@Param("masterId") Long masterId,
                                                         @Param("virtServer") String virtServer);
}
