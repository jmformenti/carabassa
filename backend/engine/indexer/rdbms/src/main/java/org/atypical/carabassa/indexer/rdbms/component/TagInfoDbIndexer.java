package org.atypical.carabassa.indexer.rdbms.component;

import org.atypical.carabassa.core.component.indexer.TagInfoIndexer;
import org.atypical.carabassa.core.component.util.LocalizedMessage;
import org.atypical.carabassa.core.exception.EntityExistsException;
import org.atypical.carabassa.core.exception.EntityNotFoundException;
import org.atypical.carabassa.core.model.TagInfo;
import org.atypical.carabassa.indexer.rdbms.entity.TagInfoEntity;
import org.atypical.carabassa.indexer.rdbms.repository.TagInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = Exception.class)
public class TagInfoDbIndexer implements TagInfoIndexer {

    private static final String TAG_INFO_ID_NOT_FOUND_MESSAGE_KEY = "db.indexer.taginfo.id_not_found";
    private static final String TAG_INFO_NAME_EXISTS_MESSAGE_KEY = "db.indexer.taginfo.name_exists";
    private static final String TAG_INFO_ALIAS_EXISTS_MESSAGE_KEY = "db.indexer.taginfo.alias_exists";

    @Autowired
    private TagInfoRepository tagInfoRepository;

    @Autowired
    private LocalizedMessage localizedMessage;

    @Override
    public TagInfo create(TagInfo tagInfo) throws EntityExistsException {
        if (tagInfo.getInternal() == null) {
            tagInfo.setInternal(false);
        }
        if (tagInfo.getSortable() == null) {
            tagInfo.setSortable(false);
        }
        if (tagInfo.getShowInHelp() == null) {
            tagInfo.setShowInHelp(false);
        }
        if (tagInfoRepository.findByTagName(tagInfo.getTagName()).isPresent()) {
            throw new EntityExistsException(
                    localizedMessage.getText(TAG_INFO_NAME_EXISTS_MESSAGE_KEY, tagInfo.getTagName()));
        }
        if (tagInfo.getAlias() != null && tagInfoRepository.findByAlias(tagInfo.getAlias()).isPresent()) {
            throw new EntityExistsException(
                    localizedMessage.getText(TAG_INFO_ALIAS_EXISTS_MESSAGE_KEY, tagInfo.getAlias()));
        }
        TagInfoEntity entity = new TagInfoEntity(tagInfo);
        return tagInfoRepository.save(entity);
    }

    @Override
    public TagInfo update(TagInfo tagInfo) throws EntityNotFoundException, EntityExistsException {
        if (tagInfo.getInternal() == null) {
            tagInfo.setInternal(false);
        }
        if (tagInfo.getSortable() == null) {
            tagInfo.setSortable(false);
        }
        if (tagInfo.getShowInHelp() == null) {
            tagInfo.setShowInHelp(false);
        }
        TagInfoEntity existing = tagInfoRepository.findById(tagInfo.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        localizedMessage.getText(TAG_INFO_ID_NOT_FOUND_MESSAGE_KEY, tagInfo.getId())));

        if (!existing.getTagName().equals(tagInfo.getTagName())
                && tagInfoRepository.findByTagName(tagInfo.getTagName()).isPresent()) {
            throw new EntityExistsException(
                    localizedMessage.getText(TAG_INFO_NAME_EXISTS_MESSAGE_KEY, tagInfo.getTagName()));
        }

        String alias = tagInfo.getAlias();
        if (alias != null) {
            java.util.Optional<TagInfoEntity> aliasMatch = tagInfoRepository.findByAlias(alias);
            if (aliasMatch.isPresent() && !aliasMatch.get().getId().equals(existing.getId())) {
                throw new EntityExistsException(
                        localizedMessage.getText(TAG_INFO_ALIAS_EXISTS_MESSAGE_KEY, alias));
            }
        }

        existing.setTagName(tagInfo.getTagName());
        existing.setDescription(tagInfo.getDescription());
        existing.setAlias(tagInfo.getAlias());
        existing.setInternal(tagInfo.getInternal());
        existing.setSortable(tagInfo.getSortable());
        existing.setShowInHelp(tagInfo.getShowInHelp());
        return tagInfoRepository.save(existing);
    }

    @Override
    public void delete(Long tagInfoId) throws EntityNotFoundException {
        TagInfoEntity existing = tagInfoRepository.findById(tagInfoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        localizedMessage.getText(TAG_INFO_ID_NOT_FOUND_MESSAGE_KEY, tagInfoId)));
        tagInfoRepository.delete(existing);
    }

    @Override
    public TagInfo findById(Long tagInfoId) throws EntityNotFoundException {
        return tagInfoRepository.findById(tagInfoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        localizedMessage.getText(TAG_INFO_ID_NOT_FOUND_MESSAGE_KEY, tagInfoId)));
    }

    @Override
    public Page<TagInfo> findAll(Pageable pageable) {
        return tagInfoRepository.findAll(pageable).map(tagInfo -> tagInfo);
    }
}
