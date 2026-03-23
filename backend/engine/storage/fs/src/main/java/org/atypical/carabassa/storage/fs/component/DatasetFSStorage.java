package org.atypical.carabassa.storage.fs.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.coobird.thumbnailator.Thumbnails;
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

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;

@Component
public class DatasetFSStorage implements DatasetStorage {

    private static final String NO_REPO_DIR_MESSAGE_KEY = "core.storage.repo.no_dir";
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

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void postConstruct() throws IOException {
        Assert.notNull(repoDir, localizedMessage.getText(NO_REPO_DIR_MESSAGE_KEY));
        if (!Files.exists(Paths.get(repoDir))) {
            Files.createDirectories(Paths.get(repoDir));
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
        Path itemPath = getItemPath(item);
        if (!Files.exists(itemPath)) {
            throw new EntityNotFoundException(localizedMessage.getText(ITEM_NOT_EXISTS_MESSAGE_KEY, item.getId()));
        }
        storedItem.setResource(new FileSystemResource(itemPath));
        try {
            storedItem.setStoredItemInfo(readJson(item));
        } catch (FileNotFoundException e) {
            storedItem.setStoredItemInfo(null);
        }

        return storedItem;
    }

    @Override
    public StoredItemThumbnail getItemThumbnail(IndexedItem item) throws IOException, EntityNotFoundException {
        Path itemPath = getItemPath(item);

        if (Files.exists(itemPath)) {
            Path thumbnailPath = getThumbnailPath(item);

            byte[] contents;
            if (Files.exists(thumbnailPath)) {
                contents = Files.readAllBytes(thumbnailPath);
            } else {
                contents = writeThumbnail(item);
            }

            if (contents != null) {
                return new StoredItemThumbnailImpl(getThumbnailFilename(item), contents);
            } else {
                throw new UnsupportedOperationException(
                        "Thumbnail generation not supported for item type: " + item.getType());
            }
        } else {
            throw new EntityNotFoundException(localizedMessage.getText(ITEM_NOT_EXISTS_MESSAGE_KEY, item.getId()));
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
        Path trashPath = getTrashPath(item.getDataset());
        if (!Files.exists(trashPath)) {
            Files.createDirectories(trashPath);
        }

        moveIfExists(getItemPath(item), trashPath.resolve(getArchiveFilename(item)));
        moveIfExists(getJsonPath(item), trashPath.resolve(getJsonFilename(item)));
        moveIfExists(getThumbnailPath(item), trashPath.resolve(getThumbnailFilename(item)));
    }

    private void moveIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path getTrashPath(Dataset dataset) {
        return Paths.get(repoDir, "trash", dataset.getName());
    }

    @Override
    public void reindex(IndexedItem updatedItem, IndexedItem previousItem) throws IOException, EntityExistsException {
        Path previousItemPath = getItemPath(previousItem);
        Path updatedItemPath = getItemPath(updatedItem);

        if (!updatedItemPath.equals(previousItemPath)) {
            Resource itemResource = new FileSystemResource(previousItemPath);

            addItem(updatedItem, itemResource);
            deleteItem(previousItem);
        }
    }

    @Override
    public void update(String originalDatasetName, Dataset updatedDataset) throws IOException {
        if (!originalDatasetName.equals(updatedDataset.getName())) {
            Files.move(getDatasetPath(originalDatasetName), getDatasetPath(updatedDataset.getName()));
        }
    }

    private byte[] writeThumbnail(IndexedItem item) throws IOException {
        byte[] contents = null;
        if (item.getType() == ItemType.IMAGE) {
            contents = createThumbnail(getItemPath(item));
        } else if (item.getType() == ItemType.VIDEO) {
            contents = createVideoThumbnail(getItemPath(item));
        }

        if (contents != null) {
            Files.write(getThumbnailPath(item), contents);
        }

        return contents;
    }

    private byte[] createThumbnail(Path itemPath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(itemPath.toFile()).size(200, 200).keepAspectRatio(true).toOutputStream(baos);
        return baos.toByteArray();
    }

    private byte[] createVideoThumbnail(Path itemPath) throws IOException {
        Path tempThumbnail = Files.createTempFile("vthumb", ".jpg");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", itemPath.toString(),
                    "-ss", "00:00:01.000", "-vframes", "1",
                    "-vf", "scale=200:200:force_original_aspect_ratio=decrease",
                    "-f", "image2",
                    tempThumbnail.toString());
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0 && Files.exists(tempThumbnail)) {
                return Files.readAllBytes(tempThumbnail);
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Video thumbnail generation interrupted", e);
        } finally {
            Files.deleteIfExists(tempThumbnail);
        }
    }

    private StoredItemInfo readJson(IndexedItem item) throws IOException {
        return mapper.readValue(getJsonPath(item).toFile(), StoredItemInfoImpl.class);
    }

    private void writeJson(IndexedItem item) throws IOException {
        mapper.writeValue(getJsonPath(item).toFile(), new StoredItemInfoImpl(item.getFilename()));
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
