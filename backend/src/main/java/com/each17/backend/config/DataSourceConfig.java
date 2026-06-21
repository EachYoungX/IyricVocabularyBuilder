package com.each17.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    /**
     * 手动配置主数据源 (app_data.db)。
     * 通过创建我们自己的 @Primary DataSource Bean，我们完全接管了主数据源的控制权，
     * 绕过了所有可能出问题的 Spring Boot 自动配置。
     */
    @Bean(name = "appDataSource")
    @Primary
    public DataSource appDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            ResourceLoader resourceLoader
    ) {
        System.out.println("====================================================");
        System.out.println("      MANUALLY CONFIGURING PRIMARY DATASOURCE       ");
        System.out.println("      URL: " + url);
        System.out.println("====================================================");

        // 1. 手动创建 DataSource 实例
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driverClassName);

        // 2. 手动执行 schema.sql 初始化
        Resource schema = resourceLoader.getResource("classpath:schema.sql");
        if (schema.exists()) {
            System.out.println(">>> schema.sql found. Executing initialization script...");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
            populator.execute(dataSource);
            System.out.println(">>> schema.sql execution finished.");
        } else {
            System.err.println("!!! WARNING: schema.sql not found! Tables will not be created.");
        }

        return dataSource;
    }

    /**
     * 手动配置第二数据源 (dictionary.sqlite)。
     */
    @Bean(name = "dictionaryDataSource")
    public DataSource dictionaryDataSource(
            @Value("${app.dictionary.datasource.url}") String url,
            @Value("${app.dictionary.datasource.driver-class-name}") String driverClassName
    ) {
        System.out.println("====================================================");
        System.out.println("     MANUALLY CONFIGURING DICTIONARY DATASOURCE     ");
        System.out.println("      URL: " + url);
        System.out.println("====================================================");

        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    // 为词典数据源创建 JdbcTemplate
    @Bean(name = "dictionaryJdbcTemplate")
    public JdbcTemplate dictionaryJdbcTemplate(@Qualifier("dictionaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}