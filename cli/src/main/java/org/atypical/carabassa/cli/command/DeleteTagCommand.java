package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.cli.util.InteractiveCommand;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "delete-tag", description = "delete tags from items.")
public class DeleteTagCommand extends AbstractItemTagCommand implements Callable<Integer> {

    private static final CommandLogger cmdLogger = new CommandLogger();

    @Option(names = {"-n", "--name"}, description = "tag name.", required = true)
    private String tagName;

    @Option(names = {"-v", "--value"}, description = "delete only tags with this value.")
    private String tagValue;

    @Option(names = {"-f", "--force"}, description = "no ask before deleting.", defaultValue = "false")
    private Boolean force;

    private record ItemTag(Long itemId, TagEntityRepresentation tag) {
    }

    @Override
    public Integer call() {
        try {
            Long datasetId = datasetApiService.findByName(dataset);
            if (datasetId == null) {
                cmdLogger.info("No dataset found.");
                return ExitCode.OK;
            }
            List<ItemTag> toDelete = new ArrayList<>();
            for (ItemRepresentation item : selectItems(datasetId)) {
                if (item.getTags() == null) {
                    continue;
                }
                for (TagEntityRepresentation tag : item.getTags()) {
                    if (tagName.equals(tag.getName())
                            && (tagValue == null || tagValue.equals(valueAsString(tag.getValue())))) {
                        toDelete.add(new ItemTag(item.getId(), tag));
                    }
                }
            }
            if (toDelete.isEmpty()) {
                cmdLogger.info("No tags found.");
                return ExitCode.OK;
            }
            if (!force && !InteractiveCommand.doConfirm(String.format(
                    "This action cannot be reversed. You are going to delete %d '%s' tags in dataset '%s'. Are you sure? [y|N] ",
                    toDelete.size(), tagName, dataset))) {
                return ExitCode.OK;
            }
            int deleted = 0;
            int error = 0;
            for (ItemTag itemTag : toDelete) {
                try {
                    datasetApiService.deleteItemTag(datasetId, itemTag.itemId(), itemTag.tag().getId());
                    cmdLogger.info(String.format("Deleted tag %d (%s=%s) from item %d", itemTag.tag().getId(),
                            itemTag.tag().getName(), valueAsString(itemTag.tag().getValue()), itemTag.itemId()));
                    deleted++;
                } catch (ApiException e) {
                    cmdLogger.error(String.format("Error deleting tag %d from item %d", itemTag.tag().getId(),
                            itemTag.itemId()), e);
                    error++;
                }
            }
            cmdLogger.info(String.format("Deleted %d of %d tags (%d error)", deleted, toDelete.size(), error));
            return error == 0 ? ExitCode.OK : ExitCode.SOFTWARE;
        } catch (ApiException e) {
            cmdLogger.error("API error", e);
            return ExitCode.SOFTWARE;
        }
    }
}
