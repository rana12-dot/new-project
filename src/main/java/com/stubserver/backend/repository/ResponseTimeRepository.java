package com.stubserver.backend.repository;

import com.stubserver.backend.entity.ResponseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseTimeRepository extends JpaRepository<ResponseTime, Long> {
}
