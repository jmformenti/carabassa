package org.atypical.carabassa.restapi.controller.impl;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.core.service.TagInfoService;
import org.atypical.carabassa.restapi.controller.TagInfoController;
import org.atypical.carabassa.restapi.representation.assembler.TagInfoModelAssembler;
import org.atypical.carabassa.restapi.representation.mapper.TagInfoMapper;
import org.atypical.carabassa.restapi.representation.model.IdRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEditableRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagInfoEntityRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TagInfoControllerImpl implements TagInfoController {

    private static final Logger logger = LoggerFactory.getLogger(TagInfoControllerImpl.class);

    @Autowired
    private TagInfoService tagInfoService;

    @Autowired
    private TagInfoMapper tagInfoMapper;

    @Autowired
    private TagInfoModelAssembler tagInfoModelAssembler;

    @Autowired
    private PagedResourcesAssembler<TagInfo> tagInfoPagedResourcesAssembler;

    @Override
    public IdRepresentation create(TagInfoEditableRepresentation tagInfoRepresentation) {
        TagInfo tagInfo;
        try {
            tagInfo = tagInfoMapper.toEntity(tagInfoRepresentation);
            tagInfo = tagInfoService.create(tagInfo);
        } catch (EntityExistsException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return new IdRepresentation(tagInfo.getId());
    }

    @Override
    public PagedModel<TagInfoEntityRepresentation> findAll(Pageable pageable) {
        Page<TagInfo> page = tagInfoService.findAll(pageable);
        return tagInfoPagedResourcesAssembler.toModel(page, tagInfoModelAssembler);
    }

    @Override
    public TagInfoEntityRepresentation findById(Long tagInfoId) {
        TagInfo tagInfo;
        try {
            tagInfo = tagInfoService.findById(tagInfoId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return tagInfoModelAssembler.toModel(tagInfo);
    }

    @Override
    public void update(Long tagInfoId, TagInfoEditableRepresentation tagInfoRepresentation) {
        try {
            TagInfo tagInfo = tagInfoMapper.toEntity(tagInfoRepresentation);
            tagInfoService.update(tagInfoId, tagInfo);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (EntityExistsException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @Override
    public void delete(Long tagInfoId) {
        try {
            tagInfoService.delete(tagInfoId);
        } catch (EntityNotFoundException e) {
            logger.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
