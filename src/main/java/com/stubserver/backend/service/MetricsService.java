package com.stubserver.backend.service;

import com.stubserver.backend.exception.BadRequestException;
import com.stubserver.backend.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final NamedParameterJdbcTemplate jdbc;

    public List<Map<String, Object>> getLifetimeHits() {
        String sql = """
            SELECT vsname, SUM(total_count) AS TOTAL_COUNT,
              SUM(total_qacount) AS TOTAL_QA_COUNT, SUM(total_uatcount) AS TOTAL_UAT_COUNT
            FROM (
              SELECT vsname, SUM(count) AS total_count,
                SUM(qacount) AS total_qacount, SUM(perfcount) AS total_uatcount
              FROM READYAPI_MONTHLY_METRICS GROUP BY vsname
              UNION ALL
              SELECT vsname, SUM(count) AS total_count,
                SUM(CASE WHEN envtype='QA' THEN count ELSE 0 END) AS total_qacount,
                SUM(CASE WHEN envtype='PERF' THEN count ELSE 0 END) AS total_uatcount
              FROM READYAPI_DAILY_METRICS
              WHERE TO_CHAR(transdate,'MON-YYYY')=TO_CHAR(SYSDATE,'MON-YYYY')
              GROUP BY vsname
            ) combined GROUP BY vsname ORDER BY vsname
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of());
        return rows.stream().map(r -> Map.<String, Object>of(
                "serviceName", r.get("VSNAME"),
                "counts", Map.of(
                        "total", r.get("TOTAL_COUNT"),
                        "qa", r.get("TOTAL_QA_COUNT"),
                        "uat", r.get("TOTAL_UAT_COUNT")
                )
        )).toList();
    }

    public Map<String, Object> getMonthlyHits(String fromMonth, String toMonth) {
        if (fromMonth == null || toMonth == null) {
            throw new BadRequestException("fromMonth and toMonth are required in MON-YYYY format");
        }
        String sql = """
            SELECT vsname AS "serviceName", LOWER(month) AS "month", LOWER(year) AS "year",
              SUM(count) AS "totalCount", SUM(qacount) AS "totalQACount", SUM(perfcount) AS "totalUATCount"
            FROM READYAPI_MONTHLY_METRICS
            WHERE TO_DATE(month||'-'||year,'MON-YYYY')
              BETWEEN TO_DATE(:fromMonth,'MON-YYYY') AND TO_DATE(:toMonth,'MON-YYYY')
            GROUP BY vsname, month, year
            ORDER BY vsname, TO_DATE(month||'-'||year,'MON-YYYY')
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromMonth", fromMonth).addValue("toMonth", toMonth);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        List<Map<String, Object>> data = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serviceName", r.get("serviceName"));
            m.put("month", r.get("month"));
            m.put("year", r.get("year"));
            m.put("totalCount", r.get("totalCount"));
            m.put("totalQACount", r.get("totalQACount"));
            m.put("totalUATCount", r.get("totalUATCount"));
            return m;
        }).toList();
        return Map.of("data", data);
    }

    public List<Map<String, Object>> getCustomReport(String fromDate, String toDate) {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }
        String sql = """
            SELECT VSNAME AS SERVICENAME, TO_CHAR(TRANSDATE,'DD-MON-YY') AS TRANSDATE,
              SUM(CASE WHEN ENVTYPE='QA' THEN COUNT ELSE 0 END) AS TOTALQACOUNT,
              SUM(CASE WHEN ENVTYPE='PERF' THEN COUNT ELSE 0 END) AS TOTALUATCOUNT,
              SUM(COUNT) AS TOTALCOUNT
            FROM READYAPI_DAILY_METRICS
            WHERE TRANSDATE BETWEEN TO_DATE(:fromDate,'DD/MM/YYYY') AND TO_DATE(:toDate,'DD/MM/YYYY')
            GROUP BY VSNAME, TRANSDATE ORDER BY TRANSDATE
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("fromDate", fromDate).addValue("toDate", toDate);
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return rows.stream().map(r -> Map.<String, Object>of(
                "serviceName", r.get("SERVICENAME"),
                "transDate", r.get("TRANSDATE"),
                "counts", Map.of(
                        "total", r.get("TOTALCOUNT"),
                        "qa", r.get("TOTALQACOUNT"),
                        "uat", r.get("TOTALUATCOUNT")
                )
        )).toList();
    }

    public Map<String, Object> getDormantServiceLists(String serverIP) {
        if (serverIP == null || serverIP.isEmpty()) {
            throw new BadRequestException("Missing serverIP in request body");
        }
        String sql = """
            WITH active_vs AS (
              SELECT VSNAME FROM READYAPI_VS_CATALOG
              WHERE STATUS='Active' AND INSTR(VIRTSERVER,:serverIP)>0
            ), daily_data AS (
              SELECT VSNAME, SUM(COUNT) AS total_hits FROM READYAPI_DAILY_METRICS
              WHERE VIRTSERVERNAME=:serverIP AND TRANSDATE>=TRUNC(SYSDATE,'MM')
              AND VSNAME IN (SELECT VSNAME FROM active_vs) GROUP BY VSNAME
            ), monthly_data AS (
              SELECT VSNAME, MONTH, YEAR, SUM(COUNT) AS total_hits
              FROM READYAPI_MONTHLY_METRICS WHERE VIRTSERVER=:serverIP
              AND VSNAME IN (SELECT VSNAME FROM active_vs)
              AND ADD_MONTHS(TRUNC(SYSDATE,'MM'),-6)<=TO_DATE(YEAR||'-'||MONTH||'-01','YYYY-MM-DD')
              GROUP BY VSNAME, MONTH, YEAR
            ), last_3_months AS (
              SELECT VSNAME, SUM(NVL(total_hits,0)) AS hits_3m FROM (
                SELECT * FROM daily_data UNION ALL
                SELECT VSNAME, total_hits FROM monthly_data
                WHERE TO_DATE(YEAR||'-'||MONTH||'-01','YYYY-MM-DD')>=ADD_MONTHS(TRUNC(SYSDATE,'MM'),-3)
              ) GROUP BY VSNAME
            ), last_6_months AS (
              SELECT VSNAME, SUM(NVL(total_hits,0)) AS hits_6m FROM (
                SELECT * FROM daily_data UNION ALL SELECT VSNAME, total_hits FROM monthly_data
              ) GROUP BY VSNAME
            )
            SELECT a.VSNAME, NVL(l3.hits_3m,0) AS HITS_3M, NVL(l6.hits_6m,0) AS HITS_6M,
              CASE WHEN NVL(l3.hits_3m,0)=0 THEN 'count_0'
                   WHEN NVL(l3.hits_3m,0) BETWEEN 1 AND 50 THEN 'count_1_50'
                   WHEN NVL(l3.hits_3m,0) BETWEEN 51 AND 100 THEN 'count_51_100'
                   ELSE NULL END AS COUNT_CATEGORY_3M,
              CASE WHEN NVL(l6.hits_6m,0)=0 THEN 'count_0'
                   WHEN NVL(l6.hits_6m,0) BETWEEN 1 AND 50 THEN 'count_1_50'
                   WHEN NVL(l6.hits_6m,0) BETWEEN 51 AND 100 THEN 'count_51_100'
                   ELSE NULL END AS COUNT_CATEGORY_6M
            FROM active_vs a
            LEFT JOIN last_3_months l3 ON a.VSNAME=l3.VSNAME
            LEFT JOIN last_6_months l6 ON a.VSNAME=l6.VSNAME
            WHERE NVL(l3.hits_3m,0)<=100
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("serverIP", serverIP));

        Map<String, Object> resp = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> last3 = new LinkedHashMap<>();
        last3.put("count_0", new ArrayList<>());
        last3.put("count_1_50", new ArrayList<>());
        last3.put("count_51_100", new ArrayList<>());
        Map<String, List<Map<String, Object>>> last6 = new LinkedHashMap<>();
        last6.put("count_0", new ArrayList<>());
        last6.put("count_1_50", new ArrayList<>());
        last6.put("count_51_100", new ArrayList<>());

        for (Map<String, Object> row : rows) {
            String vsname = String.valueOf(row.get("VSNAME"));
            Object hits3 = row.get("HITS_3M");
            Object hits6 = row.get("HITS_6M");
            String cat3 = (String) row.get("COUNT_CATEGORY_3M");
            String cat6 = (String) row.get("COUNT_CATEGORY_6M");
            if (cat3 != null && last3.containsKey(cat3)) {
                last3.get(cat3).add(Map.of("VSNAME", vsname, "COUNT", hits3));
            }
            if (cat6 != null && last6.containsKey(cat6)) {
                last6.get(cat6).add(Map.of("VSNAME", vsname, "COUNT", hits6));
            }
        }
        resp.put("last_3_months", last3);
        resp.put("last_6_months", last6);
        return resp;
    }

    public Map<String, Object> getResponseTime(String serviceName, String serverIP,
                                                String fromDateTime, String toDateTime) {
        if (serviceName == null || serverIP == null) {
            throw new BadRequestException("serviceName and serverIP are required.");
        }
        if (fromDateTime == null || toDateTime == null) {
            throw new BadRequestException("fromDateTime and toDateTime are required (ISO: YYYY-MM-DDTHH:mm).");
        }
        if (!fromDateTime.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}") ||
                !toDateTime.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
            throw new BadRequestException("Invalid datetime format. Use ISO like 2025-12-01T10:25.");
        }

        String fromUtc = DateTimeUtil.istToUtcMinuteString(fromDateTime);
        String toUtc = DateTimeUtil.istToUtcMinuteString(toDateTime);

        String sql = """
            SELECT dm.VSNAME, dm.VIRTSERVERNAME, rt.RESPID, rt.METRICSID,
              rt.STARTTIME, rt.ENDTIME, rt.AVGRESPTIME, rt.MAXRESPTIME, rt.AVGTPS
            FROM READYAPI_RESPONSE_TIME rt
            JOIN READYAPI_DAILY_METRICS dm ON rt.METRICSID=dm.METRICSID
            WHERE dm.VSNAME=:serviceName AND dm.VIRTSERVERNAME=:serverIP
            AND rt.STARTTIME<=TO_TIMESTAMP(:toDateTimeUtc,'YYYY-MM-DD"T"HH24:MI')
            AND rt.ENDTIME>=TO_TIMESTAMP(:fromDateTimeUtc,'YYYY-MM-DD"T"HH24:MI')
            ORDER BY rt.STARTTIME ASC
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("serviceName", serviceName)
                .addValue("serverIP", serverIP)
                .addValue("fromDateTimeUtc", fromUtc)
                .addValue("toDateTimeUtc", toUtc);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, params);
        return Map.of("total", rows.size(), "data", rows);
    }
}
