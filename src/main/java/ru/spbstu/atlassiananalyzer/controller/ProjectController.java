package ru.spbstu.atlassiananalyzer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.spbstu.atlassiananalyzer.dto.IssueDto;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;
import ru.spbstu.atlassiananalyzer.service.JiraService;

import java.util.Collections;
import java.util.List;

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
            // Получаем информацию о проекте
            ProjectDto project = jiraService.getProjectByKey(projectKey);
            model.addAttribute("projectKey", project.getKey());
            model.addAttribute("projectName", project.getName());
            model.addAttribute("projectDescription",
                    project.getDescription() != null ? project.getDescription() : "Описание недоступно");

            // Получаем задачи проекта
            List<IssueDto> issues = jiraService.getProjectIssues(projectKey, 200);
            model.addAttribute("issues", issues);

            // Передаем базовый URL JIRA для ссылок
            model.addAttribute("jiraBaseUrl", jiraBaseUrl);

            // Рассчитываем статистику
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
}