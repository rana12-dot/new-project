package com.stubserver.backend.service;

import com.stubserver.backend.entity.ExecutionMode;
import com.stubserver.backend.entity.LiveUrl;
import com.stubserver.backend.entity.VsCatalog;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.exception.ConflictException;
import com.stubserver.backend.exception.NotFoundException;
import com.stubserver.backend.repository.ExecutionModeRepository;
import com.stubserver.backend.repository.LiveUrlRepository;
import com.stubserver.backend.repository.VsCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExecutionModeService {

    private static final Set<String> VALID_MODES = Set.of("Failover", "Stand In", "Live Invocation", "Recording");

    private final VsCatalogRepository vsCatalogRepo;
    private final ExecutionModeRepository execModeRepo;
    private final LiveUrlRepository liveUrlRepo;

    public Map<String, Object> getExecutionMode(String serviceName, String serverIP) {
        VsCatalog catalog = vsCatalogRepo.findByVsname(serviceName)
                .orElse(null);
        if (catalog == null) {
            throw new NotFoundException("Service name not found in catalog.");
        }

        ExecutionMode exec = execModeRepo.findByMasterIdAndVirtServer(catalog.getMasterId(), serverIP.trim())
                .orElse(null);
        if (exec == null) {
            // Node.js returns 200 with {error:...} — match exactly
            return Map.of("error", "Execution mode not found.");
        }

        List<LiveUrl> urls = liveUrlRepo.findByVsid(exec.getVsid());
        List<Map<String, Object>> liveUrls = urls.stream().map(u -> Map.<String, Object>of(
                "vsurlId", u.getVsurlId(),
                "host", u.getHost(),
                "isActive", u.getIsActive()
        )).toList();

        return Map.of("executionMode", exec.getExecutionMode(), "vsid", exec.getVsid(), "liveUrls", liveUrls);
    }

    @Transactional
    public Map<String, String> updateExecutionMode(String serviceName, String serverIP, String executionMode) {
        if (!VALID_MODES.contains(executionMode)) {
            throw new BadRequestException("Invalid execution mode");
        }

        VsCatalog catalog = vsCatalogRepo.findByVsname(serviceName)
                .orElseThrow(() -> new NotFoundException("Service name not found in catalog."));

        ExecutionMode exec = execModeRepo.findByMasterIdAndVirtServer(catalog.getMasterId(), serverIP.trim())
                .orElseThrow(() -> new NotFoundException("No matching record found to update."));

        exec.setExecutionMode(executionMode);
        execModeRepo.save(exec);
        return Map.of("message", "Execution mode updated successfully");
    }

    @Transactional
    public Map<String, String> addLiveUrl(String serviceName, String serverIP, String liveurl, String updatedBy) {
        VsCatalog catalog = vsCatalogRepo.findByVsname(serviceName)
                .orElseThrow(() -> new NotFoundException("Service name not found in catalog"));

        ExecutionMode exec = execModeRepo.findByMasterIdAndVirtServer(catalog.getMasterId(), serverIP.trim())
                .orElseThrow(() -> new NotFoundException("VSID not found for the given service and server"));

        if (liveUrlRepo.existsByVsidAndHost(exec.getVsid(), liveurl.trim())) {
            throw new ConflictException("Live URL already exists");
        }

        Long maxId = liveUrlRepo.findMaxVsurlId();
        LiveUrl url = new LiveUrl();
        url.setVsurlId(maxId + 1);
        url.setVsid(exec.getVsid());
        url.setHost(liveurl.trim());
        url.setIsActive("N");
        url.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        url.setUpdatedBy(updatedBy);
        liveUrlRepo.save(url);
        return Map.of("message", "Live URL inserted successfully");
    }

    @Transactional
    public Map<String, String> deleteLiveUrl(Long vsurlid) {
        LiveUrl url = liveUrlRepo.findById(vsurlid)
                .orElseThrow(() -> new NotFoundException("vsurlid not found"));

        if ("Y".equals(url.getIsActive() != null ? url.getIsActive().trim() : "")) {
            throw new ConflictException("Cannot delete an active URL");
        }

        liveUrlRepo.deleteById(vsurlid);
        return Map.of("message", "Deleted successfully");
    }

    @Transactional
    public Map<String, String> setActiveLiveUrl(Long vsurlid, Long vsid, String host, String updatedBy) {
        List<LiveUrl> allUrls = liveUrlRepo.findByVsid(vsid);

        // Deactivate all
        for (LiveUrl u : allUrls) {
            u.setIsActive("N");
            u.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            u.setUpdatedBy(updatedBy);
        }
        liveUrlRepo.saveAll(allUrls);

        // Verify and activate target
        LiveUrl target = liveUrlRepo.findById(vsurlid)
                .orElseThrow(() -> new NotFoundException("vsurlid not found"));

        if (!target.getVsid().equals(vsid)) {
            throw new ConflictException("vsurlid not under this vsid");
        }
        if (!target.getHost().trim().equals(host.trim())) {
            throw new ConflictException("Host mismatch");
        }

        target.setIsActive("Y");
        target.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        target.setUpdatedBy(updatedBy);
        liveUrlRepo.save(target);
        return Map.of("message", "Activated successfully");
    }
}
