package com.stubserver.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseConfig {

    @Value("${app.db-type:oracle}")
    private String dbType;

    @Value("${ORACLE_USER:}")
    private String oracleUser;

    @Value("${ORACLE_PASSWORD:}")
    private String oraclePassword;

    @Value("${ORACLE_CONNECT_STRING:}")
    private String oracleConnectString;

    @Value("${ORACLE_POOL_MIN:2}")
    private int poolMin;

    @Value("${ORACLE_POOL_MAX:10}")
    private int poolMax;

    @Value("${MYSQL_URL:}")
    private String mysqlUrl;

    @Value("${MYSQL_USER:}")
    private String mysqlUser;

    @Value("${MYSQL_PASSWORD:}")
    private String mysqlPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig cfg = new HikariConfig();
        if ("mysql".equalsIgnoreCase(dbType)) {
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
            cfg.setJdbcUrl(mysqlUrl);
            cfg.setUsername(mysqlUser);
            cfg.setPassword(mysqlPassword);
        } else {
            cfg.setDriverClassName("oracle.jdbc.OracleDriver");
            cfg.setJdbcUrl("jdbc:oracle:thin:@//" + oracleConnectString);
            cfg.setUsername(oracleUser);
            cfg.setPassword(oraclePassword);
        }
        cfg.setMinimumIdle(poolMin);
        cfg.setMaximumPoolSize(poolMax);
        cfg.setConnectionTimeout(30_000);
        cfg.setIdleTimeout(600_000);
        cfg.setMaxLifetime(1_800_000);
        return new HikariDataSource(cfg);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.stubserver.backend.entity");
        HibernateJpaVendorAdapter vendor = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendor);
        Properties jpaProps = new Properties();
        jpaProps.setProperty("hibernate.ddl-auto", "none");
        jpaProps.setProperty("hibernate.show_sql", "false");
        if ("mysql".equalsIgnoreCase(dbType)) {
            jpaProps.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        } else {
            jpaProps.setProperty("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        }
        em.setJpaProperties(jpaProps);
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
