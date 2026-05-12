package org.atypical.carabassa.engine.llm.impl;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.core.service.TagInfoService;
import org.atypical.carabassa.engine.llm.LlmSearchService;
import org.atypical.carabassa.engine.llm.prompt.SystemPromptBuilder;
import org.atypical.carabassa.engine.llm.tool.SearchTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class LlmSearchServiceImpl implements LlmSearchService {

    private static final Logger logger = LoggerFactory.getLogger(LlmSearchServiceImpl.class);

    private static final int TAG_INFO_PAGE_SIZE = 200;

    private final ChatClient chatClient;
    private final DatasetService datasetService;
    private final TagInfoService tagInfoService;
    private final SystemPromptBuilder systemPromptBuilder;
    private final ZoneId zoneId;

    public LlmSearchServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                DatasetService datasetService,
                                TagInfoService tagInfoService,
                                SystemPromptBuilder systemPromptBuilder,
                                @Value("${carabassa.default-tz:UTC}") String defaultTimeZone) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = (builder != null) ? builder.build() : null;
        this.datasetService = datasetService;
        this.tagInfoService = tagInfoService;
        this.systemPromptBuilder = systemPromptBuilder;
        this.zoneId = parseZoneId(defaultTimeZone);
    }

    private static ZoneId parseZoneId(String defaultTimeZone) {
        try {
            return ZoneId.of(defaultTimeZone);
        } catch (DateTimeException e) {
            ZoneId fallback = ZoneId.systemDefault();
            logger.warn("Invalid carabassa.default-tz '{}'; falling back to {}.", defaultTimeZone, fallback);
            return fallback;
        }
    }

    @Override
    public AskResult ask(Dataset dataset, String question) {
        if (chatClient == null) {
            throw new IllegalStateException("LLM client is not available.");
        }
        List<TagInfo> tagInfos = loadAllTagInfos();
        String systemPrompt = systemPromptBuilder.build(tagInfos);

        SearchTools tools = new SearchTools(dataset, datasetService, zoneId);
        String summary;
        try {
            summary = chatClient.prompt()
                    .system(systemPrompt)
                    .user(question)
                    .tools(tools)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            logger.error("LLM invocation failed for dataset {}", dataset.getId(), e);
            throw new IllegalStateException("LLM invocation failed", e);
        }

        Page<IndexedItem> page = tools.getLastPage();
        return new AskResult(summary == null ? "" : summary.trim(), tools.getLastQuery(), page);
    }

    private List<TagInfo> loadAllTagInfos() {
        List<TagInfo> all = new ArrayList<>();
        int pageNumber = 0;
        Page<TagInfo> page;
        do {
            page = tagInfoService.findAll(
                    PageRequest.of(pageNumber, TAG_INFO_PAGE_SIZE, Sort.by("tagName")));
            all.addAll(page.getContent());
            pageNumber++;
        } while (page.hasNext());
        return all;
    }
}
