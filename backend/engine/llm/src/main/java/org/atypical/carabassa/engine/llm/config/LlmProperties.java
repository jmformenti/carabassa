package org.atypical.carabassa.engine.llm.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carabassa.llm")
public class LlmProperties {

    private String url;
    private String model;
    private String apiKey;
    private int maxIterations = 5;
    private String promptFile;

    public boolean isEnabled() {
        return StringUtils.isNotBlank(url) && StringUtils.isNotBlank(model);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException(
                    "carabassa.llm.max-iterations must be > 0 (got " + maxIterations + ")");
        }
        this.maxIterations = maxIterations;
    }

    public String getPromptFile() {
        return promptFile;
    }

    public void setPromptFile(String promptFile) {
        this.promptFile = promptFile;
    }
}
