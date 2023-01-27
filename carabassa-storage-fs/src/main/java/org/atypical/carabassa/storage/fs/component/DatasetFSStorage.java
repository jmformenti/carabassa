package org.atypical.carabassa.storage.fs.component;

import java.io.ByteArrayOutputStream;
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
import org.atypical.carabassa.core.model.StoredItemThumbnail;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.model.impl.StoredItemImpl;
import org.atypical.carabassa.core.model.impl.StoredItemInfoImpl;
import org.atypical.carabassa.core.model.impl.StoredItemThumbnailImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.coobird.thumbnailator.Thumbnails;

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

	private static final String THUMBNAIL_SUFFIX = "_thumb";
	private static final String JSON_EXT = ".json";

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
	public void addItem(IndexedItem item, Resource inputItem) throws IOException, EntityExistsException {
		Path repoPath = getArchivePath(item);
		Files.createDirectories(repoPath);

		Path itemDirPath = repoPath.resolve(getArchiveFilename(item));
		if (!Files.exists(itemDirPath)) {
			Files.move(Paths.get(inputItem.getFile().getPath()), itemDirPath, StandardCopyOption.REPLACE_EXISTING);
			writeJson(item);
			writeThumbnail(item);
		} else {
			throw new EntityExistsException(localizedMessage.getText(ITEM_EXISTS_MESSAGE_KEY, item.getId()));
		}
	}

	@Override
	public StoredItem getItem(IndexedItem item) throws IOException, EntityNotFoundException {
		StoredItem storedItem = new StoredItemImpl();
		try {
			storedItem.setContent(Files.readAllBytes(getItemPath(item)));
		} catch (NoSuchFileException e) {
			throw new EntityNotFoundException(localizedMessage.getText(ITEM_NOT_EXISTS_MESSAGE_KEY, item.getId()));
		}
		try {
			storedItem.setStoredItemInfo(readJson(item));
		} catch (FileNotFoundException e) {
			storedItem.setStoredItemInfo(null);
		}

		return storedItem;
	}

	@Override
	public StoredItemThumbnail getItemThumbnail(IndexedItem item) throws IOException, EntityNotFoundException {
		if (item.getType() == ItemType.IMAGE) {
			Path itemPath = getItemPath(item);

			if (Files.exists(itemPath)) {
				Path thumbnailPath = getThumbnailPath(item);

				byte[] contents;
				if (Files.exists(thumbnailPath)) {
					contents = Files.readAllBytes(thumbnailPath);
				} else {
					contents = writeThumbnail(item);
				}

				return new StoredItemThumbnailImpl(getThumbnailFilename(item), contents);
			} else {
				throw new EntityNotFoundException(localizedMessage.getText(ITEM_NOT_EXISTS_MESSAGE_KEY, item.getId()));
			}
		} else {
			throw new IllegalArgumentException("Thumbnail implemented only for images");
		}
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
	public void deleteItem(IndexedItem item) throws IOException {
		Files.deleteIfExists(getItemPath(item));
		deleteJson(item);
		deleteThumbnail(item);
	}

	@Override
	public void resetItem(IndexedItem updatedItem, IndexedItem previousItem) throws IOException, EntityExistsException {
		Path previousItemPath = getItemPath(previousItem);
		Path updatedItemPath = getItemPath(updatedItem);

		if (!updatedItemPath.equals(previousItemPath)) {
			Resource itemResource = new FileSystemResource(previousItemPath);

			addItem(updatedItem, itemResource);
			deleteItem(previousItem);
		}
	}

	@Override
	public void update(Dataset dataset) throws IOException {
		if (!Files.exists(getDatasetPath(dataset))) {
			throw new IOException(localizedMessage.getText(DATASET_NOT_EXISTS_MESSAGE_KEY, dataset.getName()));
		}
	}

	private byte[] writeThumbnail(IndexedItem item) throws IOException {
		if (item.getType() == ItemType.IMAGE) {
			byte[] contents = createThumbnail(getItemPath(item));
			Files.write(getThumbnailPath(item), contents);

			return contents;
		} else {
			return null;
		}
	}

	private byte[] createThumbnail(Path itemPath) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Thumbnails.of(itemPath.toFile()).size(200, 200).keepAspectRatio(true).toOutputStream(baos);
		return baos.toByteArray();
	}

	private StoredItemInfo readJson(IndexedItem item) throws JsonParseException, JsonMappingException, IOException {
		return mapper.readValue(getJsonPath(item).toFile(), StoredItemInfoImpl.class);
	}

	private void writeJson(IndexedItem item) throws JsonGenerationException, JsonMappingException, IOException {
		mapper.writeValue(getJsonPath(item).toFile(), new StoredItemInfoImpl(item.getFilename()));
	}

	private void deleteJson(IndexedItem item) throws IOException {
		Files.deleteIfExists(getJsonPath(item));
	}

	private void deleteThumbnail(IndexedItem item) throws IOException {
		Files.deleteIfExists(getThumbnailPath(item));
	}

	private Path getItemPath(IndexedItem item) {
		return getArchivePath(item).resolve(getArchiveFilename(item));
	}

	private Path getJsonPath(IndexedItem item) {
		return getArchivePath(item).resolve(getJsonFilename(item));
	}

	private Path getThumbnailPath(IndexedItem item) {
		return getArchivePath(item).resolve(getThumbnailFilename(item));
	}

	private String getArchiveFilename(IndexedItem item) {
		String extension = getArchiveExtension(item);
		return getArchiveBasename(item) + (StringUtils.isBlank(extension) ? "" : "." + extension.toLowerCase());
	}

	private String getThumbnailFilename(IndexedItem item) {
		return "." + getArchiveBasename(item) + THUMBNAIL_SUFFIX + "." + StoredItemThumbnail.THUMBNAIL_FORMAT;
	}

	private String getJsonFilename(IndexedItem item) {
		return getArchiveBasename(item) + JSON_EXT;
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

	private Path getArchivePath(IndexedItem item) {
		if (item.isArchived()) {
			return getArchivedPath(item);
		} else {
			return getNotArchivedPath(item);
		}
	}

	private Path getNotArchivedPath(IndexedItem item) {
		return Paths.get(getDatasetPath(item.getDataset()).toString(), getTypeDir(item.getType()), NOT_ARCHIVED_DIR);
	}

	private Path getArchivedPath(IndexedItem item) {
		ZonedDateTime archiveTime = item.getArchiveTimeAsZoned("UTC");
		return Paths.get(getDatasetPath(item.getDataset()).toString(), getTypeDir(item.getType()), ARCHIVED_DIR,
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

}
