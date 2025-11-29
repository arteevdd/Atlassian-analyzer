package ru.spbstu.atlassiananalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;
import ru.spbstu.atlassiananalyzer.service.JiraService;

@Controller
@RequestMapping("/project")
public class ProjectController {

    private final JiraService jiraService;

    @Autowired
    public ProjectController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/{projectKey}")
    public String projectPage(@PathVariable String projectKey, Model model) {
        ProjectDto project = jiraService.getProjectByKey(projectKey);
        model.addAttribute("projectKey", project.getKey());
        model.addAttribute("projectName", project.getName());
        model.addAttribute("projectDescription",
                project.getDescription() != null ? project.getDescription() : "Описание недоступно");
        return "project";
    }
}