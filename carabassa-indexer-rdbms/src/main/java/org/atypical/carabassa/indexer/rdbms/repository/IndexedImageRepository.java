package org.atypical.carabassa.indexer.rdbms.repository;

import java.util.Optional;

import org.atypical.carabassa.core.model.Dataset;
import org.atypical.carabassa.core.model.IndexedImage;
import org.atypical.carabassa.indexer.rdbms.entity.IndexedImageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndexedImageRepository extends CrudRepository<IndexedImageEntity, Long> {

	@Query("from IndexedImageEntity i where i.dataset=:dataset")
	public Page<IndexedImage> findImages(Dataset dataset, Pageable pageable);

	@Query("from IndexedImageEntity where dataset=:dataset and id=:imageId")
	public Optional<IndexedImage> findImageById(Dataset dataset, Long imageId);

	@Query("from IndexedImageEntity where dataset=:dataset and hash=:hash")
	public Optional<IndexedImage> findImageByHash(Dataset dataset, String hash);

}
