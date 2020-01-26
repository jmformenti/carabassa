package org.atypical.core.component.storage.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.atypical.core.component.storage.DatasetStorage;
import org.atypical.core.component.util.LocalizedMessage;
import org.atypical.core.exception.EntityExistsException;
import org.atypical.core.exception.EntityNotFoundException;
import org.atypical.core.model.Dataset;
import org.atypical.core.model.IndexedImage;
import org.atypical.core.model.StoredImage;
import org.atypical.core.model.StoredImageInfo;
import org.atypical.core.model.impl.StoredImageImpl;
import org.atypical.core.model.impl.StoredImageInfoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DatasetFSStorage implements DatasetStorage {

	private static final String NO_REPO_DIR_MESSAGE_KEY = "core.storage.repo.no_dir";
	private static final String REPO_NOT_EXISTS_MESSAGE_KEY = "core.storage.repo.not_exists";
	private static final String DATASET_NOT_EXISTS_MESSAGE_KEY = "core.storage.repo.dataset.not_exists";
	private static final String DATASET_EXISTS_MESSAGE_KEY = "core.storage.repo.dataset.exists";
	private static final String IMAGE_NOT_EXISTS_MESSAGE_KEY = "core.storage.repo.image.not_exists";
	private static final String IMAGE_EXISTS_MESSAGE_KEY = "core.storage.repo.image.exists";

	private static final String ARCHIVED_DIR = "image/archived";
	private static final String NOT_ARCHIVED_DIR = "image/not_archived";

	@Value("${carabassa.repodir}")
	private String repoDir;

	@Autowired
	private LocalizedMessage localizedMessage;

	private ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	private void postConstruct() throws FileNotFoundException {
		Assert.notNull(repoDir, localizedMessage.getText(NO_REPO_DIR_MESSAGE_KEY));
		if (!Files.exists(Paths.get(repoDir))) {
			throw new FileNotFoundException(localizedMessage.getText(REPO_NOT_EXISTS_MESSAGE_KEY));
		}
	}

	@Override
	public void create(String datasetName) throws IOException, EntityExistsException {
		Path datasetPath = getDatasetPath(datasetName);
		if (!Files.exists(datasetPath)) {
			Files.createDirectories(datasetPath);
		} else {
			throw new EntityExistsException(localizedMessage.getText(DATASET_EXISTS_MESSAGE_KEY, datasetName));
		}
	}

	@Override
	public void addImage(Dataset dataset, IndexedImage image, Resource inputImage)
			throws IOException, EntityExistsException {
		Path repoPath = getArchivePath(dataset, image);
		Files.createDirectories(repoPath);

		Path imageDirPath = repoPath.resolve(getArchiveFilename(image));
		if (!Files.exists(imageDirPath)) {
			Files.copy(inputImage.getInputStream(), imageDirPath);
			writeJson(repoPath, image);
		} else {
			throw new EntityExistsException(localizedMessage.getText(IMAGE_EXISTS_MESSAGE_KEY, image.getId()));
		}
	}

	@Override
	public StoredImage getImage(Dataset dataset, IndexedImage image) throws IOException, EntityNotFoundException {
		Path repoPath = getArchivePath(dataset, image);

		StoredImage storedImage = new StoredImageImpl();
		try {
			storedImage.setContent(Files.readAllBytes(repoPath.resolve(getArchiveFilename(image))));
		} catch (NoSuchFileException e) {
			throw new EntityNotFoundException(localizedMessage.getText(IMAGE_NOT_EXISTS_MESSAGE_KEY, image.getId()));
		}
		try {
			storedImage.setStoredImageInfo(readJson(repoPath, image));
		} catch (FileNotFoundException e) {
			storedImage.setStoredImageInfo(null);
		}

		return storedImage;
	}

	@Override
	public void deleteAll() throws IOException {
		FileUtils.deleteDirectory(Paths.get(repoDir).toFile());
	}

	@Override
	public void delete(Dataset dataset) throws IOException {
		FileUtils.deleteDirectory(getDatasetPath(dataset).toFile());
	}

	@Override
	public void deleteImage(Dataset dataset, IndexedImage image) throws IOException {
		Path repoPath = getArchivePath(dataset, image);
		Files.delete(repoPath.resolve(getArchiveFilename(image)));
		deleteJson(repoPath, image);
	}

	private StoredImageInfo readJson(Path repoDirPath, IndexedImage image)
			throws JsonParseException, JsonMappingException, IOException {
		String filename = getArchiveBasename(image) + ".json";
		return mapper.readValue(repoDirPath.resolve(filename).toFile(), StoredImageInfoImpl.class);
	}

	private void writeJson(Path repoDirPath, IndexedImage image)
			throws JsonGenerationException, JsonMappingException, IOException {
		String filename = getArchiveBasename(image) + ".json";
		mapper.writeValue(repoDirPath.resolve(filename).toFile(), new StoredImageInfoImpl(image.getFilename()));
	}

	private void deleteJson(Path repoDirPath, IndexedImage image) throws IOException {
		String filename = getArchiveBasename(image) + ".json";
		Files.deleteIfExists(repoDirPath.resolve(filename));
	}

	private String getArchiveFilename(IndexedImage image) {
		String extension = getArchiveExtension(image);
		return getArchiveBasename(image) + (StringUtils.isBlank(extension) ? "" : "." + extension.toLowerCase());
	}

	private String getArchiveBasename(IndexedImage image) {
		return image.getHash();
	}

	private String getArchiveExtension(IndexedImage image) {
		String extension = image.getFileType();
		if (extension == null) {
			extension = FilenameUtils.getExtension(image.getFilename());
		}
		return extension;
	}

	private Path getArchivePath(Dataset dataset, IndexedImage image) {
		if (image.isArchived()) {
			return getArchivePath(dataset, image.getArchiveTime());
		} else {
			return getNotArchivedPath(dataset);
		}
	}

	private Path getNotArchivedPath(Dataset dataset) {
		return Paths.get(getDatasetPath(dataset).toString(), NOT_ARCHIVED_DIR);
	}

	private Path getArchivePath(Dataset dataset, ZonedDateTime archiveTime) {
		return Paths.get(getDatasetPath(dataset).toString(), ARCHIVED_DIR, String.valueOf(archiveTime.getYear()),
				String.format("%02d", archiveTime.getMonth().getValue()),
				String.format("%02d", archiveTime.getDayOfMonth()));
	}

	private Path getDatasetPath(Dataset dataset) {
		return getDatasetPath(dataset.getName());
	}

	private Path getDatasetPath(String datasetName) {
		return Paths.get(repoDir, datasetName);
	}

	@Override
	public void update(Dataset dataset) throws IOException {
		if (!Files.exists(getDatasetPath(dataset))) {
			throw new IOException(localizedMessage.getText(DATASET_NOT_EXISTS_MESSAGE_KEY, dataset.getName()));
		}
	}

}
