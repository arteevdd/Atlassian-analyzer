package ru.spbstu.atlassiananalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class JiraConfig {

    @Value("${jira.base-url:https://issues.apache.org/jira}")
    private String jiraBaseUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public String jiraBaseUrl() {
        return jiraBaseUrl;
    }
}