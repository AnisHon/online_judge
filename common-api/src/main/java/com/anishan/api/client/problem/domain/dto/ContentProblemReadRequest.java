package com.anishan.api.client.problem.domain.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/** Read-only problem identifiers requested by content-service. Caller identity is never accepted here. */
public class ContentProblemReadRequest {
    private List<String> problemIds;
    @JsonIgnore
    private boolean unsupportedProperties;

    @JsonAnySetter
    public void recordUnsupportedProperty(String property, Object value) {
        unsupportedProperties = true;
    }

    public List<String> getProblemIds() {
        return problemIds;
    }

    public void setProblemIds(List<String> problemIds) {
        this.problemIds = problemIds;
    }

    public boolean hasUnsupportedProperties() {
        return unsupportedProperties;
    }
}
