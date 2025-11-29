package ru.spbstu.atlassiananalyzer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDto {

    @JsonProperty("key")
    private String key;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    // Конструкторы
    public ProjectDto() {}

    public ProjectDto(String key, String name, String description) {
        this.key = key;
        this.name = name;
        this.description = description;
    }

    // Геттеры и сеттеры
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ProjectDto{key='" + key + "', name='" + name + "', description='" + description + "'}";
    }
}