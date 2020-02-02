package org.atypical.carabassa.restapi.controller.impl;

import java.io.IOException;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.mapper.ImageMapper;
import org.atypical.carabassa.restapi.mapper.TagMapper;
import org.atypical.carabassa.restapi.representation.assembler.DatasetModelAssembler;
import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class DatasetControllerImpl implements DatasetController {

	private static final Logger logger = LoggerFactory.getLogger(DatasetControllerImpl.class);

	@Autowired
	private DatasetService datasetService;

	@Autowired
	private DatasetMapper datasetMapper;

	@Autowired
	private ImageMapper imageMapper;

	@Autowired
	private TagMapper tagMapper;

	@Autowired
	private DatasetModelAssembler datasetModelAssembler;

	@Autowired
	private PagedResourcesAssembler<Dataset> pagedResourcesAssembler;

	@Override
	public Long create(DatasetRepresentation datasetRepresentation) {
		Dataset dataset = null;
		try {
			dataset = datasetService.create(datasetRepresentation.getName());
			datasetMapper.update(datasetRepresentation, dataset);
			datasetService.update(dataset);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		} catch (EntityExistsException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		}

		return dataset.getId();
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
	public PagedModel<DatasetRepresentation> findAll(Pageable pageable) {
		Page<Dataset> page = datasetService.findAll(pageable);
		return pagedResourcesAssembler.toModel(page, datasetModelAssembler);
	}

	@Override
	public DatasetRepresentation findById(Long datasetId) {
		Dataset dataset = getDataset(datasetId);
		return datasetMapper.toRepresentation(dataset);
	}

	@Override
	public DatasetRepresentation findByName(String datasetName) {
		Dataset dataset = null;
		try {
			dataset = datasetService.findByName(datasetName);
		} catch (EntityNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
		return datasetMapper.toRepresentation(dataset);
	}

	@Override
	public void update(Long datasetId, DatasetRepresentation datasetRepresentation) {
		Dataset dataset = getDataset(datasetId);
		datasetMapper.update(datasetRepresentation, dataset);
		try {
			datasetService.update(dataset);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public Page<ImageRepresentation> getImages(Long datasetId, Pageable pageable) {
		Dataset dataset = getDataset(datasetId);
		return datasetService.findImages(dataset, pageable).map(i -> imageMapper.toRepresentation(i));
	}

	@Override
	public ImageRepresentation getImage(Long datasetId, Long imageId) {
		IndexedImage indexedImage = getIndexedImage(getDataset(datasetId), imageId);
		return imageMapper.toRepresentation(indexedImage);
	}

	@Override
	public ResponseEntity<byte[]> getImageContent(Long datasetId, Long imageId) {
		Dataset dataset = getDataset(datasetId);
		IndexedImage indexedImage = getIndexedImage(dataset, imageId);

		StoredImage storedImage;
		try {
			storedImage = datasetService.getStoredImage(dataset, indexedImage);
		} catch (EntityNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}

		return ResponseEntity.ok() //
				.contentType(MediaType.parseMediaType("image/" + indexedImage.getFileType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + indexedImage.getFilename() + "\"")
				.body(storedImage.getContent());
	}

	@Override
	public Long addImage(Long datasetId, MultipartFile file) {
		Dataset dataset = getDataset(datasetId);
		try {
			IndexedImage indexedImage = datasetService.addImage(dataset, file.getResource());
			return indexedImage.getId();
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		} catch (EntityExistsException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public Long addImageTag(Long datasetId, Long imageId, TagRepresentation tagRepresentation) {
		Dataset dataset = getDataset(datasetId);
		try {
			return datasetService.addImageTag(dataset, imageId, tagMapper.toEntity(tagRepresentation));
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
		} catch (EntityNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	@Override
	public void deleteImage(Long datasetId, Long imageId) {
		Dataset dataset = getDataset(datasetId);
		try {
			datasetService.deleteImage(dataset, imageId);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public void deleteImageTag(Long datasetId, Long imageId, Long tagId) {
		Dataset dataset = getDataset(datasetId);
		try {
			datasetService.deleteImageTag(dataset, imageId, tagId);
		} catch (EntityNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	private Dataset getDataset(Long datasetId) {
		Dataset dataset;
		try {
			dataset = datasetService.findById(datasetId);
		} catch (EntityNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
		return dataset;
	}

	private IndexedImage getIndexedImage(Dataset dataset, Long imageId) {
		IndexedImage indexedImage;
		try {
			indexedImage = datasetService.findImageById(dataset, imageId);
		} catch (EntityNotFoundException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
		return indexedImage;
	}

}
