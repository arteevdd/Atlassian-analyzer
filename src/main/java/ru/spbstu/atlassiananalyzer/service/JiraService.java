package ru.spbstu.atlassiananalyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.spbstu.atlassiananalyzer.dto.IssueChangelogDto;
import ru.spbstu.atlassiananalyzer.dto.IssueDto;
import ru.spbstu.atlassiananalyzer.dto.IssueSearchResultDto;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
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
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public List<ProjectDto> getAllProjects() {
        String url = jiraBaseUrl + "/rest/api/2/project";
        logger.info("Fetching projects from: {}", url);

        ProjectDto[] projects = restTemplate.getForObject(url, ProjectDto[].class);
        List<ProjectDto> projectList = Arrays.asList(projects);
        logger.info("Fetched {} projects", projectList.size());

        List<ProjectDto> limitedProjects = projectList.stream()
                .limit(100)
                .collect(Collectors.toList());

        List<CompletableFuture<ProjectDto>> futures = limitedProjects.stream()
                .map(project -> CompletableFuture.supplyAsync(() -> getProjectByKey(project.getKey()), executorService))
                .collect(Collectors.toList());

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
            return new ProjectDto(projectKey, projectKey, null);
        }
    }

    public List<IssueDto> getProjectIssues(String projectKey, int maxResults) {
        String url = jiraBaseUrl + "/rest/api/2/search" +
                "?jql=project = \"" + projectKey + "\"" +
                "&maxResults=" + maxResults +
                "&fields=summary,status,assignee,reporter,created,updated,resolutiondate," +
                "timespent,timeoriginalestimate,priority";

        logger.debug("Fetching issues for project: {}", projectKey);

        try {
            IssueSearchResultDto result = restTemplate.getForObject(url, IssueSearchResultDto.class);
            List<IssueDto> issues = result != null ? result.getIssues() : Collections.emptyList();

            long issuesWithPriority = issues.stream()
                    .filter(issue -> issue.getFields().getPriority() != null)
                    .count();
            logger.info("Fetched {} issues, {} have priority data", issues.size(), issuesWithPriority);

            return issues;
        } catch (Exception e) {
            logger.error("Error fetching issues for project {}: {}", projectKey, e.getMessage());
            return Collections.emptyList();
        }
    }


    /**
     * Получить историю изменений задачи
     */
    public IssueChangelogDto getIssueChangelog(String issueKey) {
        String url = jiraBaseUrl + "/rest/api/2/issue/" + issueKey + "/changelog";

        try {
            IssueChangelogDto changelog = restTemplate.getForObject(url, IssueChangelogDto.class);
            return changelog;
        } catch (Exception e) {
            logger.error("Error fetching changelog for issue {}: {}", issueKey, e.getMessage());
            return new IssueChangelogDto();
        }
    }

    /**
     * Получить распределение времени по состояниям для задачи
     */
    public Map<String, Long> getIssueStatusTimeDistribution(String issueKey) {
        IssueDto issue = getIssueByKey(issueKey);
        IssueChangelogDto changelog = getIssueChangelog(issueKey);

        if (issue == null || changelog == null || changelog.getHistories() == null) {
            return Collections.emptyMap();
        }

        return calculateStatusTimeDistribution(issue, changelog);
    }

    /**
     * Получить задачу по ключу
     */
    private IssueDto getIssueByKey(String issueKey) {
        String url = jiraBaseUrl + "/rest/api/2/issue/" + issueKey +
                "?fields=created,status,resolutiondate";

        try {
            return restTemplate.getForObject(url, IssueDto.class);
        } catch (Exception e) {
            logger.error("Error fetching issue {}: {}", issueKey, e.getMessage());
            return null;
        }
    }

    /**
     * Рассчитать время в каждом статусе
     */
    private Map<String, Long> calculateStatusTimeDistribution(IssueDto issue, IssueChangelogDto changelog) {
        Map<String, Long> statusTimeMap = new HashMap<>();

        // Сортировка истории по времени
        List<IssueChangelogDto.History> histories = changelog.getHistories();
        histories.sort(Comparator.comparing(IssueChangelogDto.History::getCreated));

        // Начальные условия
        String currentStatus = extractInitialStatus(issue, histories);
        LocalDateTime lastStatusChange = parseDateTime(issue.getFields().getCreated());
        LocalDateTime currentTime;

        // Обработка каждого изменения статуса
        for (IssueChangelogDto.History history : histories) {
            for (IssueChangelogDto.ChangeItem item : history.getItems()) {
                if ("status".equals(item.getField())) {
                    currentTime = parseDateTime(history.getCreated());

                    // Добавляем время в предыдущем статусе
                    long timeInStatus = Duration.between(lastStatusChange, currentTime).toHours();
                    statusTimeMap.merge(currentStatus, timeInStatus, Long::sum);

                    // Обновляем статус
                    currentStatus = item.getToString();
                    lastStatusChange = currentTime;
                }
            }
        }

        // Добавляем время в последнем статусе до закрытия или текущего момента
        LocalDateTime endTime = issue.getFields().getResolutionDate() != null
                ? parseDateTime(issue.getFields().getResolutionDate())
                : LocalDateTime.now();

        long finalTimeInStatus = Duration.between(lastStatusChange, endTime).toHours();
        statusTimeMap.merge(currentStatus, finalTimeInStatus, Long::sum);

        return statusTimeMap;
    }

    /**
     * Извлечь начальный статус
     */
    private String extractInitialStatus(IssueDto issue, List<IssueChangelogDto.History> histories) {
        // Ищем самое первое изменение статуса в истории
        for (IssueChangelogDto.History history : histories) {
            for (IssueChangelogDto.ChangeItem item : history.getItems()) {
                if ("status".equals(item.getField()) && item.getFromString() != null) {
                    return item.getFromString();
                }
            }
        }

        // Если не нашли в истории, берем текущий статус
        return issue.getFields().getStatus() != null
                ? issue.getFields().getStatus().getName()
                : "Unknown";
    }

    /**
     * Парсить дату-время из строки
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr.substring(0, 19));
        } catch (Exception e) {
            logger.warn("Could not parse datetime: {}", dateTimeStr);
            return LocalDateTime.now();
        }
    }

    /**
     * Получить агрегированное распределение времени по состояниям для проекта
     */
    public Map<String, Map<String, Long>> getProjectStatusTimeDistribution(String projectKey, int sampleSize) {
        // Получаем задачи проекта
        List<IssueDto> issues = getProjectIssues(projectKey, sampleSize);
        Map<String, Map<String, Long>> projectStatusTime = new HashMap<>();

        // Ограничиваем количество задач для производительности
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .limit(50) // Ограничиваем для производительности
                .collect(java.util.stream.Collectors.toList());

        logger.info("Analyzing status time distribution for {} closed issues", closedIssues.size());

        // Для каждой задачи получаем распределение времени по статусам
        for (IssueDto issue : closedIssues) {
            try {
                Map<String, Long> issueStatusTime = getIssueStatusTimeDistribution(issue.getKey());

                // Агрегируем в проектную статистику
                for (Map.Entry<String, Long> entry : issueStatusTime.entrySet()) {
                    String status = entry.getKey();
                    Long time = entry.getValue();

                    Map<String, Long> statusMap = projectStatusTime.computeIfAbsent(status, k -> new HashMap<>());

                    // Группируем время в бакеты (в часах)
                    String timeBucket = getTimeBucket(time);
                    statusMap.merge(timeBucket, 1L, Long::sum);
                }
            } catch (Exception e) {
                logger.warn("Could not analyze issue {}: {}", issue.getKey(), e.getMessage());
            }
        }

        return projectStatusTime;
    }

    /**
     * Определить бакет времени
     */
    private String getTimeBucket(long hours) {
        if (hours < 1) return "< 1 часа";
        else if (hours < 4) return "1-4 часа";
        else if (hours < 24) return "4-24 часа";
        else if (hours < 24 * 3) return "1-3 дня";
        else return "> 3 дней";
    }
}