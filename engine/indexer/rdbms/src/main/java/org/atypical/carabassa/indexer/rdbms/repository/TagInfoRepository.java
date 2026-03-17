package org.atypical.carabassa.indexer.rdbms.repository;

import java.util.Optional;

import org.atypical.carabassa.indexer.rdbms.entity.TagInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagInfoRepository extends JpaRepository<TagInfoEntity, Long> {

    Optional<TagInfoEntity> findByTagName(String tagName);

    Optional<TagInfoEntity> findByAlias(String alias);
}
