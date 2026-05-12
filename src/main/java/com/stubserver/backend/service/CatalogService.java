package com.stubserver.backend.service;

import com.stubserver.backend.config.TableNames;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.repository.EnvTableRepository;
import com.stubserver.backend.repository.VsCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final VsCatalogRepository vsCatalogRepo;
    private final EnvTableRepository envRepo;
    private final TableNames tableNames;
    private final NamedParameterJdbcTemplate jdbc;

    public Map<String, Object> getServiceGroupTagsList() {
        String sql = "SELECT VSNAME, \"GROUP\", TAGS FROM READYAPI_VS_CATALOG";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of());
        List<Map<String, Object>> services = rows.stream().map(row -> {
            String name = row.get("VSNAME") != null ? row.get("VSNAME").toString().trim() : "";
            String group = row.get("GROUP") != null ? row.get("GROUP").toString().trim() : "";
            String tagsRaw = row.get("TAGS") != null ? row.get("TAGS").toString() : "";
            List<String> tags = tagsRaw.isEmpty() ? List.of() :
                    Arrays.stream(tagsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            return Map.<String, Object>of("servicename", name, "group", group, "tags", tags);
        }).toList();
        return Map.of("totalService", services.size(), "services", services);
    }

    public Map<String, Object> checkMasterCatalog(String serviceName, String port, String environment) {
        if (environment == null || (!environment.equalsIgnoreCase("QA") && !environment.equalsIgnoreCase("UAT"))) {
            throw new BadRequestException("Invalid or missing environment parameter (QA | UAT required)");
        }
        boolean nameEmpty = serviceName == null || serviceName.isEmpty();
        boolean portEmpty = port == null || port.isEmpty();
        if (nameEmpty && portEmpty) {
            throw new BadRequestException("Provide at least one of: serviceName or port");
        }

        String env = environment.toUpperCase();
        String tableName = tableNames.masterCatalogTable(env);

        Map<String, Object> result = envRepo.checkMasterCatalog(tableName, serviceName, port);
        Number nameMatchNum = (Number) result.get("NAME_MATCH");
        Number portMatchNum = (Number) result.get("PORT_MATCH");
        boolean nameMatch = nameMatchNum != null && nameMatchNum.intValue() == 1;
        boolean portMatch = portMatchNum != null && portMatchNum.intValue() == 1;
        boolean exists = nameMatch || portMatch;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("environment", env);
        resp.put("serviceName", nameEmpty ? null : serviceName);
        resp.put("port", portEmpty ? null : port);
        resp.put("exists", exists);
        resp.put("nameMatch", nameMatch);
        resp.put("portMatch", portMatch);
        return resp;
    }
}
