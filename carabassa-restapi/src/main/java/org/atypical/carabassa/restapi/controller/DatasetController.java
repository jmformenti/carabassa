package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.restapi.representation.model.DatasetRepresentation;
import org.atypical.carabassa.restapi.representation.model.ImageRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagRepresentation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = { "/api/dataset" })
public interface DatasetController {

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public Long create(@RequestBody DatasetRepresentation datasetRepresentation);

	@GetMapping
	public PagedModel<DatasetRepresentation> findAll(Pageable pageable);

	@GetMapping(value = "{datasetId}")
	public DatasetRepresentation findById(@PathVariable("datasetId") Long datasetId);

	@GetMapping(value = "/name/{datasetName}")
	public DatasetRepresentation findByName(@PathVariable("datasetName") String datasetName);

	@PutMapping(value = "{datasetId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void update(@PathVariable("datasetId") Long datasetId,
			@RequestBody DatasetRepresentation datasetRepresentation);

	@DeleteMapping(value = "{datasetId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("datasetId") Long datasetId);

	@GetMapping(value = "{datasetId}/image")
	public Page<ImageRepresentation> getImages(@PathVariable("datasetId") Long datasetId, Pageable pageable);

	@GetMapping(value = "{datasetId}/image/{id}")
	public ImageRepresentation getImage(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long imageId);

	@GetMapping(value = "{datasetId}/image/{id}/content")
	public ResponseEntity<byte[]> getImageContent(@PathVariable("datasetId") Long datasetId,
			@PathVariable("id") Long imageId);

	@PostMapping(value = "{datasetId}/image")
	@ResponseStatus(code = HttpStatus.CREATED)
	public Long addImage(@PathVariable("datasetId") Long datasetId, @RequestParam("file") MultipartFile file);

	@PostMapping(value = "{datasetId}/image/{id}/tag")
	@ResponseStatus(code = HttpStatus.CREATED)
	public Long addImageTag(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long imageId,
			@RequestBody TagRepresentation tagRepresentation);

	@DeleteMapping(value = "{datasetId}/image/{id}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deleteImage(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long imageId);

	@DeleteMapping(value = "{datasetId}/image/{id}/tag/{tagId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deleteImageTag(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long imageId,
			@PathVariable("tagId") Long tagId);

}
