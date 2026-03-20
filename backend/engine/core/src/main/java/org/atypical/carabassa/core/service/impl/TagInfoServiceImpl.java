package org.atypical.carabassa.core.service.impl;

import org.atypical.carabassa.core.component.indexer.TagInfoIndexer;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.core.service.TagInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TagInfoServiceImpl implements TagInfoService {

    @Autowired
    private TagInfoIndexer tagInfoIndexer;

    @Override
    public TagInfo create(TagInfo tagInfo) throws EntityExistsException {
        return tagInfoIndexer.create(tagInfo);
    }

    @Override
    public TagInfo update(Long tagInfoId, TagInfo tagInfo)
            throws EntityNotFoundException, EntityExistsException {
        tagInfo.setId(tagInfoId);
        return tagInfoIndexer.update(tagInfo);
    }

    @Override
    public void delete(Long tagInfoId) throws EntityNotFoundException {
        tagInfoIndexer.delete(tagInfoId);
    }

    @Override
    public TagInfo findById(Long tagInfoId) throws EntityNotFoundException {
        return tagInfoIndexer.findById(tagInfoId);
    }

    @Override
    public Page<TagInfo> findAll(Pageable pageable) {
        return tagInfoIndexer.findAll(pageable);
    }
}
