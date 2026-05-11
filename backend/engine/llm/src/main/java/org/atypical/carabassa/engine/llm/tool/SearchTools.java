package org.atypical.carabassa.engine.llm.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.search.SearchCriteriaParser;
import org.atypical.carabassa.core.service.DatasetService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tools exposed to the LLM. One instance per ask request: it carries the dataset
 * scope and remembers the last successful search so the controller can return
 * those exact items to the user.
 */
public class SearchTools {

    private static final int SAMPLE_SIZE = 10;
    private static final int RESULT_PAGE_SIZE = 24;
    private static final int TAG_VALUES_PAGE_SIZE = 50;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final Dataset dataset;
    private final DatasetService datasetService;
    private final ZoneId zoneId;

    private String lastQuery;
    private Page<IndexedItem> lastPage;

    public SearchTools(Dataset dataset, DatasetService datasetService) {
        this(dataset, datasetService, ZoneId.systemDefault());
    }

    public SearchTools(Dataset dataset, DatasetService datasetService, ZoneId zoneId) {
        this.dataset = dataset;
        this.datasetService = datasetService;
        this.zoneId = zoneId == null ? ZoneId.systemDefault() : zoneId;
    }

    public String getLastQuery() {
        return lastQuery;
    }

    public Page<IndexedItem> getLastPage() {
        return lastPage;
    }

    @Tool(name = "current_date", description = """
            Return the current date and time in the server's configured timezone.
            Call this whenever the user uses relative time references (e.g.
            'last summer', 'this week', 'yesterday') so you can compute the
            correct absolute dates for the search query.""")
    public String currentDate() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        LocalDate today = now.toLocalDate();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("date", today.toString());
        payload.put("weekday", today.getDayOfWeek().toString());
        payload.put("timezone", zoneId.toString());
        payload.put("datetime", now.toString());
        return toJson(payload);
    }

    @Tool(name = "search_items", description = """
            Run a search using Carabassa's structured query syntax (e.g.
            'person:Maria from:2026). Returns
            the total count and a sample of up to 10 matching items so you can
            decide whether to refine. The exact query you pass will also be the
            one whose results are returned to the user, so call this last with
            your final query.""")
    public String searchItems(@ToolParam(description = "Carabassa structured query string") String query) {
        this.lastQuery = null;
        this.lastPage = null;

        SearchCriteria criteria;
        try {
            criteria = SearchCriteriaParser.parse(query);
        } catch (IllegalArgumentException e) {
            return errorJson("invalid query: " + e.getMessage());
        }

        Pageable pageable = PageRequest.of(0, RESULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "archiveTime"));
        Page<IndexedItem> page = (criteria == null || criteria.isEmpty())
                ? datasetService.findItems(dataset, pageable)
                : datasetService.findItems(dataset, criteria, pageable);

        this.lastQuery = query;
        this.lastPage = page;

        return summarize(query, page);
    }

    @Tool(name = "list_tag_values", description = """
            List distinct values present in the dataset for the given tag. Use
            this to disambiguate user-mentioned values (e.g. names of people,
            place names) before composing your final query. Optionally filter by
            a case-insensitive prefix.""")
    public String listTagValues(
            @ToolParam(description = "Tag name as listed in the system prompt") String tagName,
            @ToolParam(required = false, description = "Optional case-insensitive prefix filter") String prefix) {
        String filter = prefix == null ? null : prefix.toLowerCase();
        List<String> values = new ArrayList<>();
        long totalElements = 0;
        int pageIndex = 0;
        boolean hasNext = true;

        while (hasNext && values.size() < TAG_VALUES_PAGE_SIZE) {
            Pageable pageable = PageRequest.of(pageIndex, TAG_VALUES_PAGE_SIZE, Sort.by("textValue"));
            Page<String> page;
            try {
                page = datasetService.findDistinctValuesByTagName(dataset, tagName, pageable);
            } catch (EntityNotFoundException e) {
                return errorJson("unknown tag: " + tagName);
            }
            totalElements = page.getTotalElements();
            for (String v : page.getContent()) {
                if (v == null) continue;
                if (filter == null || v.toLowerCase().startsWith(filter)) {
                    values.add(v);
                    if (values.size() >= TAG_VALUES_PAGE_SIZE) {
                        break;
                    }
                }
            }
            hasNext = page.hasNext();
            pageIndex++;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tag", tagName);
        payload.put("total", totalElements);
        payload.put("values", values);
        return toJson(payload);
    }

    private String summarize(String query, Page<IndexedItem> page) {
        int n = Math.min(SAMPLE_SIZE, page.getContent().size());
        List<Map<String, Object>> sample = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            IndexedItem item = page.getContent().get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", item.getId());
            entry.put("type", item.getType() == null ? null : item.getType().toString());
            if (item.getArchiveTime() != null) {
                entry.put("archive_time", item.getArchiveTime().toString());
            }
            entry.put("tags", groupTags(item.getTags()));
            sample.add(entry);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("total", page.getTotalElements());
        payload.put("sample", sample);
        return toJson(payload);
    }

    private Map<String, List<String>> groupTags(java.util.Set<Tag> tags) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        if (tags != null) {
            for (Tag t : tags) {
                Object v = t.getValue();
                if (v == null) continue;
                grouped.computeIfAbsent(t.getName(), k -> new ArrayList<>()).add(String.valueOf(v));
            }
        }
        return grouped;
    }

    private static String toJson(Object payload) {
        try {
            return JSON_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return errorJson("serialization failed");
        }
    }

    private static String errorJson(String message) {
        try {
            return JSON_MAPPER.writeValueAsString(Map.of("error", message));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"unknown\"}";
        }
    }
}
