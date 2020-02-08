package org.atypical.carabassa.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

@Component
@Command(name = "upload", description = "upload images to dataset.", mixinStandardHelpOptions = true, exitCodeOnExecutionException = 1)
public class UploadDatasetCommand implements Callable<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(UploadDatasetCommand.class);

	@Option(names = { "-d", "--dataset" }, description = "dataset name.", required = true)
	private String dataset;

	@Option(names = { "-p", "--path" }, description = "base path to upload images.", required = true)
	private String basePath;

	private CommandLogger cmdLogger = new CommandLogger(logger);

	@Autowired
	private DatasetApiService datasetApiService;

	@Override
	public Integer call() throws Exception {
		List<Path> imagesPath = Files.walk(Paths.get(basePath)).filter(path -> isImage(path))
				.collect(Collectors.toList());
		int uploaded = 0;
		for (Path imagePath : imagesPath) {
			cmdLogger.info(String.format("Uploading image %s ...", imagePath));
			try {
				datasetApiService.addImage(dataset, imagePath);
				uploaded++;
			} catch (ApiException e) {
				cmdLogger.error("API error", e);
			}
		}
		cmdLogger.info(String.format("uploaded %d of %d images from path=%s to dataset=%s", uploaded, imagesPath.size(),
				basePath, dataset));
		return ExitCode.OK;
	}

	private boolean isImage(Path path) {
		if (Files.isRegularFile(path)) {
			String mimeType;
			try {
				mimeType = Files.probeContentType(path);
			} catch (IOException e) {
				cmdLogger.warn(String.format("Error reading files %s, ignoring", path), e);
				return false;
			}
			if (mimeType != null && mimeType.startsWith("image/")) {
				return true;
			} else {
				cmdLogger.warn(String.format("File %s is not an image, ignoring", path));
				return false;
			}
		} else {
			return false;
		}
	}

}
