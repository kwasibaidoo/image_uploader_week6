package com.image_uploader_week6.image_uploader_week6.config;

import javax.sql.DataSource;

import org.json.JSONObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Configuration
public class DatabaseConfig {
    
    String secretARN = "rds!db-027f803e-1fa4-4c76-b968-17db311d0e32";
    // String secretARN = "weeksixproject/database/credentials";

    @Profile("prod")
    @Bean
    public DataSource dataSource() throws Exception {
        SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder().region(Region.of("eu-west-1")).build();
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretARN).build();
        GetSecretValueResponse getSecretValueResponse;

        try {
            getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            throw e;
        }
        
        String host = "week6lab-postgresqldatabase-fdmlfhsu9exd.cjyeckoc8ro1.eu-west-1.rds.amazonaws.com";
        String port = "5432";
        String dbname = "imagesdb";
        String secret = getSecretValueResponse.secretString();
        JSONObject secretJson = new JSONObject(secret);
        String username = secretJson.getString("username");
        String password = secretJson.getString("password");
        
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, dbname);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikariConfig.setConnectionTimeout(60000);  // 30 seconds
        hikariConfig.setValidationTimeout(60000);   // 5 seconds
        hikariConfig.setIdleTimeout(60000);        // 30 seconds
        hikariConfig.setMaxLifetime(2000000);      // About 33 minutes
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setPoolName("PostgreSQLConnectionPool");

        return new HikariDataSource(hikariConfig);

    }

    // @Bean
    // @Profile("github-actions")  // Apply this bean only if the 'github-actions' profile is active
    // public DataSource githubActionsDataSource() {
    //     // For GitHub Actions, use an in-memory database (e.g., H2 or a mock database)
    //     HikariConfig hikariConfig = new HikariConfig();
    //     hikariConfig.setJdbcUrl("jdbc:h2:mem:testdb");  // Use in-memory database during GitHub Actions
    //     hikariConfig.setUsername("sa");
    //     hikariConfig.setPassword("password");
        
    //     hikariConfig.setConnectionTimeout(60000);
    //     hikariConfig.setValidationTimeout(60000);
    //     hikariConfig.setIdleTimeout(60000);
    //     hikariConfig.setMaxLifetime(2000000);
    //     hikariConfig.setMaximumPoolSize(10);
    //     hikariConfig.setMinimumIdle(5);
    //     hikariConfig.setConnectionTestQuery("SELECT 1");
    //     hikariConfig.setPoolName("H2ConnectionPool");

    //     return new HikariDataSource(hikariConfig);
    // }
}
