package ru.spbstu.atlassiananalyzer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.spbstu.atlassiananalyzer.dto.ProjectDto;
import ru.spbstu.atlassiananalyzer.service.JiraService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private final JiraService jiraService;

    @Autowired
    public MainController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping
    public String index(Model model) {
        List<ProjectDto> projects = jiraService.getAllProjects();

        List<ProjectDto> cleanedProjects = projects.stream()
                .map(this::cleanProjectDescription)
                .collect(Collectors.toList());

        long withDescriptions = cleanedProjects.stream()
                .filter(p -> p.getDescription() != null)
                .count();
        logger.info("After cleaning: {}/{} projects have descriptions", withDescriptions, cleanedProjects.size());

        model.addAttribute("projects", cleanedProjects);
        return "index";
    }

    private ProjectDto cleanProjectDescription(ProjectDto project) {
        boolean isRetired = project.getName().contains("(Retired)");

        ProjectDto cleanedProject = new ProjectDto();
        cleanedProject.setKey(project.getKey());
        cleanedProject.setName(project.getName());

        if (isRetired) {
            cleanedProject.setDescription(null);
        } else {
            String cleanedDescription = project.getDescription();
            if (cleanedDescription != null && !cleanedDescription.equals("null") && !cleanedDescription.isEmpty()) {
                cleanedDescription = cleanedDescription
                        .replaceAll("<[^>]*>", "")
                        .replaceAll("\\s+", " ")
                        .trim();

                if (cleanedDescription.isEmpty()) {
                    cleanedDescription = null;
                } else if (cleanedDescription.length() > 120) {
                    cleanedDescription = cleanedDescription.substring(0, 117) + "...";
                }
            } else {
                cleanedDescription = null;
            }
            cleanedProject.setDescription(cleanedDescription);
        }

        return cleanedProject;
    }
}