package org.atypical.carabassa.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ImageAlreadyExists;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "upload", description = "upload images to dataset.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class UploadDatasetCommand implements Callable<Integer> {

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-p", "--path" }, description = "base path to upload images.", required = true)
	private String basePath;

	private CommandLogger cmdLogger = new CommandLogger();

	@Autowired
	private DatasetApiService datasetApiService;

	@Override
	public Integer call() throws Exception {
		Long datasetId = null;
		try {
			datasetId = datasetApiService.findByName(dataset);
		} catch (ApiException e) {
			cmdLogger.error(String.format("Error getting dataset id for %s: %s", dataset, e.getMessage()));
		}

		List<Path> imagesPath = Files.walk(Paths.get(basePath)).filter(path -> isImage(path))
				.collect(Collectors.toList());

		int count = 0, uploaded = 0, existing = 0, error = 0;
		int total = imagesPath.size();

		long imageId;
		for (Path imagePath : imagesPath) {
			cmdLogger.info(String.format("Uploading image %s ( %d / %d ) ...", imagePath, ++count, total));
			try {
				imageId = datasetApiService.addImage(datasetId, imagePath);
				uploaded++;
				cmdLogger.info(String.format("Uploaded image %s with id %d.", imagePath, imageId));
			} catch (ImageAlreadyExists e) {
				cmdLogger.warn(String.format("Warning uploading image %s", imagePath), e);
				existing++;
			} catch (ApiException | IOException e) {
				cmdLogger.error(String.format("Error uploading image %s", imagePath), e);
				error++;
			}
		}

		cmdLogger.info(String.format(
				"Uploaded %d of %d images (%d already existing, %d error) from path=\"%s\" to dataset=\"%s\"", uploaded,
				total, existing, error, basePath, dataset));
		return ExitCode.OK;
	}

	private boolean isImage(Path path) {
		if (Files.isRegularFile(path)) {
			String mimeType;
			try {
				mimeType = Files.probeContentType(path);
			} catch (IOException e) {
				cmdLogger.warn(String.format("Error reading file %s, ignoring", path));
				return false;
			}
			if (mimeType != null && mimeType.startsWith("image/")) {
				cmdLogger.debug(String.format("Found image %s", path));
				return true;
			} else {
				cmdLogger.debug(String.format("File %s is not an image, ignoring", path));
				return false;
			}
		} else {
			return false;
		}
	}

}
