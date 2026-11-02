package org.atypical.carabassa.cli.service.impl;

import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetApiServiceImplTest {

    private final DatasetApiServiceImpl service = new DatasetApiServiceImpl();
    private final List<ClientRequest> requests = new ArrayList<>();
    private final Queue<String> responses = new ConcurrentLinkedQueue<>();

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://carabassa.test/")
                .exchangeFunction(request -> {
                    requests.add(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(responses.remove())
                            .build());
                })
                .build();
        ReflectionTestUtils.setField(service, "webClient", webClient);
        ReflectionTestUtils.setField(service, "objectMapper", JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build());
    }

    @Test
    void findAllItemsGoesThroughAllPages() throws Exception {
        responses.add(page(2, 1, 2));
        responses.add(page(2, 3));

        List<ItemRepresentation> items = service.findAllItems(1L, "race=SUP", true);

        assertEquals(List.of(1L, 2L, 3L), items.stream().map(ItemRepresentation::getId).toList());
        assertEquals(2, requests.size());
        String firstQuery = requests.get(0).url().getQuery();
        String secondQuery = requests.get(1).url().getQuery();
        assertTrue(firstQuery.contains("page=0"), firstQuery);
        assertTrue(firstQuery.contains("includeTags=true"), firstQuery);
        assertTrue(firstQuery.contains("search=race=SUP"), firstQuery);
        assertTrue(secondQuery.contains("page=1"), secondQuery);
    }

    @Test
    void findAllItemsWithNoResultsMakesOneRequest() throws Exception {
        responses.add("{\"page\":{\"size\":100,\"totalElements\":0,\"totalPages\":0,\"number\":0}}");

        assertTrue(service.findAllItems(1L, "nothing", true).isEmpty());
        assertEquals(1, requests.size());
    }

    @Test
    void findItemsOnlyReadsTheFirstPage() throws Exception {
        responses.add(page(2, 1, 2));

        assertEquals(2, service.findItems(1L, "race=SUP").size());
        assertEquals(1, requests.size());
    }

    @Test
    void findItemRequestsTheItem() throws Exception {
        responses.add("{\"id\":5}");

        assertEquals(5L, service.findItem(1L, 5L).getId());
        assertEquals("/dataset/1/item/5", requests.get(0).url().getPath());
    }

    @Test
    void addItemTagPostsToTheItem() throws Exception {
        responses.add("{\"id\":10}");

        assertEquals(10L, service.addItemTag(1L, 5L, "race", "SUP", "STRING"));
        assertEquals(HttpMethod.POST, requests.get(0).method());
        assertEquals("/dataset/1/item/5/tag", requests.get(0).url().getPath());
    }

    @Test
    void deleteItemTagDeletesTheTag() throws Exception {
        responses.add("");

        service.deleteItemTag(1L, 5L, 10L);

        assertEquals(HttpMethod.DELETE, requests.get(0).method());
        assertEquals("/dataset/1/item/5/tag/10", requests.get(0).url().getPath());
    }

    private static String page(int totalPages, long... ids) {
        StringBuilder items = new StringBuilder();
        for (long id : ids) {
            items.append(items.isEmpty() ? "" : ",").append("{\"id\":").append(id).append('}');
        }
        return "{\"_embedded\":{\"itemRepresentationList\":[" + items + "]},"
                + "\"page\":{\"size\":100,\"totalElements\":3,\"totalPages\":" + totalPages + ",\"number\":0}}";
    }
}
