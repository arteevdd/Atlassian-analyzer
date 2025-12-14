package ru.spbstu.atlassiananalyzer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueDto {

    @JsonProperty("key")
    private String key;

    @JsonProperty("fields")
    private Fields fields;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {

        @JsonProperty("summary")
        private String summary;

        @JsonProperty("status")
        private Status status;

        @JsonProperty("assignee")
        private User assignee;

        @JsonProperty("reporter")
        private User reporter;

        @JsonProperty("created")
        private String created;

        @JsonProperty("updated")
        private String updated;

        @JsonProperty("resolutiondate")
        private String resolutionDate;

        @JsonProperty("timespent")
        private Long timespent;

        @JsonProperty("timeoriginalestimate")
        private Long timeoriginalestimate;

        @JsonProperty("timetracking")
        private TimeTracking timetracking;

        @JsonProperty("priority")
        private Priority priority;

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        // Геттеры и сеттеры
        public Long getTimespent() {
            return timespent;
        }

        public void setTimespent(Long timespent) {
            this.timespent = timespent;
        }

        public Long getTimeoriginalestimate() {
            return timeoriginalestimate;
        }

        public void setTimeoriginalestimate(Long timeoriginalestimate) {
            this.timeoriginalestimate = timeoriginalestimate;
        }

        public TimeTracking getTimetracking() {
            return timetracking;
        }

        public void setTimetracking(TimeTracking timetracking) {
            this.timetracking = timetracking;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public User getAssignee() {
            return assignee;
        }

        public void setAssignee(User assignee) {
            this.assignee = assignee;
        }

        public User getReporter() {
            return reporter;
        }

        public void setReporter(User reporter) {
            this.reporter = reporter;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getUpdated() {
            return updated;
        }

        public void setUpdated(String updated) {
            this.updated = updated;
        }

        public String getResolutionDate() {
            return resolutionDate;
        }

        public void setResolutionDate(String resolutionDate) {
            this.resolutionDate = resolutionDate;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {

        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Priority {
        @JsonProperty("name")
        private String name;

        @JsonProperty("id")
        private String id;

        @JsonProperty("iconUrl")
        private String iconUrl;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getIconUrl() {
            return iconUrl;
        }

        public void setIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {

        @JsonProperty("displayName")
        private String displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    // Дополнительный класс для timetracking
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeTracking {
        @JsonProperty("originalEstimate")
        private String originalEstimate;

        @JsonProperty("remainingEstimate")
        private String remainingEstimate;

        @JsonProperty("timeSpent")
        private String timeSpent;

        public String getOriginalEstimate() {
            return originalEstimate;
        }

        public void setOriginalEstimate(String originalEstimate) {
            this.originalEstimate = originalEstimate;
        }

        public String getRemainingEstimate() {
            return remainingEstimate;
        }

        public void setRemainingEstimate(String remainingEstimate) {
            this.remainingEstimate = remainingEstimate;
        }

        public String getTimeSpent() {
            return timeSpent;
        }

        public void setTimeSpent(String timeSpent) {
            this.timeSpent = timeSpent;
        }
    }
}