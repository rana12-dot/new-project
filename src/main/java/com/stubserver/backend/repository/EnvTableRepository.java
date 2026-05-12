package com.stubserver.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EnvTableRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<Map<String, Object>> getAuditLogs(String tableName) {
        String sql = "SELECT * FROM " + tableName + " ORDER BY TIMESTAMP DESC";
        return jdbc.queryForList(sql, Map.of());
    }

    public void insertAuditLog(String tableName, String username, String serviceName,
                               String action, String remark) {
        String sql = "INSERT INTO " + tableName +
                " (USERNAME, SERVICE_NAME, ACTION_TYPE, REMARK) " +
                "VALUES (:username, :serviceName, :action, :remark)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("serviceName", serviceName)
                .addValue("action", action)
                .addValue("remark", remark);
        jdbc.update(sql, params);
    }

    public List<Map<String, Object>> getAuditLogsByEnvForServerList(String tableName) {
        String sql = "SELECT ACTION_TYPE, TIMESTAMP FROM " + tableName +
                " WHERE ACTION_TYPE IN ('SERVER START', 'SERVER STOP')" +
                " ORDER BY TIMESTAMP DESC FETCH FIRST 1 ROWS ONLY";
        return jdbc.queryForList(sql, Map.of());
    }

    public void deleteAssignedServices(String tableName, String username) {
        String sql = "DELETE FROM " + tableName + " WHERE USERNAME = :username";
        jdbc.update(sql, Map.of("username", username));
    }

    public void insertAssignedServices(String tableName, String username, List<String> services) {
        String sql = "INSERT INTO " + tableName + " (USERNAME, SERVICENAME) VALUES (:username, :serviceName)";
        List<MapSqlParameterSource> batchParams = services.stream()
                .map(s -> new MapSqlParameterSource()
                        .addValue("username", username)
                        .addValue("serviceName", s))
                .toList();
        jdbc.batchUpdate(sql, batchParams.toArray(new MapSqlParameterSource[0]));
    }

    public List<String> getAssignedServices(String tableName, String username) {
        String sql = "SELECT SERVICENAME FROM " + tableName + " WHERE USERNAME = :username";
        return jdbc.queryForList(sql, Map.of("username", username), String.class);
    }

    public Map<String, Object> getGroupTagsConfig(String tableName, String serviceName) {
        String sql = "SELECT \"GROUP\", TAGS FROM " + tableName + " WHERE VSNAME = :serviceName";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("serviceName", serviceName));
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    public void updateGroup(String tableName, String serviceName, String group) {
        String sql = "UPDATE " + tableName + " SET \"GROUP\" = :groupVal WHERE VSNAME = :serviceName";
        jdbc.update(sql, Map.of("groupVal", group, "serviceName", serviceName));
    }

    public void updateTags(String tableName, String serviceName, String tags) {
        String sql = "UPDATE " + tableName + " SET TAGS = :tags WHERE VSNAME = :serviceName";
        jdbc.update(sql, Map.of("tags", tags, "serviceName", serviceName));
    }

    public List<Map<String, Object>> getDatasourceEnabled(String tableName) {
        String sql = "SELECT VSNAME, DATASOURCEENABLED FROM " + tableName +
                " WHERE UPPER(TRIM(TO_CHAR(DATASOURCEENABLED))) IN ('1', 'Y', 'TRUE')" +
                " GROUP BY VSNAME, DATASOURCEENABLED ORDER BY VSNAME ASC";
        return jdbc.queryForList(sql, Map.of());
    }

    public Map<String, Object> checkMasterCatalog(String tableName, String serviceName, String port) {
        List<String> whereClauses = new java.util.ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (serviceName != null && !serviceName.isEmpty()) {
            whereClauses.add("VSNAME = :serviceName");
            params.addValue("serviceName", serviceName);
        }
        if (port != null && !port.isEmpty()) {
            whereClauses.add("PORT = :port");
            params.addValue("port", port);
        }

        if (whereClauses.isEmpty()) {
            return Map.of("NAME_MATCH", 0, "PORT_MATCH", 0);
        }

        String where = "WHERE " + String.join(" OR ", whereClauses);
        String nameExpr = (serviceName != null && !serviceName.isEmpty())
                ? "MAX(CASE WHEN VSNAME = :serviceName THEN 1 ELSE 0 END)"
                : "0";
        String portExpr = (port != null && !port.isEmpty())
                ? "MAX(CASE WHEN PORT = :port THEN 1 ELSE 0 END)"
                : "0";

        String sql = "SELECT " + nameExpr + " AS NAME_MATCH, " + portExpr + " AS PORT_MATCH FROM " + tableName + " " + where;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.isEmpty() ? Map.of("NAME_MATCH", 0, "PORT_MATCH", 0) : rows.get(0);
    }
}
