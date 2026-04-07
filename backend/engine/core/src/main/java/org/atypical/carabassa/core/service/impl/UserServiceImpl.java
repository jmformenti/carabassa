package org.atypical.carabassa.core.service.impl;

import org.atypical.carabassa.core.component.indexer.UserIndexer;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.User;
import org.atypical.carabassa.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserIndexer userIndexer;

    @Override
    public Page<IndexedItem> findFavorites(User user, Pageable pageable) {
        return userIndexer.findFavorites(user, pageable);
    }

}
