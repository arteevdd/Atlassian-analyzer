package ru.spbstu.atlassiananalyzer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;
import ru.spbstu.atlassiananalyzer.service.JiraService;

import java.util.List;

@Controller
@RequestMapping("/")
public class MainController {

    private final JiraService jiraService;

    @Autowired
    public MainController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping
    public String index(Model model) {
        List<ProjectDto> projects = jiraService.getAllProjects();
        model.addAttribute("projects", projects);
        return "index";
    }
}