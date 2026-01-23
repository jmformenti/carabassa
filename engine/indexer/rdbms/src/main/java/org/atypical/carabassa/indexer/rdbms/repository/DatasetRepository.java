package org.atypical.carabassa.indexer.rdbms.repository;

import org.atypical.carabassa.indexer.rdbms.entity.DatasetEntity;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasetRepository extends ListCrudRepository<DatasetEntity, Long>, PagingAndSortingRepository<DatasetEntity, Long> {

    Optional<DatasetEntity> findByName(String name);

}
