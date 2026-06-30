package com.mlops.model;

/**
 * A single evaluation metric event captured for a model during a monitoring /
 * evaluation run. Kept as a historical log entry to track concept drift.
 */
public class EvaluationMetric {

    private String id;             // Unique metric event ID (UUID)
    private long timestamp;        // Epoch time (ms) when captured
    private double accuracyScore;  // Accuracy / F1 score recorded during the run

    public EvaluationMetric() {
    }

    public EvaluationMetric(String id, long timestamp, double accuracyScore) {
        this.id = id;
        this.timestamp = timestamp;
        this.accuracyScore = accuracyScore;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getAccuracyScore() {
        return accuracyScore;
    }

    public void setAccuracyScore(double accuracyScore) {
        this.accuracyScore = accuracyScore;
    }
}
