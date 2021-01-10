package org.atypical.carabassa.indexer.rdbms.repository;

import java.util.Optional;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedItem;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexedItemRepository extends CrudRepository<IndexedItemEntity, Long> {

	@Query("from IndexedItemEntity i where i.dataset=:dataset")
	public Page<IndexedItem> findItems(Dataset dataset, Pageable pageable);

	@Query("from IndexedItemEntity where dataset=:dataset and id=:itemId")
	public Optional<IndexedItem> findItemById(Dataset dataset, Long itemId);

	@Query("from IndexedItemEntity where dataset=:dataset and hash=:hash")
	public Optional<IndexedItem> findItemByHash(Dataset dataset, String hash);

}
