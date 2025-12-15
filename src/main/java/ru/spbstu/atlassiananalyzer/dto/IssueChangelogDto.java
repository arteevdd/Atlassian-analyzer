package ru.spbstu.atlassiananalyzer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueChangelogDto {

    @JsonProperty("histories")
    private List<History> histories;

    public List<History> getHistories() {
        return histories;
    }

    public void setHistories(List<History> histories) {
        this.histories = histories;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class History {
        @JsonProperty("created")
        private String created;

        @JsonProperty("items")
        private List<ChangeItem> items;

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public List<ChangeItem> getItems() {
            return items;
        }

        public void setItems(List<ChangeItem> items) {
            this.items = items;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeItem {
        @JsonProperty("field")
        private String field;

        @JsonProperty("fromString")
        private String fromString;

        @JsonProperty("toString")
        private String toString;

        @JsonProperty("fieldtype")
        private String fieldType;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getFromString() {
            return fromString;
        }

        public void setFromString(String fromString) {
            this.fromString = fromString;
        }

        public String getToString() {
            return toString;
        }

        public void setToString(String toString) {
            this.toString = toString;
        }

        public String getFieldType() {
            return fieldType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }
    }
}