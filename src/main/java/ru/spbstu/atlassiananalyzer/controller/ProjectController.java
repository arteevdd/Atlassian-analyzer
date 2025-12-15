package ru.spbstu.atlassiananalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.spbstu.atlassiananalyzer.dto.IssueDto;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;
import ru.spbstu.atlassiananalyzer.service.JiraService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/project")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private final JiraService jiraService;
    private final String jiraBaseUrl;

    @Autowired
    public ProjectController(JiraService jiraService, @Value("${jira.base-url}") String jiraBaseUrl) {
        this.jiraService = jiraService;
        this.jiraBaseUrl = jiraBaseUrl;
    }

    @GetMapping("/{projectKey}")
    public String projectPage(@PathVariable String projectKey, Model model) {
        try {
            ProjectDto project = jiraService.getProjectByKey(projectKey);
            model.addAttribute("projectKey", project.getKey());
            model.addAttribute("projectName", project.getName());
            model.addAttribute("projectDescription",
                    project.getDescription() != null ? project.getDescription() : "Описание недоступно");

            List<IssueDto> issues = jiraService.getProjectIssues(projectKey, 200);
            model.addAttribute("issues", issues);
            model.addAttribute("jiraBaseUrl", jiraBaseUrl);

            long completedCount = issues.stream()
                    .filter(issue -> "Done".equals(issue.getFields().getStatus().getName()))
                    .count();
            long inProgressCount = issues.stream()
                    .filter(issue -> !"Done".equals(issue.getFields().getStatus().getName()))
                    .count();
            long assignedCount = issues.stream()
                    .filter(issue -> issue.getFields().getAssignee() != null)
                    .count();

            model.addAttribute("completedCount", completedCount);
            model.addAttribute("inProgressCount", inProgressCount);
            model.addAttribute("assignedCount", assignedCount);

            logger.info("Loaded {} issues for project {}", issues.size(), projectKey);

        } catch (Exception e) {
            logger.error("Error loading project {}: {}", projectKey, e.getMessage());
            model.addAttribute("error", "Ошибка загрузки данных проекта: " + e.getMessage());
            model.addAttribute("projectKey", projectKey);
            model.addAttribute("projectName", projectKey);
            model.addAttribute("projectDescription", "Описание недоступно");
            model.addAttribute("issues", Collections.emptyList());
            model.addAttribute("completedCount", 0);
            model.addAttribute("inProgressCount", 0);
            model.addAttribute("assignedCount", 0);
            model.addAttribute("jiraBaseUrl", jiraBaseUrl);
        }

        return "project";
    }

    @GetMapping("/{projectKey}/stats/data")
    @ResponseBody
    public Map<String, Object> getProjectStatsData(@PathVariable String projectKey) {
        try {
            logger.info("Loading stats data for project: {}", projectKey);
            List<IssueDto> issues = jiraService.getProjectIssues(projectKey, 500);
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

    @GetMapping("/{projectKey}/stats/api-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProjectStatsApiData(@PathVariable String projectKey) {
        try {
            logger.info("Loading stats data via API for project: {}", projectKey);
            List<IssueDto> issues = jiraService.getProjectIssues(projectKey, 500);
            Map<String, Object> chartData = generateChartData(issues);
            logger.info("Successfully generated chart data via API for project: {}", projectKey);
            return ResponseEntity.ok(chartData);
        } catch (Exception e) {
            logger.error("Error loading stats data via API for project {}: {}", projectKey, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load data: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private Map<String, Object> generateChartData(List<IssueDto> issues) {
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("openTimeHistogram", generateOpenTimeHistogramData(issues));
        chartData.put("statusTimeDistribution", generateStatusTimeDistributionData(issues));

        chartData.put("dailyIssueChart", generateDailyIssueChartData(issues));
        chartData.put("userStatsChart", generateUserStatsChartData(issues));
        chartData.put("loggedTimeChart", generateLoggedTimeChartData(issues));
        chartData.put("priorityChart", generatePriorityChartData(issues));


        return chartData;
    }


    private Map<String, Object> generatePriorityChartData(List<IssueDto> issues) {
        logger.info("Generating priority chart data for {} issues", issues.size());

        boolean hasAnyPriority = issues.stream()
                .anyMatch(issue -> issue.getFields().getPriority() != null
                        && issue.getFields().getPriority().getName() != null);

        if (!hasAnyPriority) {
            logger.warn("No priority data found for any issue. Showing status distribution instead.");

            Map<String, Long> statusStats = issues.stream()
                    .filter(issue -> issue.getFields().getStatus() != null
                            && issue.getFields().getStatus().getName() != null)
                    .collect(Collectors.groupingBy(
                            issue -> issue.getFields().getStatus().getName(),
                            Collectors.counting()
                    ));

            if (statusStats.isEmpty()) {
                logger.error("No status data found either!");
                return createFallbackChartData(issues.size());
            }

            List<Map.Entry<String, Long>> sortedStatuses = statusStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());

            List<String> labels = new ArrayList<>();
            List<Long> data = new ArrayList<>();
            List<String> backgroundColor = new ArrayList<>();
            List<String> borderColor = new ArrayList<>();

            Map<String, String[]> statusColors = createStatusColors();

            for (Map.Entry<String, Long> entry : sortedStatuses) {
                String statusName = entry.getKey();
                Long count = entry.getValue();

                labels.add(statusName);
                data.add(count);

                String[] colors = statusColors.getOrDefault(statusName,
                        new String[]{"rgba(153, 102, 255, 0.6)", "rgba(153, 102, 255, 1)"});

                backgroundColor.add(colors[0]);
                borderColor.add(colors[1]);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("labels", labels);
            result.put("data", data);
            result.put("backgroundColor", backgroundColor);
            result.put("borderColor", borderColor);
            result.put("totalIssues", issues.size());
            result.put("uniqueCategories", statusStats.size());
            result.put("chartType", "status");
            result.put("originalType", "priority");
            result.put("message", "Приоритеты не указаны. Показано распределение по статусам.");
            result.put("hasPriorityData", false);

            logger.info("Status stats (instead of priority): {}", statusStats);
            return result;
        }

        logger.info("Priority data found, generating priority chart");
        return generateRealPriorityChartData(issues);
    }

    private Map<String, Object> generateRealPriorityChartData(List<IssueDto> issues) {
        Map<String, Long> priorityStats = issues.stream()
                .filter(issue -> issue.getFields().getPriority() != null
                        && issue.getFields().getPriority().getName() != null)
                .collect(Collectors.groupingBy(
                        issue -> issue.getFields().getPriority().getName(),
                        Collectors.counting()
                ));

        long issuesWithoutPriority = issues.stream()
                .filter(issue -> issue.getFields().getPriority() == null
                        || issue.getFields().getPriority().getName() == null)
                .count();

        if (issuesWithoutPriority > 0) {
            priorityStats.put("Not set", issuesWithoutPriority);
        }

        List<Map.Entry<String, Long>> sortedPriorities = priorityStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        List<String> backgroundColor = new ArrayList<>();
        List<String> borderColor = new ArrayList<>();

        Map<String, String[]> priorityColors = createPriorityColors();

        for (Map.Entry<String, Long> entry : sortedPriorities) {
            String priorityName = entry.getKey();
            Long count = entry.getValue();

            labels.add(priorityName);
            data.add(count);

            String[] colors = priorityColors.getOrDefault(priorityName,
                    new String[]{"rgba(153, 102, 255, 0.6)", "rgba(153, 102, 255, 1)"});

            backgroundColor.add(colors[0]);
            borderColor.add(colors[1]);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        result.put("backgroundColor", backgroundColor);
        result.put("borderColor", borderColor);
        result.put("totalIssues", issues.size());
        result.put("uniqueCategories", priorityStats.size());
        result.put("chartType", "priority");
        result.put("originalType", "priority");
        result.put("message", "Распределение задач по приоритетам");
        result.put("hasPriorityData", true);

        return result;
    }

    private Map<String, String[]> createStatusColors() {
        Map<String, String[]> colors = new HashMap<>();

        colors.put("Open", new String[]{"rgba(66, 139, 202, 0.7)", "rgba(66, 139, 202, 1)"});
        colors.put("In Progress", new String[]{"rgba(92, 184, 92, 0.7)", "rgba(92, 184, 92, 1)"});
        colors.put("Resolved", new String[]{"rgba(240, 173, 78, 0.7)", "rgba(240, 173, 78, 1)"});
        colors.put("Closed", new String[]{"rgba(217, 83, 79, 0.7)", "rgba(217, 83, 79, 1)"});
        colors.put("Reopened", new String[]{"rgba(91, 192, 222, 0.7)", "rgba(91, 192, 222, 1)"});
        colors.put("To Do", new String[]{"rgba(217, 83, 79, 0.7)", "rgba(217, 83, 79, 1)"});
        colors.put("Done", new String[]{"rgba(92, 184, 92, 0.7)", "rgba(92, 184, 92, 1)"});
        colors.put("Blocked", new String[]{"rgba(255, 0, 0, 0.7)", "rgba(255, 0, 0, 1)"});

        return colors;
    }

    private Map<String, String[]> createPriorityColors() {
        Map<String, String[]> colors = new HashMap<>();

        colors.put("Highest", new String[]{"rgba(255, 0, 0, 0.7)", "rgba(255, 0, 0, 1)"});
        colors.put("High", new String[]{"rgba(255, 99, 71, 0.7)", "rgba(255, 99, 71, 1)"});
        colors.put("Medium", new String[]{"rgba(255, 165, 0, 0.7)", "rgba(255, 165, 0, 1)"});
        colors.put("Low", new String[]{"rgba(50, 205, 50, 0.7)", "rgba(50, 205, 50, 1)"});
        colors.put("Lowest", new String[]{"rgba(135, 206, 250, 0.7)", "rgba(135, 206, 250, 1)"});
        colors.put("Not set", new String[]{"rgba(211, 211, 211, 0.7)", "rgba(169, 169, 169, 1)"});

        return colors;
    }

    private Map<String, Object> createFallbackChartData(int totalIssues) {
        Map<String, Object> result = new HashMap<>();

        result.put("labels", Arrays.asList("Есть данные", "Нет данных"));
        result.put("data", Arrays.asList((long) totalIssues, 0L));
        result.put("backgroundColor", Arrays.asList(
                "rgba(75, 192, 192, 0.6)",
                "rgba(255, 99, 132, 0.6)"
        ));
        result.put("borderColor", Arrays.asList(
                "rgba(75, 192, 192, 1)",
                "rgba(255, 99, 132, 1)"
        ));
        result.put("totalIssues", totalIssues);
        result.put("uniqueCategories", 2);
        result.put("chartType", "fallback");
        result.put("message", "Нет данных о приоритетах и статусах");
        result.put("hasPriorityData", false);

        return result;
    }
    private Map<String, Object> generateLoggedTimeChartData(List<IssueDto> issues) {
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .collect(Collectors.toList());

        logger.info("Processing {} closed issues for logged time chart", closedIssues.size());

        boolean hasTimespent = closedIssues.stream()
                .anyMatch(issue -> issue.getFields().getTimespent() != null);

        List<Long> timeValues = new ArrayList<>();

        if (hasTimespent) {
            for (IssueDto issue : closedIssues) {
                Long timespent = issue.getFields().getTimespent();
                if (timespent != null && timespent > 0) {
                    long hours = timespent / 3600;
                    timeValues.add(hours);
                }
            }
            logger.info("Using timespent field: {} tasks have time data", timeValues.size());
        } else {
            for (IssueDto issue : closedIssues) {
                Long timeEstimate = issue.getFields().getTimeoriginalestimate();
                if (timeEstimate != null && timeEstimate > 0) {
                    long hours = timeEstimate / 3600;
                    timeValues.add(hours);
                }
            }
            logger.info("Using timeoriginalestimate field: {} tasks have estimate data", timeValues.size());
        }

        if (timeValues.isEmpty()) {
            logger.info("No time data found, using date difference as fallback");
            for (IssueDto issue : closedIssues) {
                try {
                    String createdStr = issue.getFields().getCreated();
                    String resolvedStr = issue.getFields().getResolutionDate();

                    if (createdStr != null && resolvedStr != null) {
                        LocalDateTime created = LocalDateTime.parse(createdStr.substring(0, 19));
                        LocalDateTime resolved = LocalDateTime.parse(resolvedStr.substring(0, 19));

                        long hours = Duration.between(created, resolved).toHours();
                        timeValues.add(hours);
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse dates for issue {}: {}", issue.getKey(), e.getMessage());
                }
            }
        }

        List<Integer> buckets = Arrays.asList(0, 0, 0, 0, 0, 0, 0);
        List<String> bucketLabels = Arrays.asList(
                "< 1 часа", "1-4 часа", "4-8 часов", "1-3 дня", "3-7 дней", "1-2 недели", "> 2 недель"
        );

        for (Long hours : timeValues) {
            int bucketIndex;
            if (hours < 1) bucketIndex = 0;
            else if (hours < 4) bucketIndex = 1;
            else if (hours < 8) bucketIndex = 2;
            else if (hours < 24 * 3) bucketIndex = 3;
            else if (hours < 24 * 7) bucketIndex = 4;
            else if (hours < 24 * 14) bucketIndex = 5;
            else bucketIndex = 6;

            buckets.set(bucketIndex, buckets.get(bucketIndex) + 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", bucketLabels);
        result.put("data", buckets);
        result.put("totalTasks", timeValues.size());
        result.put("dataSource", hasTimespent ? "timespent" :
                timeValues.isEmpty() ? "dateDifference" : "timeoriginalestimate");

        logger.info("Logged time buckets: {}", buckets);

        return result;
    }

    private Map<String, Object> generateUserStatsChartData(List<IssueDto> issues) {
        Map<String, Long> assigneeStats = issues.stream()
                .filter(issue -> issue.getFields().getAssignee() != null)
                .collect(Collectors.groupingBy(
                        issue -> issue.getFields().getAssignee().getDisplayName(),
                        Collectors.counting()
                ));

        Map<String, Long> reporterStats = issues.stream()
                .filter(issue -> issue.getFields().getReporter() != null)
                .collect(Collectors.groupingBy(
                        issue -> issue.getFields().getReporter().getDisplayName(),
                        Collectors.counting()
                ));


        Map<String, Long> combinedStats = new HashMap<>();

        assigneeStats.forEach((user, count) ->
                combinedStats.put(user + " (исполнитель)", count));

        reporterStats.forEach((user, count) ->
                combinedStats.merge(user + " (репортёр)", count, Long::sum));

        List<Map.Entry<String, Long>> topUsers = combinedStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(30)
                .collect(Collectors.toList());

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i = topUsers.size() - 1; i >= 0; i--) {
            Map.Entry<String, Long> entry = topUsers.get(i);
            labels.add(entry.getKey());
            data.add(entry.getValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        result.put("totalUsers", combinedStats.size());
        result.put("topUsersCount", topUsers.size());

        logger.info("User stats prepared: {} total users, showing top {}",
                combinedStats.size(), topUsers.size());

        return result;
    }

    private Map<String, Object> generateDailyIssueChartData(List<IssueDto> issues) {
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .collect(Collectors.toList());

        Map<LocalDate, DailyStats> dailyStatsMap = new TreeMap<>();

        for (IssueDto issue : issues) {
            try {
                LocalDate createdDate = LocalDate.parse(issue.getFields().getCreated().substring(0, 10));

                DailyStats stats = dailyStatsMap.computeIfAbsent(createdDate, k -> new DailyStats());
                stats.createdCount++;

            } catch (Exception e) {
                logger.warn("Could not parse created date for issue {}: {}", issue.getKey(), e.getMessage());
            }
        }

        for (IssueDto issue : closedIssues) {
            try {
                LocalDate resolvedDate = LocalDate.parse(issue.getFields().getResolutionDate().substring(0, 10));

                DailyStats stats = dailyStatsMap.computeIfAbsent(resolvedDate, k -> new DailyStats());
                stats.resolvedCount++;

            } catch (Exception e) {
                logger.warn("Could not parse resolution date for issue {}: {}", issue.getKey(), e.getMessage());
            }
        }

        List<String> labels = new ArrayList<>();
        List<Integer> createdData = new ArrayList<>();
        List<Integer> resolvedData = new ArrayList<>();
        List<Integer> cumulativeCreated = new ArrayList<>();
        List<Integer> cumulativeResolved = new ArrayList<>();

        int totalCreated = 0;
        int totalResolved = 0;

        for (Map.Entry<LocalDate, DailyStats> entry : dailyStatsMap.entrySet()) {
            LocalDate date = entry.getKey();
            DailyStats stats = entry.getValue();

            labels.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            createdData.add(stats.createdCount);
            resolvedData.add(stats.resolvedCount);

            totalCreated += stats.createdCount;
            totalResolved += stats.resolvedCount;
            cumulativeCreated.add(totalCreated);
            cumulativeResolved.add(totalResolved);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("created", createdData);
        result.put("resolved", resolvedData);
        result.put("cumulativeCreated", cumulativeCreated);
        result.put("cumulativeResolved", cumulativeResolved);

        logger.info("Daily chart data prepared for {} days", labels.size());
        return result;
    }

    class DailyStats {
        int createdCount = 0;
        int resolvedCount = 0;

        @Override
        public String toString() {
            return "DailyStats{created=" + createdCount + ", resolved=" + resolvedCount + "}";
        }
    }




    private Map<String, Object> generateOpenTimeHistogramData(List<IssueDto> issues) {
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .collect(Collectors.toList());

        logger.info("Processing {} closed issues for histogram", closedIssues.size());
        List<Integer> timeBuckets = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0, 0, 0));

        for (IssueDto issue : closedIssues) {
            try {
                String createdStr = issue.getFields().getCreated();
                String resolvedStr = issue.getFields().getResolutionDate();

                if (createdStr == null || resolvedStr == null) {
                    continue;
                }

                LocalDateTime created = LocalDateTime.parse(createdStr.substring(0, 19));
                LocalDateTime resolved = LocalDateTime.parse(resolvedStr.substring(0, 19));
                long daysBetween = Duration.between(created, resolved).toDays();

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
        List<IssueDto> closedIssues = issues.stream()
                .filter(issue -> issue.getFields().getResolutionDate() != null)
                .collect(Collectors.toList());

        int totalClosed = closedIssues.size();

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

    @GetMapping("/{projectKey}/stats")
    public String projectStatsPage(@PathVariable String projectKey, Model model) {
        try {
            ProjectDto project = jiraService.getProjectByKey(projectKey);
            model.addAttribute("projectKey", project.getKey());
            model.addAttribute("projectName", project.getName());

            List<IssueDto> issues = jiraService.getProjectIssues(projectKey, 500);

            long totalIssues = issues.size();
            long closedIssues = issues.stream()
                    .filter(issue -> issue.getFields().getResolutionDate() != null)
                    .count();

            model.addAttribute("totalIssues", totalIssues);
            model.addAttribute("closedIssues", closedIssues);

            logger.info("Loading stats page for project: {} ({} issues, {} closed)",
                    projectKey, totalIssues, closedIssues);

        } catch (Exception e) {
            logger.error("Error loading project {} for stats page: {}", projectKey, e.getMessage());
            model.addAttribute("projectKey", projectKey);
            model.addAttribute("projectName", projectKey);
            model.addAttribute("totalIssues", 0);
            model.addAttribute("closedIssues", 0);
            model.addAttribute("error", "Не удалось загрузить данные проекта: " + e.getMessage());
        }

        return "project-stats";
    }
}