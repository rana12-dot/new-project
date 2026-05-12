package com.stubserver.backend.repository;

import com.stubserver.backend.entity.LiveUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveUrlRepository extends JpaRepository<LiveUrl, Long> {
    List<LiveUrl> findByVsid(Long vsid);
    boolean existsByVsidAndHost(Long vsid, String host);

    @Query("SELECT COALESCE(MAX(l.vsurlId), 0) FROM LiveUrl l")
    Long findMaxVsurlId();
}
