package org.atypical.carabassa.indexer.rdbms.component;

import org.atypical.carabassa.core.component.indexer.UserIndexer;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.Tag;
import org.atypical.carabassa.core.model.User;
import org.atypical.carabassa.core.model.enums.SearchOperator;
import org.atypical.carabassa.core.model.impl.SearchConditionImpl;
import org.atypical.carabassa.core.model.impl.SearchCriteriaImpl;
import org.atypical.carabassa.indexer.rdbms.entity.specification.ItemSpecification;
import org.atypical.carabassa.indexer.rdbms.repository.IndexedItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class UserDbIndexer implements UserIndexer {

    @Autowired
    private IndexedItemRepository indexedItemRepository;

    @Override
    public Page<IndexedItem> findFavorites(User user, Pageable pageable) {
        Assert.notNull(user, "User can not be null.");
        Assert.hasText(user.getUsername(), "Username can not be blank.");

        SearchCriteriaImpl criteria = new SearchCriteriaImpl();
        criteria.add(new SearchConditionImpl(Tag.FAVORITE_NAME, SearchOperator.EQUAL, user.getUsername()));

        Sort sort = pageable.getSort();
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return indexedItemRepository.findAll(new ItemSpecification(null, criteria, sort), unsortedPageable)
                .map(item -> item);
    }

}
