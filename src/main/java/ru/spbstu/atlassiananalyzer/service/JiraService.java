package ru.spbstu.atlassiananalyzer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;

import java.util.Arrays;
import java.util.List;

@Service
public class JiraService {

    private final RestTemplate restTemplate;
    private final String jiraBaseUrl;

    @Autowired
    public JiraService(RestTemplate restTemplate, @Value("${jira.base-url}") String jiraBaseUrl) {
        this.restTemplate = restTemplate;
        this.jiraBaseUrl = jiraBaseUrl;
    }

    public List<ProjectDto> getAllProjects() {
        String url = jiraBaseUrl + "/rest/api/2/project";
        ProjectDto[] projects = restTemplate.getForObject(url, ProjectDto[].class);
        return Arrays.asList(projects);
    }

    public ProjectDto getProjectByKey(String projectKey) {
        String url = jiraBaseUrl + "/rest/api/2/project/" + projectKey;
        return restTemplate.getForObject(url, ProjectDto.class);
    }
}