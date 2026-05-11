package org.atypical.carabassa.restapi.representation.model;

import java.util.ArrayList;
import java.util.List;

public class AskResponseRepresentation {

    private String summary;
    private String search;
    private long totalItems;
    private List<ItemRepresentation> items;

    public AskResponseRepresentation() {
        super();
    }

    public AskResponseRepresentation(String summary, String search, long totalItems, List<ItemRepresentation> items) {
        this.summary = summary;
        this.search = search;
        this.totalItems = totalItems;
        this.items = items == null ? null : new ArrayList<>(items);
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public List<ItemRepresentation> getItems() {
        return items == null ? null : new ArrayList<>(items);
    }

    public void setItems(List<ItemRepresentation> items) {
        this.items = items == null ? null : new ArrayList<>(items);
    }
}
