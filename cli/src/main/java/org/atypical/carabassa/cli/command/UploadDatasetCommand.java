package org.atypical.carabassa.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import org.atypical.carabassa.cli.dto.ItemToUpload;
import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ItemAlreadyExists;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.util.MediaTypeDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "upload", description = "upload items to dataset.")
public class UploadDatasetCommand implements Callable<Integer> {

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-p", "--path" }, description = "base path to upload items.", required = true)
	private String basePath;

	private final CommandLogger cmdLogger = new CommandLogger();

	@Autowired
	private DatasetApiService datasetApiService;

	@Value("${carabassa.upload.threads}")
	private int numThreads;

	private int count;
	private int uploaded;
	private int existing;
	private int error;
	private int total;

	@Override
	public Integer call() throws Exception {
		this.count = 0;
		this.uploaded = 0;
		this.existing = 0;
		this.error = 0;

		Long datasetId = getDatasetId(dataset);
		if (datasetId != null) {
			List<ItemToUpload> itemsToUpload = getItemsToUpload(basePath);
			this.total = itemsToUpload.size();

			if (total > 0) {
				uploadItems(datasetId, itemsToUpload);

				cmdLogger.info(String.format(
						"Uploaded %d of %d items (%d already existing, %d error) from path=\"%s\" to dataset=\"%s\"",
						uploaded, total, existing, error, basePath, dataset));
			} else {
				cmdLogger.info("No items found.");
			}

			cmdLogger.info("done.");

			return ExitCode.OK;
		} else {
			return ExitCode.SOFTWARE;
		}
	}

	private Long getDatasetId(String dataset) {
		try {
			return datasetApiService.findByName(dataset);
		} catch (ApiException e) {
			cmdLogger.error(String.format("Error getting dataset id for %s: %s", dataset, e.getMessage()));
			return null;
		}
	}

	private List<ItemToUpload> getItemsToUpload(String basePath) throws IOException {
		cmdLogger.info(String.format("Looking for items in %s ...", basePath));

		return Files.walk(Paths.get(basePath)) //
				.filter(Files::isRegularFile) //
				.map(this::toItemToUpload) //
				.filter(this::isSupportedType) //
				.collect(Collectors.toList());
	}

	private ItemToUpload toItemToUpload(Path path) {
		ItemToUpload itemToUpload = new ItemToUpload();
		itemToUpload.setFilename(path.getFileName().toString());
		itemToUpload.setPath(path);
		try {
			itemToUpload.setContentType(MediaTypeDetector.detect(path));
		} catch (IOException e) {
			cmdLogger.warn(String.format("Error reading file %s, ignoring", path));
			itemToUpload.setContentType(null);
		}
		return itemToUpload;
	}

	private void uploadItems(Long datasetId, List<ItemToUpload> itemsToUpload)
			throws InterruptedException, ExecutionException {
		cmdLogger.info(String.format("Uploading %d items ...", total));

		ForkJoinPool customThreadPool = new ForkJoinPool(numThreads);
		customThreadPool
				.submit(() -> itemsToUpload.parallelStream().forEach(itemToUpload -> upload(datasetId, itemToUpload)))
				.get();
	}

	private void upload(Long datasetId, ItemToUpload itemToUpload) {
		long itemId;
		Path itemPath = itemToUpload.getPath();

		cmdLogger.info(String.format("Uploading item %s ( %d / %d ) ...", itemPath, ++count, total));
		try {
			itemId = datasetApiService.addItem(datasetId, itemToUpload);
			uploaded++;
			cmdLogger.info(String.format("Uploaded item %s with id %d.", itemPath, itemId));
		} catch (ItemAlreadyExists e) {
			cmdLogger.warn(String.format("Warning uploading item %s", itemPath), e);
			existing++;
		} catch (ApiException | IOException e) {
			cmdLogger.error(String.format("Error uploading item %s", itemPath), e);
			error++;
		}
	}

	private boolean isSupportedType(ItemToUpload itemToUpload) {
		ItemType type = MediaTypeDetector.convert(itemToUpload.getContentType());
		if (type != null) {
			cmdLogger.debug(String.format("Found %s %s (%s)", type.normalized(), itemToUpload.getPath(),
					itemToUpload.getContentType()));
			return true;
		} else {
			cmdLogger.debug(String.format("File %s is not supported type, ignoring", itemToUpload.getPath()));
			return false;
		}
	}

}
