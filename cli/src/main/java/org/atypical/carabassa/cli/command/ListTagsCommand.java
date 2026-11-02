package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "list-tags", description = "list item tags.")
public class ListTagsCommand extends AbstractItemTagCommand implements Callable<Integer> {

    private static final String OUTPUT_FORMAT = "%8s\t%8s\t%-30s\t%s\n";

    private static final CommandLogger cmdLogger = new CommandLogger();

    @Option(names = {"-n", "--name"}, description = "show only tags with this name.")
    private String tagName;

    @Override
    public Integer call() {
        if (!hasSelection()) {
            cmdLogger.error("Items must be selected with --item or --search.");
            return ExitCode.USAGE;
        }
        try {
            Long datasetId = datasetApiService.findByName(dataset);
            if (datasetId == null) {
                cmdLogger.info("No dataset found.");
                return ExitCode.OK;
            }
            List<ItemRepresentation> items = selectItems(datasetId);
            if (items.isEmpty()) {
                cmdLogger.info("No items found.");
                return ExitCode.OK;
            }
            int count = 0;
            System.out.format(OUTPUT_FORMAT, "item", "tag id", "name", "value");
            for (ItemRepresentation item : items) {
                if (item.getTags() == null) {
                    continue;
                }
                for (TagEntityRepresentation tag : item.getTags()) {
                    if (tagName == null || tagName.equals(tag.getName())) {
                        System.out.format(OUTPUT_FORMAT, item.getId(), tag.getId(), tag.getName(),
                                valueAsString(tag.getValue()));
                        count++;
                    }
                }
            }
            cmdLogger.info(String.format("%d tags in %d items.", count, items.size()));
        } catch (ApiException e) {
            cmdLogger.error("API error", e);
            return ExitCode.SOFTWARE;
        }
        return ExitCode.OK;
    }
}
