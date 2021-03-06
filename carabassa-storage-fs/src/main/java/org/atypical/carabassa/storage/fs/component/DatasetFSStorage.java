package org.atypical.carabassa.storage.fs.component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.atypical.carabassa.core.component.storage.DatasetStorage;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemInfo;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.impl.StoredItemImpl;
import org.atypical.carabassa.core.model.impl.StoredItemInfoImpl;
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
	private static final String ITEM_NOT_EXISTS_MESSAGE_KEY = "core.storage.repo.item.not_exists";
	private static final String ITEM_EXISTS_MESSAGE_KEY = "core.storage.repo.item.exists";

	private static final String ARCHIVED_DIR = "archived";
	private static final String NOT_ARCHIVED_DIR = "not_archived";

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
	public void create(Dataset dataset) throws IOException, EntityExistsException {
		String datasetName = dataset.getName();
		Path datasetPath = getDatasetPath(datasetName);
		if (!Files.exists(datasetPath)) {
			Files.createDirectories(datasetPath);
		} else {
			throw new EntityExistsException(localizedMessage.getText(DATASET_EXISTS_MESSAGE_KEY, datasetName));
		}
	}

	@Override
	public void addItem(Dataset dataset, IndexedItem item, Resource inputItem)
			throws IOException, EntityExistsException {
		Path repoPath = getArchivePath(dataset, item);
		Files.createDirectories(repoPath);

		Path itemDirPath = repoPath.resolve(getArchiveFilename(item));
		if (!Files.exists(itemDirPath)) {
			Files.move(Paths.get(inputItem.getFile().getPath()), itemDirPath, StandardCopyOption.REPLACE_EXISTING);
			writeJson(repoPath, item);
		} else {
			throw new EntityExistsException(localizedMessage.getText(ITEM_EXISTS_MESSAGE_KEY, item.getId()));
		}
	}

	@Override
	public StoredItem getItem(Dataset dataset, IndexedItem item) throws IOException, EntityNotFoundException {
		Path repoPath = getArchivePath(dataset, item);

		StoredItem storedItem = new StoredItemImpl();
		try {
			storedItem.setContent(Files.readAllBytes(repoPath.resolve(getArchiveFilename(item))));
		} catch (NoSuchFileException e) {
			throw new EntityNotFoundException(localizedMessage.getText(ITEM_NOT_EXISTS_MESSAGE_KEY, item.getId()));
		}
		try {
			storedItem.setStoredItemInfo(readJson(repoPath, item));
		} catch (FileNotFoundException e) {
			storedItem.setStoredItemInfo(null);
		}

		return storedItem;
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
	public void deleteItem(Dataset dataset, IndexedItem item) throws IOException {
		Path repoPath = getArchivePath(dataset, item);
		Files.delete(repoPath.resolve(getArchiveFilename(item)));
		deleteJson(repoPath, item);
	}

	private StoredItemInfo readJson(Path repoDirPath, IndexedItem item)
			throws JsonParseException, JsonMappingException, IOException {
		String filename = getArchiveBasename(item) + ".json";
		return mapper.readValue(repoDirPath.resolve(filename).toFile(), StoredItemInfoImpl.class);
	}

	private void writeJson(Path repoDirPath, IndexedItem item)
			throws JsonGenerationException, JsonMappingException, IOException {
		String filename = getArchiveBasename(item) + ".json";
		mapper.writeValue(repoDirPath.resolve(filename).toFile(), new StoredItemInfoImpl(item.getFilename()));
	}

	private void deleteJson(Path repoDirPath, IndexedItem item) throws IOException {
		String filename = getArchiveBasename(item) + ".json";
		Files.deleteIfExists(repoDirPath.resolve(filename));
	}

	private String getArchiveFilename(IndexedItem item) {
		String extension = getArchiveExtension(item);
		return getArchiveBasename(item) + (StringUtils.isBlank(extension) ? "" : "." + extension.toLowerCase());
	}

	private String getArchiveBasename(IndexedItem item) {
		return item.getHash();
	}

	private String getArchiveExtension(IndexedItem item) {
		String extension = item.getFormat();
		if (extension == null) {
			extension = FilenameUtils.getExtension(item.getFilename());
		}
		return extension;
	}

	private Path getArchivePath(Dataset dataset, IndexedItem item) {
		if (item.isArchived()) {
			return getArchivedPath(dataset, item);
		} else {
			return getNotArchivedPath(dataset, item);
		}
	}

	private Path getNotArchivedPath(Dataset dataset, IndexedItem item) {
		return Paths.get(getDatasetPath(dataset).toString(), getTypeDir(item.getType()), NOT_ARCHIVED_DIR);
	}

	private Path getArchivedPath(Dataset dataset, IndexedItem item) {
		ZonedDateTime archiveTime = item.getArchiveTime();
		return Paths.get(getDatasetPath(dataset).toString(), getTypeDir(item.getType()), ARCHIVED_DIR,
				String.valueOf(archiveTime.getYear()), String.format("%02d", archiveTime.getMonth().getValue()),
				String.format("%02d", archiveTime.getDayOfMonth()));
	}

	private Path getDatasetPath(Dataset dataset) {
		return getDatasetPath(dataset.getName());
	}

	private Path getDatasetPath(String datasetName) {
		return Paths.get(repoDir, datasetName);
	}

	private String getTypeDir(ItemType type) {
		return type.normalized();
	}

	@Override
	public void update(Dataset dataset) throws IOException {
		if (!Files.exists(getDatasetPath(dataset))) {
			throw new IOException(localizedMessage.getText(DATASET_NOT_EXISTS_MESSAGE_KEY, dataset.getName()));
		}
	}

}
