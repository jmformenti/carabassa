package org.atypical.carabassa.engine.llm.prompt;

import org.apache.commons.lang3.StringUtils;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.engine.llm.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class SystemPromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SystemPromptBuilder.class);
    private static final String DEFAULT_PROMPT_RESOURCE = "llm/system-prompt.txt";
    private static final String TAGS_PLACEHOLDER = "{{tags}}";
    private static final long MAX_PROMPT_FILE_SIZE = 1024L * 1024L; // 1 MiB

    private final LlmProperties llmProperties;
    private final String defaultTemplate;

    public SystemPromptBuilder(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
        this.defaultTemplate = loadDefaultTemplate();
    }

    public String build(List<? extends TagInfo> tagInfos) {
        String template = readOverride();
        if (template == null) {
            template = defaultTemplate;
        }
        String tagsBlock = renderTags(tagInfos);
        return template.contains(TAGS_PLACEHOLDER)
                ? template.replace(TAGS_PLACEHOLDER, tagsBlock)
                : template + "\n\nAvailable tags:\n" + tagsBlock;
    }

    private String renderTags(List<? extends TagInfo> tagInfos) {
        if (tagInfos == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TagInfo info : tagInfos) {
            if (Boolean.TRUE.equals(info.getInternal())) {
                continue;
            }
            sb.append("- ").append(info.getTagName());
            if (StringUtils.isNotBlank(info.getAlias())) {
                sb.append(" (alias: ").append(info.getAlias()).append(")");
            }
            sb.append(", type=").append(info.getType());
            if (StringUtils.isNotBlank(info.getDescription())) {
                sb.append(" — ").append(info.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String readOverride() {
        String path = llmProperties.getPromptFile();
        if (StringUtils.isBlank(path)) {
            return null;
        }
        Path file = Path.of(path);
        if (!Files.isRegularFile(file)) {
            logger.warn("LLM prompt file '{}' not found; falling back to default prompt.", path);
            return null;
        }
        try {
            long size = Files.size(file);
            if (size > MAX_PROMPT_FILE_SIZE) {
                logger.warn("LLM prompt file '{}' is too large ({} bytes, max {}); falling back to default prompt.",
                        path, size, MAX_PROMPT_FILE_SIZE);
                return null;
            }
            return Files.readString(file);
        } catch (IOException e) {
            logger.warn("Failed to read LLM prompt file '{}': {}", path, e.getMessage());
            return null;
        }
    }

    private static String loadDefaultTemplate() {
        try (var input = new ClassPathResource(DEFAULT_PROMPT_RESOURCE).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load default LLM prompt resource: " + DEFAULT_PROMPT_RESOURCE, e);
        }
    }
}
