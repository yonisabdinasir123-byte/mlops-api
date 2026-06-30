package com.mlops.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Machine Learning Workspace - a logical container that owns a
 * collection of deployed models for a given team.
 */
public class MLWorkspace {

    private String id;                       // Unique identifier, e.g. "WS-VISION-01"
    private String teamName;                 // Human-readable team name
    private int storageQuotaGb;              // Max storage allocated for datasets (GB)
    private List<String> modelIds = new ArrayList<>(); // IDs of models deployed here

    public MLWorkspace() {
    }

    public MLWorkspace(String id, String teamName, int storageQuotaGb) {
        this.id = id;
        this.teamName = teamName;
        this.storageQuotaGb = storageQuotaGb;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getStorageQuotaGb() {
        return storageQuotaGb;
    }

    public void setStorageQuotaGb(int storageQuotaGb) {
        this.storageQuotaGb = storageQuotaGb;
    }

    public List<String> getModelIds() {
        return modelIds;
    }

    public void setModelIds(List<String> modelIds) {
        this.modelIds = modelIds;
    }
}
