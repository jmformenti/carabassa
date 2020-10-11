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

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.exception.ImageAlreadyExists;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.cli.util.CommandLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
			List<Path> imagesPath = getImagesToUpload(basePath);
			this.total = imagesPath.size();

			if (total > 0) {
				uploadImages(datasetId, imagesPath);

				cmdLogger.info(String.format(
						"Uploaded %d of %d images (%d already existing, %d error) from path=\"%s\" to dataset=\"%s\"",
						uploaded, total, existing, error, basePath, dataset));
			} else {
				cmdLogger.info("No images found.");
			}

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

	private List<Path> getImagesToUpload(String basePath) throws IOException {
		cmdLogger.info(String.format("Looking for images in %s ...", basePath));

		return Files.walk(Paths.get(basePath)).filter(path -> isImage(path)).collect(Collectors.toList());
	}

	private void uploadImages(Long datasetId, List<Path> imagesPath) throws InterruptedException, ExecutionException {
		cmdLogger.info(String.format("Uploading %d images ...", total));

		ForkJoinPool customThreadPool = new ForkJoinPool(numThreads);
		customThreadPool.submit(() -> imagesPath.parallelStream().forEach(path -> upload(datasetId, path))).get();
	}

	private void upload(Long datasetId, Path imagePath) {
		long imageId = 0;

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
