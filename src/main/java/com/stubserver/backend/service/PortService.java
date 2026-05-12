package com.stubserver.backend.service;

import com.stubserver.backend.entity.PortRange;
import com.stubserver.backend.entity.VsCatalog;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.exception.ConflictException;
import com.stubserver.backend.exception.NotFoundException;
import com.stubserver.backend.repository.PortRangeRepository;
import com.stubserver.backend.repository.VsCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PortService {

    private final PortRangeRepository portRangeRepo;
    private final VsCatalogRepository vsCatalogRepo;

    public Map<String, Object> getAppPortLists() {
        List<PortRange> all = portRangeRepo.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getAppName() != null ? p.getAppName() : ""))
                .toList();
        List<Map<String, Object>> data = all.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("PORTID", p.getPortId());
            m.put("APPNAME", p.getAppName());
            m.put("PORTS", p.getPorts());
            return m;
        }).toList();
        return Map.of("totalApp", data.size(), "data", data);
    }

    @Transactional
    public Map<String, String> addAppPortDetails(String appname, String port, String updatedby) {
        if (appname == null || port == null || updatedby == null) {
            throw new BadRequestException("Missing required fields: appname, port, updatedby");
        }
        if (portRangeRepo.existsByAppName(appname)) {
            throw new ConflictException("Requested Application name already exists");
        }
        Long lastId = portRangeRepo.findMaxPortId();
        PortRange pr = new PortRange();
        pr.setPortId(lastId + 1);
        pr.setAppName(appname);
        pr.setPorts(port);
        pr.setUpdatedBy(updatedby);
        pr.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        portRangeRepo.save(pr);
        return Map.of("message", "Application & port added successfully");
    }

    @Transactional
    public Map<String, String> modifyAppPortDetails(Long portid, String appname, String port, String updatedby) {
        PortRange existing = portRangeRepo.findById(portid)
                .orElseThrow(() -> new NotFoundException("No record found with the given portid"));

        if (Objects.equals(existing.getAppName(), appname) && Objects.equals(existing.getPorts(), port)) {
            return Map.of("message", "No changes made. Data is identical.");
        }

        existing.setAppName(appname);
        existing.setPorts(port);
        existing.setUpdatedBy(updatedby);
        existing.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        portRangeRepo.save(existing);
        return Map.of("message", "Application port details updated successfully");
    }

    @Transactional
    public Map<String, String> deleteAppPort(Long portid) {
        if (!portRangeRepo.existsById(portid)) {
            throw new NotFoundException("No record found to delete with the given portid");
        }
        portRangeRepo.deleteById(portid);
        return Map.of("message", "Application port deleted successfully");
    }

    public Map<String, Object> getAvailablePorts(String appName, String portRange) {
        // Build port→statuses map from catalog
        List<VsCatalog> catalog = vsCatalogRepo.findAll().stream()
                .filter(v -> "Active".equals(v.getStatus()) || "Inactive".equals(v.getStatus()))
                .toList();

        Map<Integer, List<String>> masterMap = new HashMap<>();
        for (var v : catalog) {
            if (v.getPort() == null) continue;
            try {
                int p = Integer.parseInt(v.getPort().trim());
                masterMap.computeIfAbsent(p, k -> new ArrayList<>()).add(v.getStatus());
            } catch (NumberFormatException ignored) {}
        }

        List<Integer> requestedPorts = parsePorts(portRange);
        List<Map<String, Object>> availablePorts = new ArrayList<>();

        for (int p : requestedPorts) {
            if (masterMap.containsKey(p)) {
                List<String> statuses = masterMap.get(p);
                if (statuses.contains("Active")) {
                    availablePorts.add(Map.of("port", p, "status", "USED(ACTIVE)"));
                } else if (statuses.stream().allMatch("Inactive"::equals)) {
                    availablePorts.add(Map.of("port", p, "status", "ASSIGNED(INACTIVE)"));
                }
            } else {
                availablePorts.add(Map.of("port", p, "status", "NOT_ASSIGNED"));
            }
        }

        return Map.of("appName", appName != null ? appName : "Application",
                "availablePorts", availablePorts, "totalAvailable", availablePorts.size());
    }

    private List<Integer> parsePorts(String portRange) {
        List<Integer> ports = new ArrayList<>();
        if (portRange == null || portRange.isEmpty()) return ports;
        for (String segment : portRange.split(",")) {
            segment = segment.trim();
            if (segment.contains("-")) {
                String[] parts = segment.split("-");
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int i = start; i <= end; i++) ports.add(i);
            } else {
                try { ports.add(Integer.parseInt(segment)); } catch (NumberFormatException ignored) {}
            }
        }
        return ports;
    }
}
