package org.atypical.carabassa.indexer.rdbms.repository;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndexedItemRepository
        extends CrudRepository<IndexedItemEntity, Long>, JpaSpecificationExecutor<IndexedItemEntity> {

    @Query("from IndexedItemEntity i where i.dataset=:dataset")
    Page<IndexedItem> findItems(Dataset dataset, Pageable pageable);

    @Query("from IndexedItemEntity where dataset=:dataset and id=:itemId")
    Optional<IndexedItem> findItemById(Dataset dataset, Long itemId);

    @Query("from IndexedItemEntity where dataset=:dataset and hash=:hash")
    Optional<IndexedItem> findItemByHash(Dataset dataset, String hash);

}
