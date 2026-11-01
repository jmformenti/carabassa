package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.cli.util.InteractiveCommand;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "add-tag", description = "add a tag to items.")
public class AddTagCommand extends AbstractItemTagCommand implements Callable<Integer> {

    private static final CommandLogger cmdLogger = new CommandLogger();

    @Option(names = {"-n", "--name"}, description = "tag name.", required = true)
    private String tagName;

    @Option(names = {"-v", "--value"}, description = "tag value.", required = true)
    private String tagValue;

    @Option(names = {"-t", "--type"}, description = "tag value type: ${COMPLETION-CANDIDATES} (default: server decides).")
    private TagValueType tagType;

    @Option(names = {"-f", "--force"}, description = "no ask before tagging several items.", defaultValue = "false")
    private Boolean force;

    enum TagValueType {STRING, LONG, DOUBLE, DATE, BOOLEAN}

    @Override
    public Integer call() {
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
            if (items.size() > 1 && !force && !InteractiveCommand.doConfirm(String.format(
                    "You are going to add tag '%s=%s' to %d items in dataset '%s'. Are you sure? [y|N] ",
                    tagName, tagValue, items.size(), dataset))) {
                return ExitCode.OK;
            }
            int added = 0;
            int error = 0;
            for (ItemRepresentation item : items) {
                try {
                    Long tagId = datasetApiService.addItemTag(datasetId, item.getId(), tagName, tagValue,
                            tagType == null ? null : tagType.name());
                    cmdLogger.info(String.format("Added tag %d to item %d", tagId, item.getId()));
                    added++;
                } catch (ApiException e) {
                    cmdLogger.error(String.format("Error adding tag to item %d", item.getId()), e);
                    error++;
                }
            }
            cmdLogger.info(String.format("Tagged %d of %d items (%d error)", added, items.size(), error));
            return error == 0 ? ExitCode.OK : ExitCode.SOFTWARE;
        } catch (ApiException e) {
            cmdLogger.error("API error", e);
            return ExitCode.SOFTWARE;
        }
    }
}
