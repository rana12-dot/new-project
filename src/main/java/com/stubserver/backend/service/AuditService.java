package com.stubserver.backend.service;

import com.stubserver.backend.config.TableNames;
import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.repository.EnvTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Set<String> REMARK_ACTIONS = Set.of(
            "deploy", "re-deploy", "response delay", "dataset upload", "dataset delete"
    );

    private final EnvTableRepository envRepo;
    private final TableNames tableNames;

    public Map<String, Object> getAuditLogs(String environment) {
        validateEnv(environment);
        String table = tableNames.auditTable(environment);
        List<Map<String, Object>> rows = envRepo.getAuditLogs(table);
        List<Map<String, Object>> logs = rows.stream().map(row -> Map.<String, Object>of(
                "id", row.get("ID"),
                "user", row.get("USERNAME"),
                "serviceName", row.get("SERVICE_NAME"),
                "action", row.get("ACTION_TYPE"),
                "remark", row.get("REMARK"),
                "timestamp", row.get("TIMESTAMP")
        )).toList();
        return Map.of("logs", logs);
    }

    public Map<String, String> logAudit(String username, String serviceName, String action,
                                         String environment, String remark) {
        if (username == null || serviceName == null || action == null || environment == null) {
            throw new BadRequestException("Missing required fields");
        }
        String table = tableNames.auditTable(environment);
        String finalRemark = REMARK_ACTIONS.contains(action.toLowerCase()) ? (remark != null ? remark : "-") : "-";
        envRepo.insertAuditLog(table, username, serviceName, action, finalRemark);
        return Map.of("message", "Audit log inserted");
    }

    private void validateEnv(String env) {
        if (env == null || (!env.equalsIgnoreCase("QA") && !env.equalsIgnoreCase("UAT"))) {
            throw new BadRequestException("Invalid or missing environment parameter");
        }
    }
}
