package ru.spbstu.atlassiananalyzer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.spbstu.atlassiananalyzer.dto.IssueDto;
import ru.spbstu.atlassiananalyzer.service.JiraService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/project")
public class ProjectStatsApiController {

    private final JiraService jiraService;
    private static final Logger logger = LoggerFactory.getLogger(ProjectStatsApiController.class);

    @Autowired
    public ProjectStatsApiController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/{projectKey}/stats/data")
    public Map<String, Object> getProjectStatsData(@PathVariable String projectKey) {
        try {
            logger.info("Loading stats data for project: {}", projectKey);

            // Получаем задачи проекта
            List<IssueDto> issues = jiraService.getProjectIssues(projectKey, 500);

            // Генерируем данные для графиков
            Map<String, Object> chartData = generateChartData(issues);

            logger.info("Successfully generated chart data for project: {}", projectKey);
            return chartData;

        } catch (Exception e) {
            logger.error("Error loading stats data for project {}: {}", projectKey, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load data: " + e.getMessage());
            return errorResponse;
        }
    }

    private Map<String, Object> generateChartData(List<IssueDto> issues) {
        Map<String, Object> chartData = new HashMap<>();

        // Данные для гистограммы времени в открытом состоянии
        chartData.put("openTimeHistogram", generateOpenTimeHistogramData(issues));

        // Данные для диаграмм по состояниям
        chartData.put("statusTimeDistribution", generateStatusTimeDistributionData(issues));

        return chartData;
    }

    private Map<String, Object> generateOpenTimeHistogramData(List<IssueDto> issues) {
        // Фильтруем только закрытые задачи
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .collect(Collectors.toList());

        logger.info("Processing {} closed issues for histogram", closedIssues.size());

        // Инициализируем временные интервалы
        List<Integer> timeBuckets = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0));

        for (IssueDto issue : closedIssues) {
            try {
                String createdStr = issue.getFields().getCreated();
                String resolvedStr = issue.getFields().getResolutionDate();

                if (createdStr == null || resolvedStr == null) {
                    logger.debug("Skipping issue {} with null dates", issue.getKey());
                    continue;
                }

                LocalDateTime created = LocalDateTime.parse(createdStr.substring(0, 19));
                LocalDateTime resolved = LocalDateTime.parse(resolvedStr.substring(0, 19));

                long daysBetween = Duration.between(created, resolved).toDays();

                // Распределяем по интервалам
                int bucketIndex;
                if (daysBetween < 1) bucketIndex = 0;
                else if (daysBetween < 3) bucketIndex = 1;
                else if (daysBetween < 7) bucketIndex = 2;
                else if (daysBetween < 14) bucketIndex = 3;
                else if (daysBetween < 28) bucketIndex = 4;
                else if (daysBetween < 90) bucketIndex = 5;
                else bucketIndex = 6;

                timeBuckets.set(bucketIndex, timeBuckets.get(bucketIndex) + 1);

            } catch (Exception e) {
                logger.warn("Could not parse dates for issue {}: {}", issue.getKey(), e.getMessage());
            }
        }

        logger.info("Time buckets distribution: {}", timeBuckets);

        Map<String, Object> result = new HashMap<>();
        result.put("labels", Arrays.asList("< 1 дня", "1-3 дня", "3-7 дней", "1-2 недели", "2-4 недели", "1-3 месяца", "> 3 месяцев"));
        result.put("data", timeBuckets);

        return result;
    }

    private Map<String, Object> generateStatusTimeDistributionData(List<IssueDto> issues) {
        Map<String, Object> result = new HashMap<>();

        // Пока используем тестовые данные
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .collect(Collectors.toList());

        int totalClosed = closedIssues.size();
        logger.info("Generating status distribution for {} closed issues", totalClosed);

        // Простое распределение для демонстрации
        Map<String, Object> todoData = new HashMap<>();
        todoData.put("labels", Arrays.asList("< 1 часа", "1-4 часа", "4-24 часа", "1-3 дня", "> 3 дней"));
        todoData.put("data", Arrays.asList(
                Math.min(10, totalClosed / 10),
                Math.min(20, totalClosed / 5),
                Math.min(30, totalClosed / 3),
                Math.min(15, totalClosed / 7),
                Math.min(5, totalClosed / 20)
        ));

        Map<String, Object> inProgressData = new HashMap<>();
        inProgressData.put("labels", Arrays.asList("< 1 часа", "1-4 часа", "4-24 часа", "1-3 дня", "> 3 дней"));
        inProgressData.put("data", Arrays.asList(
                Math.min(15, totalClosed / 8),
                Math.min(25, totalClosed / 4),
                Math.min(35, totalClosed / 2),
                Math.min(20, totalClosed / 6),
                Math.min(8, totalClosed / 15)
        ));

        Map<String, Object> codeReviewData = new HashMap<>();
        codeReviewData.put("labels", Arrays.asList("< 1 часа", "1-4 часа", "4-24 часа", "1-3 дня", "> 3 дней"));
        codeReviewData.put("data", Arrays.asList(
                Math.min(8, totalClosed / 12),
                Math.min(18, totalClosed / 6),
                Math.min(25, totalClosed / 4),
                Math.min(12, totalClosed / 8),
                Math.min(4, totalClosed / 25)
        ));

        Map<String, Object> testingData = new HashMap<>();
        testingData.put("labels", Arrays.asList("< 1 часа", "1-4 часа", "4-24 часа", "1-3 дня", "> 3 дней"));
        testingData.put("data", Arrays.asList(
                Math.min(5, totalClosed / 15),
                Math.min(12, totalClosed / 8),
                Math.min(20, totalClosed / 5),
                Math.min(10, totalClosed / 10),
                Math.min(3, totalClosed / 30)
        ));

        result.put("To Do", todoData);
        result.put("In Progress", inProgressData);
        result.put("Code Review", codeReviewData);
        result.put("Testing", testingData);

        return result;
    }
}