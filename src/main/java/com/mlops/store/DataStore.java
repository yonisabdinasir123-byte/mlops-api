package com.mlops.store;

import com.mlops.model.EvaluationMetric;
import com.mlops.model.MLWorkspace;
import com.mlops.model.MachineLearningModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store. The coursework forbids any database technology, so all
 * state is held in HashMaps / ArrayLists. Implemented as a thread-safe
 * singleton so every resource shares the same data for the life of the server.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, MLWorkspace> workspaces = new ConcurrentHashMap<>();
    private final Map<String, MachineLearningModel> models = new ConcurrentHashMap<>();
    private final Map<String, List<EvaluationMetric>> metricsByModel = new ConcurrentHashMap<>();

    private DataStore() {
        seedSampleData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ---------- Workspaces ----------
    public Collection<MLWorkspace> getAllWorkspaces() {
        return workspaces.values();
    }

    public MLWorkspace getWorkspace(String id) {
        return workspaces.get(id);
    }

    public void saveWorkspace(MLWorkspace workspace) {
        workspaces.put(workspace.getId(), workspace);
    }

    public MLWorkspace removeWorkspace(String id) {
        return workspaces.remove(id);
    }

    public boolean workspaceExists(String id) {
        return workspaces.containsKey(id);
    }

    // ---------- Models ----------
    public Collection<MachineLearningModel> getAllModels() {
        return models.values();
    }

    public MachineLearningModel getModel(String id) {
        return models.get(id);
    }

    public void saveModel(MachineLearningModel model) {
        models.put(model.getId(), model);
    }

    // ---------- Metrics ----------
    public List<EvaluationMetric> getMetrics(String modelId) {
        return metricsByModel.computeIfAbsent(modelId, k -> new ArrayList<>());
    }

    public void addMetric(String modelId, EvaluationMetric metric) {
        getMetrics(modelId).add(metric);
    }

    /** Seed a little sample data so the API has something to return on first run. */
    private void seedSampleData() {
        MLWorkspace vision = new MLWorkspace("WS-VISION-01", "Computer Vision Lab", 500);
        MLWorkspace nlp = new MLWorkspace("WS-NLP-02", "Language Models Team", 1000);
        workspaces.put(vision.getId(), vision);
        workspaces.put(nlp.getId(), nlp);

        MachineLearningModel resnet =
                new MachineLearningModel("MOD-8832", "TensorFlow", "DEPLOYED", 0.94, "WS-VISION-01");
        vision.getModelIds().add(resnet.getId());
        models.put(resnet.getId(), resnet);
    }
}
