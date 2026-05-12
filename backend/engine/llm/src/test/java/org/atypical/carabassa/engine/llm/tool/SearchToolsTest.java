package org.atypical.carabassa.engine.llm.tool;

import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.service.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchToolsTest {

    private Dataset dataset;
    private DatasetService datasetService;
    private SearchTools tools;

    @BeforeEach
    void setUp() {
        dataset = mock(Dataset.class);
        datasetService = mock(DatasetService.class);
        tools = new SearchTools(dataset, datasetService);
    }

    @Test
    void searchItemsExecutesQueryAndRecordsLastResult() {
        IndexedItem item = mock(IndexedItem.class);
        when(item.getId()).thenReturn(42L);
        when(item.getType()).thenReturn(ItemType.IMAGE);
        when(item.getTags()).thenReturn(java.util.Collections.emptySet());
        Page<IndexedItem> page = new PageImpl<>(List.of(item));

        when(datasetService.findItems(eq(dataset), any(SearchCriteria.class), any(Pageable.class)))
                .thenReturn(page);

        String result = tools.searchItems("person:Maria");

        assertTrue(result.contains("\"total\":1"), result);
        assertTrue(result.contains("\"id\":42"), result);
        assertEquals("person:Maria", tools.getLastQuery());
        assertEquals(page, tools.getLastPage());
    }

    @Test
    void searchItemsWithEmptyQueryUsesUnfilteredFinder() {
        Page<IndexedItem> page = new PageImpl<>(List.of());
        when(datasetService.findItems(eq(dataset), any(Pageable.class))).thenReturn(page);

        String result = tools.searchItems("");

        assertTrue(result.contains("\"total\":0"), result);
        verify(datasetService, times(1)).findItems(eq(dataset), any(Pageable.class));
    }

    @Test
    void searchItemsReturnsErrorOnInvalidQuery() {
        String result = tools.searchItems(":bad:");

        assertTrue(result.contains("\"error\""), result);
        assertNull(tools.getLastQuery());
        assertNull(tools.getLastPage());
    }

    @Test
    void listTagValuesReturnsValuesAndFiltersByPrefix() throws Exception {
        Page<String> values = new PageImpl<>(List.of("Maria", "Marc", "Pere"));
        when(datasetService.findDistinctValuesByTagName(eq(dataset), eq("person"), any(Pageable.class)))
                .thenReturn(values);

        String result = tools.listTagValues("person", "Mar");

        assertTrue(result.contains("\"Maria\""), result);
        assertTrue(result.contains("\"Marc\""), result);
        assertTrue(!result.contains("\"Pere\""), result);
    }

    @Test
    void listTagValuesReturnsErrorWhenTagUnknown() throws Exception {
        when(datasetService.findDistinctValuesByTagName(eq(dataset), eq("ghost"), any(Pageable.class)))
                .thenThrow(new EntityNotFoundException("not found"));

        String result = tools.listTagValues("ghost", null);

        assertTrue(result.contains("\"error\""), result);
    }

    @Test
    void currentDateReturnsTodayInConfiguredTimeZone() {
        ZoneId zone = ZoneId.of("Europe/Andorra");
        SearchTools zonedTools = new SearchTools(dataset, datasetService, zone);

        String result = zonedTools.currentDate();

        LocalDate expected = ZonedDateTime.now(zone).toLocalDate();
        assertTrue(result.contains("\"date\":\"" + expected + "\""), result);
        assertTrue(result.contains("\"timezone\":\"Europe/Andorra\""), result);
        assertTrue(result.contains("\"weekday\":\""), result);
    }

    @Test
    void searchItemsCriteriaCapturesParsedQuery() {
        ArgumentCaptor<SearchCriteria> captor = ArgumentCaptor.forClass(SearchCriteria.class);
        when(datasetService.findItems(eq(dataset), captor.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        tools.searchItems("location:Barcelona");

        SearchCriteria criteria = captor.getValue();
        assertNotNull(criteria);
        assertTrue(!criteria.isEmpty());
    }
}
