package com.solar.ops.workorder.repository;

import com.solar.ops.workorder.document.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeRepository extends ElasticsearchRepository<KnowledgeDocument, Long> {

    Page<KnowledgeDocument> findByStatus(Integer status, Pageable pageable);

    List<KnowledgeDocument> findByFaultCode(String faultCode);

    Page<KnowledgeDocument> findByFaultLevelAndStatus(Integer faultLevel, Integer status, Pageable pageable);
}
