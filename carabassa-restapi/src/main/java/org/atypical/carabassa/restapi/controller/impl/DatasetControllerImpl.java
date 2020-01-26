package org.atypical.carabassa.restapi.controller.impl;

import java.io.IOException;

import javax.transaction.Transactional;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.core.model.StoredImage;
import org.atypical.carabassa.core.service.DatasetService;
import org.atypical.carabassa.restapi.controller.DatasetController;
import org.atypical.carabassa.restapi.dto.DatasetDto;
import org.atypical.carabassa.restapi.dto.ImageDto;
import org.atypical.carabassa.restapi.dto.TagDto;
import org.atypical.carabassa.restapi.mapper.DatasetMapper;
import org.atypical.carabassa.restapi.mapper.ImageMapper;
import org.atypical.carabassa.restapi.mapper.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
@Transactional(rollbackOn = Exception.class)
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

	@Override
	public Long create(DatasetDto datasetDto) {
		Dataset dataset = null;
		try {
			dataset = datasetService.create(datasetDto.getName());
			datasetMapper.update(datasetDto, dataset);
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
	public Page<DatasetDto> findAll(Pageable pageable) {
		return datasetService.findAll(pageable).map(d -> datasetMapper.toDTO(d));
	}

	@Override
	public DatasetDto findById(Long datasetId) {
		Dataset dataset = getDataset(datasetId);
		return datasetMapper.toDTO(dataset);
	}

	@Override
	public void update(Long datasetId, DatasetDto datasetDto) {
		Dataset dataset = getDataset(datasetId);
		datasetMapper.update(datasetDto, dataset);
		try {
			datasetService.update(dataset);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public Page<ImageDto> getImages(Long datasetId, Pageable pageable) {
		Dataset dataset = getDataset(datasetId);
		return datasetService.findImages(dataset, pageable).map(i -> imageMapper.toDTO(i));
	}

	@Override
	public ImageDto getImage(Long datasetId, Long imageId) {
		IndexedImage indexedImage = getIndexedImage(getDataset(datasetId), imageId);
		return imageMapper.toDTO(indexedImage);
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
	public Long addImageTag(Long datasetId, Long imageId, TagDto tagDto) {
		Dataset dataset = getDataset(datasetId);
		try {
			return datasetService.addImageTag(dataset, imageId, tagMapper.toEntity(tagDto));
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
