package org.atypical.carabassa.core.service;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<IndexedItem> findFavorites(User user, Pageable pageable);

}
