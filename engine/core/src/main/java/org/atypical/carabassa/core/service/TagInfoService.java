package org.atypical.carabassa.core.service;

import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.TagInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TagInfoService {

    TagInfo create(TagInfo tagInfo) throws EntityExistsException;

    TagInfo update(Long tagInfoId, TagInfo tagInfo) throws EntityNotFoundException, EntityExistsException;

    void delete(Long tagInfoId) throws EntityNotFoundException;

    TagInfo findById(Long tagInfoId) throws EntityNotFoundException;

    Page<TagInfo> findAll(Pageable pageable);
}
