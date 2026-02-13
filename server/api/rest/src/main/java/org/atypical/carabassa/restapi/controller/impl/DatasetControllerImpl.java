package org.atypical.carabassa.restapi.controller.impl;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.SearchCriteria;
import org.atypical.carabassa.core.model.StoredItem;
import org.atypical.carabassa.core.model.StoredItemThumbnail;
import org.atypical.carabassa.core.model.enums.ItemType;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.core.util.MediaTypeDetector;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.helper.SearchCriteriaParser;
import org.atypical.carabassa.restapi.representation.assembler.DatasetModelAssembler;
import org.atypical.carabassa.restapi.representation.assembler.ItemModelAssembler;
import org.atypical.carabassa.restapi.representation.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.representation.mapper.ItemMapper;
import org.atypical.carabassa.restapi.representation.mapper.TagMapper;
import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.IdRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Component
public class DatasetControllerImpl implements DatasetController {

    private static final Logger logger = LoggerFactory.getLogger(DatasetControllerImpl.class);

    private static final String TEMP_FILE_PREFIX = "item";

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private DatasetModelAssembler datasetModelAssembler;

    @Autowired
    private ItemModelAssembler itemModelAssembler;

    @Autowired
    private PagedResourcesAssembler<Dataset> datasetPagedResourcesAssembler;

    @Autowired
    private PagedResourcesAssembler<IndexedItem> itemPagedResourcesAssembler;

    @Value("${carabassa.tempdir:#{null}}")
    private String tempDirPath;

    @Override
    public IdRepresentation create(DatasetEditableRepresentation datasetRepresentation) {
        Dataset dataset;
        try {
            dataset = datasetMapper.toEntity(datasetRepresentation);
            dataset = datasetService.create(dataset);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (IllegalArgumentException | EntityExistsException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        return new IdRepresentation(dataset.getId());
    }

    @Override
    public void delete(Long datasetId) {
        try {
            Dataset dataset = getDataset(datasetId);
            datasetService.delete(dataset);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public PagedModel<DatasetEntityRepresentation> findAll(Pageable pageable) {
        Page<Dataset> page = datasetService.findAll(pageable);
        return datasetPagedResourcesAssembler.toModel(page, datasetModelAssembler);
    }

    @Override
    public DatasetEntityRepresentation findById(Long datasetId) {
        Dataset dataset = getDataset(datasetId);
        return datasetMapper.toRepresentation(dataset);
    }

    @Override
    public DatasetEntityRepresentation findByName(String datasetName) {
        Dataset dataset;
        try {
            dataset = datasetService.findByName(datasetName);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return datasetMapper.toRepresentation(dataset);
    }

    @Override
    public void update(Long datasetId, DatasetEditableRepresentation datasetRepresentation) {
        Dataset dataset = getDataset(datasetId);
        String originalDatasetName = dataset.getName();
        datasetMapper.update(datasetRepresentation, dataset);
        try {
            datasetService.update(originalDatasetName, dataset);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public PagedModel<ItemRepresentation> findItems(Long datasetId, String search, Pageable pageable) {
        Dataset dataset = getDataset(datasetId);

        SearchCriteria searchCriteria = null;
        if (search != null) {
            searchCriteria = SearchCriteriaParser.parse(search);
        }

        Page<IndexedItem> page;
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            page = datasetService.findItems(dataset, pageable);
        } else {
            page = datasetService.findItems(dataset, searchCriteria, pageable);
        }

        return itemPagedResourcesAssembler.toModel(page, itemModelAssembler);
    }

    @Override
    public ItemRepresentation findItem(Long datasetId, Long itemId) {
        IndexedItem indexedItem = getIndexedItem(getDataset(datasetId), itemId);
        return itemMapper.toRepresentation(indexedItem);
    }

    @Override
    public void existsItem(Long datasetId, String hash) {
        try {
            datasetService.findItemByHash(getDataset(datasetId), hash);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Resource> findItemContent(Long datasetId, Long itemId) {
        Dataset dataset = getDataset(datasetId);
        IndexedItem indexedItem = getIndexedItem(dataset, itemId);

        StoredItem storedItem;
        try {
            storedItem = datasetService.getStoredItem(dataset, indexedItem);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return ResponseEntity.ok() //
                .contentType(MediaType.parseMediaType(ItemType.IMAGE.normalized() + "/" + indexedItem.getFormat()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + indexedItem.getFilename() + "\"")
                .body(storedItem.getResource());
    }

    @Override
    public ResponseEntity<byte[]> findItemThumbnail(Long datasetId, Long itemId) {
        Dataset dataset = getDataset(datasetId);
        IndexedItem indexedItem = getIndexedItem(dataset, itemId);

        StoredItemThumbnail storedItemThumbnail;
        try {
            storedItemThumbnail = datasetService.getStoredItemThumbnail(dataset, indexedItem);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return ResponseEntity.ok() //
                .contentType(
                        MediaType.parseMediaType(ItemType.IMAGE.normalized() + "/" + storedItemThumbnail.getFormat()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + storedItemThumbnail.getFilename() + "\"")
                .body(storedItemThumbnail.getContent());
    }

    @Override
    public IdRepresentation addItem(Long datasetId, MultipartFile file) {
        Dataset dataset = getDataset(datasetId);
        try {
            IndexedItem indexedItem = datasetService.addItem(dataset, MediaTypeDetector.convert(file.getContentType()),
                    file.getOriginalFilename(), getTempResource(file));
            return new IdRepresentation(indexedItem.getId());
        } catch (EntityExistsException e) {
            logger.error(String.format("Error adding file %s already exists.", file.getOriginalFilename()));
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            logger.error(String.format("Error adding file %s.", file.getOriginalFilename()), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public IdRepresentation addItemTag(Long datasetId, Long itemId, TagEditableRepresentation tagRepresentation) {
        Dataset dataset = getDataset(datasetId);
        try {
            Long tagId = datasetService.addItemTag(dataset, itemId, tagMapper.toEntity(tagRepresentation));
            return new IdRepresentation(tagId);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public void deleteItem(Long datasetId, Long itemId) {
        Dataset dataset = getDataset(datasetId);
        try {
            datasetService.deleteItem(dataset, itemId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public void deleteItemTag(Long datasetId, Long itemId, Long tagId) {
        Dataset dataset = getDataset(datasetId);
        try {
            datasetService.deleteItemTag(dataset, itemId, tagId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public void reindex(Long datasetId, Long itemId) {
        Dataset dataset = getDataset(datasetId);
        try {
            datasetService.reindex(dataset, itemId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (EntityExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Dataset getDataset(Long datasetId) {
        Dataset dataset;
        try {
            dataset = datasetService.findById(datasetId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return dataset;
    }

    private IndexedItem getIndexedItem(Dataset dataset, Long itemId) {
        IndexedItem indexedItem;
        try {
            indexedItem = datasetService.findItemById(dataset, itemId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return indexedItem;
    }

    /**
     * This method is intended to write uploaded file in temporal file to avoid
     * problems when the indexer tries to access to a file to detect media type and
     * extract metadata.
     * It also renames the uploaded file instead of creating a new one using the
     * transferTo method.
     *
     * @param file the multi part file
     * @return resource persisted in file system
     * @throws IOException i/o exception
     */
    private Resource getTempResource(MultipartFile file) throws IOException {
        File tempFile;
        if (tempDirPath != null) {
            tempFile = File.createTempFile(TEMP_FILE_PREFIX, null, Paths.get(tempDirPath).toFile());
        } else {
            tempFile = File.createTempFile(TEMP_FILE_PREFIX, null);
        }
        file.transferTo(tempFile);
        return new FileSystemResource(tempFile);
    }

}
