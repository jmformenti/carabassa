package org.atypical.carabassa.core.db.repository;

import java.util.Optional;

import org.atypical.carabassa.core.db.entity.DatasetEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetRepository extends PagingAndSortingRepository<DatasetEntity, Long> {

	public Optional<DatasetEntity> findByName(String name);

}
