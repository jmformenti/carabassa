package org.atypical.carabassa.indexer.rdbms.repository;

import java.util.Optional;

import org.atypical.carabassa.indexer.rdbms.entity.DatasetEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetRepository extends PagingAndSortingRepository<DatasetEntity, Long> {

	Optional<DatasetEntity> findByName(String name);

}
