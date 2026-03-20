package org.atypical.carabassa.restapi.controller;

import org.atypical.carabassa.restapi.representation.model.IdRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEntityRepresentation;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = {"/api/tag-info"})
@CrossOrigin
public interface TagInfoController {

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    IdRepresentation create(@RequestBody @Valid TagInfoEditableRepresentation tagInfoRepresentation);

    @GetMapping
    PagedModel<TagInfoEntityRepresentation> findAll(Pageable pageable);

    @GetMapping(value = "/{tagInfoId}")
    TagInfoEntityRepresentation findById(@PathVariable("tagInfoId") Long tagInfoId);

    @PutMapping(value = "/{tagInfoId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    void update(@PathVariable("tagInfoId") Long tagInfoId,
                @RequestBody @Valid TagInfoEditableRepresentation tagInfoRepresentation);

    @DeleteMapping(value = "/{tagInfoId}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    void delete(@PathVariable("tagInfoId") Long tagInfoId);
}
