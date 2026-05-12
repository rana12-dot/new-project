package com.stubserver.backend.repository;

import com.stubserver.backend.entity.VsCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VsCatalogRepository extends JpaRepository<VsCatalog, Long> {
    Optional<VsCatalog> findByVsname(String vsname);
}
