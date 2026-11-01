package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * Common options to select the items whose tags are managed: explicit item ids or search conditions.
 */
public abstract class AbstractItemTagCommand {

    @Option(names = {"-d", "--dataset"}, description = "dataset name.", required = true)
    protected String dataset;

    @Option(names = {"-i", "--item"}, split = ",", description = "item id(s), comma separated.")
    protected List<Long> itemIds;

    @Option(names = {"-s", "--search"}, description = "search conditions (ignored if --item is set).")
    protected String searchString;

    @Autowired
    protected DatasetApiService datasetApiService;

    /**
     * Returns the selected items, including their tags.
     */
    protected List<ItemRepresentation> selectItems(Long datasetId) throws ApiException {
        if (itemIds != null && !itemIds.isEmpty()) {
            List<ItemRepresentation> items = new ArrayList<>();
            for (Long itemId : itemIds) {
                items.add(datasetApiService.findItem(datasetId, itemId));
            }
            return items;
        }
        return datasetApiService.findItems(datasetId, searchString, true);
    }

    protected static String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
