package org.atypical.carabassa.restapi.controller;

import javax.validation.Valid;

import org.atypical.carabassa.restapi.representation.model.DatasetEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.DatasetEntityRepresentation;
import org.atypical.carabassa.restapi.representation.model.IdRepresentation;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEditableRepresentation;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
// TODO add authentication
public interface DatasetController {

	@PostMapping
	@ResponseStatus(code = HttpStatus.CREATED)
	public IdRepresentation create(@RequestBody @Valid DatasetEditableRepresentation datasetRepresentation);

	@GetMapping
	public PagedModel<DatasetEntityRepresentation> findAll(Pageable pageable);

	@GetMapping(value = "/{datasetId}")
	public DatasetEntityRepresentation findById(@PathVariable("datasetId") Long datasetId);

	@GetMapping(value = "/name/{datasetName}")
	public DatasetEntityRepresentation findByName(@PathVariable("datasetName") String datasetName);

	@PutMapping(value = "/{datasetId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void update(@PathVariable("datasetId") Long datasetId,
			@RequestBody @Valid DatasetEditableRepresentation datasetRepresentation);

	@DeleteMapping(value = "/{datasetId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("datasetId") Long datasetId);

	@GetMapping(value = "/{datasetId}/item")
	public PagedModel<ItemRepresentation> findItems(@PathVariable("datasetId") Long datasetId,
			@RequestParam(value = "search", required = false) String search, Pageable pageable);

	@GetMapping(value = "/{datasetId}/item/{id}")
	public ItemRepresentation findItem(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long itemId);

	@GetMapping(value = "/{datasetId}/item/exists/{hash}")
	public void existsItem(@PathVariable("datasetId") Long datasetId, @PathVariable("hash") String hash);

	@GetMapping(value = "/{datasetId}/item/{id}/content")
	public ResponseEntity<byte[]> findItemContent(@PathVariable("datasetId") Long datasetId,
			@PathVariable("id") Long itemId);

	@GetMapping(value = "/{datasetId}/item/{id}/thumbnail")
	public ResponseEntity<byte[]> findItemThumbnail(@PathVariable("datasetId") Long datasetId,
			@PathVariable("id") Long itemId);

	@PostMapping(value = "/{datasetId}/item")
	@ResponseStatus(code = HttpStatus.CREATED)
	public IdRepresentation addItem(@PathVariable("datasetId") Long datasetId,
			@RequestParam("file") MultipartFile file);

	@PostMapping(value = "/{datasetId}/item/{id}/tag")
	@ResponseStatus(code = HttpStatus.CREATED)
	public IdRepresentation addItemTag(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long itemId,
			@RequestBody @Valid TagEditableRepresentation tagRepresentation);

	@DeleteMapping(value = "/{datasetId}/item/{id}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deleteItem(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long itemId);

	@DeleteMapping(value = "/{datasetId}/item/{id}/tag/{tagId}")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void deleteItemTag(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long itemId,
			@PathVariable("tagId") Long tagId);

	@PutMapping(value = "/{datasetId}/item/{id}/reset")
	@ResponseStatus(code = HttpStatus.NO_CONTENT)
	public void resetItem(@PathVariable("datasetId") Long datasetId, @PathVariable("id") Long itemId);

}
