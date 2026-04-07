package org.atypical.carabassa.core.component.indexer;

import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserIndexer {

    Page<IndexedItem> findFavorites(User user, Pageable pageable);

}
