package com.homeverse.search.repository;

import com.homeverse.search.document.PropertyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertySearchRepository extends ElasticsearchRepository<PropertyDocument, Long> {
}