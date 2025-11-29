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
}