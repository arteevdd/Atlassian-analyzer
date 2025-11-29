package ru.spbstu.atlassiananalyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.spbstu.atlassiananalyzer.dto.IssueDto;
import ru.spbstu.atlassiananalyzer.dto.IssueSearchResultDto;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class JiraService {

    private static final Logger logger = LoggerFactory.getLogger(JiraService.class);
    private final RestTemplate restTemplate;
    private final String jiraBaseUrl;
    private final ExecutorService executorService;

    @Autowired
    public JiraService(RestTemplate restTemplate, @Value("${jira.base-url}") String jiraBaseUrl) {
        this.restTemplate = restTemplate;
        this.jiraBaseUrl = jiraBaseUrl;
        this.executorService = Executors.newFixedThreadPool(5); // Уменьшим количество потоков
    }

    public List<ProjectDto> getAllProjects() {
        String url = jiraBaseUrl + "/rest/api/2/project";
        logger.info("Fetching projects from: {}", url);

        ProjectDto[] projects = restTemplate.getForObject(url, ProjectDto[].class);
        List<ProjectDto> projectList = Arrays.asList(projects);
        logger.info("Fetched {} projects", projectList.size());

        // Берем только первые 100 проектов для быстрой демонстрации
        List<ProjectDto> limitedProjects = projectList.stream()
                .limit(100)
                .collect(Collectors.toList());

        // Получаем полную информацию только для ограниченного количества проектов
        List<CompletableFuture<ProjectDto>> futures = limitedProjects.stream()
                .map(project -> CompletableFuture.supplyAsync(() -> getProjectByKey(project.getKey()), executorService))
                .collect(Collectors.toList());

        // Ждем завершения всех запросов
        List<ProjectDto> fullProjects = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        logger.info("Completed fetching full project info for {} projects", fullProjects.size());
        return fullProjects;
    }

    public ProjectDto getProjectByKey(String projectKey) {
        String url = jiraBaseUrl + "/rest/api/2/project/" + projectKey;

        try {
            ProjectDto project = restTemplate.getForObject(url, ProjectDto.class);
            return project;
        } catch (Exception e) {
            logger.error("Error fetching project {}: {}", projectKey, e.getMessage());
            // Возвращаем базовую информацию если не удалось получить полную
            return new ProjectDto(projectKey, projectKey, null);
        }
    }

    public List<IssueDto> getProjectIssues(String projectKey, int maxResults) {
        String url = jiraBaseUrl + "/rest/api/2/search" +
                "?jql=project = \"" + projectKey + "\"" +
                "&maxResults=" + maxResults +
                "&fields=summary,status,assignee,reporter,created,updated,resolutiondate";

        logger.debug("Fetching issues for project: {}", projectKey);

        try {
            IssueSearchResultDto result = restTemplate.getForObject(url, IssueSearchResultDto.class);
            List<IssueDto> issues = result != null ? result.getIssues() : Collections.emptyList();
            return issues;
        } catch (Exception e) {
            logger.error("Error fetching issues for project {}: {}", projectKey, e.getMessage());
            return Collections.emptyList();
        }
    }
}