package com.stubserver.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "table")
@PropertySource(value = "classpath:db-tables.properties", ignoreResourceNotFound = true)
public class TableNames {

    private String users = "STUBSERVERUSERS";
    private String refreshTokens = "AUTH_REFRESH_TOKENS";
    private String vsCatalog = "READYAPI_VS_CATALOG";
    private String executionMode = "VS_EXECUTIONMODE";
    private String liveUrls = "VS_LIVEURLS";
    private String portRange = "READYAPI_PORT_RANGE";
    private String dailyMetrics = "READYAPI_DAILY_METRICS";
    private String monthlyMetrics = "READYAPI_MONTHLY_METRICS";
    private String responseTime = "READYAPI_RESPONSE_TIME";

    private AuditLogs auditLogs = new AuditLogs();
    private AssignedServices assignedServices = new AssignedServices();
    private VsDetails vsDetails = new VsDetails();
    private MasterCatalog masterCatalog = new MasterCatalog();

    @Getter @Setter
    public static class AuditLogs {
        private String qa = "STUBSERVERAUDITLOGS_QA";
        private String uat = "STUBSERVERAUDITLOGS_UAT";
    }

    @Getter @Setter
    public static class AssignedServices {
        private String qa = "STUBASSIGNEDSERVICES_QA";
        private String uat = "STUBASSIGNEDSERVICES_UAT";
    }

    @Getter @Setter
    public static class VsDetails {
        private String qa = "STUBSERVERQA_VSDETAILS";
        private String uat = "STUBSERVERUAT_VSDETAILS";
    }

    @Getter @Setter
    public static class MasterCatalog {
        private String qa = "STUBSERVER_MASTER_CATALOG_QA";
        private String uat = "STUBSERVERUAT_VSDETAILS";
    }

    public String auditTable(String env) {
        return "UAT".equalsIgnoreCase(env) ? auditLogs.getUat() : auditLogs.getQa();
    }

    public String assignedServicesTable(String env) {
        return "UAT".equalsIgnoreCase(env) ? assignedServices.getUat() : assignedServices.getQa();
    }

    public String vsDetailsTable(String env) {
        return "UAT".equalsIgnoreCase(env) ? vsDetails.getUat() : vsDetails.getQa();
    }

    public String masterCatalogTable(String env) {
        return "UAT".equalsIgnoreCase(env) ? masterCatalog.getUat() : masterCatalog.getQa();
    }
}
