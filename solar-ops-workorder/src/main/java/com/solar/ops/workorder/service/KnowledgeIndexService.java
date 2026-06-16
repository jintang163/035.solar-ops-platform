package com.solar.ops.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.workorder.document.KnowledgeDocument;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.mapper.FaultLibraryMapper;
import com.solar.ops.workorder.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexService {

    private final FaultLibraryMapper faultLibraryMapper;
    private final KnowledgeRepository knowledgeRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void init() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(KnowledgeDocument.class);
            if (!indexOps.exists()) {
                log.info("ES索引不存在，开始创建索引 knowledge_base");
                indexOps.create();
                indexOps.putMapping();
                log.info("ES索引创建成功，开始全量同步数据");
                fullSync();
            } else {
                log.info("ES索引 knowledge_base 已存在，检查是否需要同步数据");
                long esCount = knowledgeRepository.count();
                if (esCount == 0) {
                    log.info("ES索引数据为空，触发全量同步");
                    fullSync();
                }
            }
        } catch (Exception e) {
            log.warn("ES索引初始化失败（可能ES服务未启动），搜索功能将降级为数据库查询: {}", e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void fullSync() {
        LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultLibrary::getDeleted, 0);
        List<FaultLibrary> allData = faultLibraryMapper.selectList(wrapper);

        if (CollectionUtils.isEmpty(allData)) {
            log.info("无数据需要同步");
            return;
        }

        List<KnowledgeDocument> docs = allData.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

        try {
            knowledgeRepository.saveAll(docs);
            log.info("知识库全量同步完成，共{}条记录", docs.size());
        } catch (Exception e) {
            log.error("知识库全量同步到ES失败", e);
            throw new RuntimeException("ES同步失败: " + e.getMessage(), e);
        }
    }

    public void syncById(Long id) {
        FaultLibrary entity = faultLibraryMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            deleteById(id);
            return;
        }
        KnowledgeDocument doc = convertToDocument(entity);
        try {
            knowledgeRepository.save(doc);
            log.debug("知识库ID={} 同步到ES成功", id);
        } catch (Exception e) {
            log.warn("知识库ID={} 同步到ES失败: {}", id, e.getMessage());
        }
    }

    public void deleteById(Long id) {
        try {
            knowledgeRepository.deleteById(id);
            log.debug("知识库ID={} 从ES删除成功", id);
        } catch (Exception e) {
            log.warn("知识库ID={} 从ES删除失败: {}", id, e.getMessage());
        }
    }

    public void batchSync(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) return;
        LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FaultLibrary::getId, ids)
                .eq(FaultLibrary::getDeleted, 0);
        List<FaultLibrary> list = faultLibraryMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(list)) {
            List<KnowledgeDocument> docs = list.stream()
                    .map(this::convertToDocument)
                    .collect(Collectors.toList());
            try {
                knowledgeRepository.saveAll(docs);
            } catch (Exception e) {
                log.warn("批量同步ES失败: {}", e.getMessage());
            }
        }
    }

    public boolean isEsAvailable() {
        try {
            knowledgeRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private KnowledgeDocument convertToDocument(FaultLibrary e) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(e.getId());
        doc.setFaultCode(e.getFaultCode());
        doc.setFaultName(e.getFaultName());
        doc.setFaultLevel(e.getFaultLevel());
        doc.setFaultType(e.getFaultType());
        doc.setFaultDesc(e.getFaultDesc());
        doc.setSolution(e.getSolution());
        doc.setSolutionRichText(stripHtml(e.getSolutionRichText()));
        doc.setVideoUrl(e.getVideoUrl());
        doc.setTags(e.getTags());
        doc.setTagList(parseTags(e.getTags()));
        doc.setLikeCount(e.getLikeCount() != null ? e.getLikeCount() : 0);
        doc.setDislikeCount(e.getDislikeCount() != null ? e.getDislikeCount() : 0);
        doc.setViewCount(e.getViewCount() != null ? e.getViewCount() : 0);
        doc.setUseCount(e.getUseCount() != null ? e.getUseCount() : 0);
        doc.setCreatorId(e.getCreatorId());
        doc.setCreatorName(e.getCreatorName());
        doc.setStatus(e.getStatus() != null ? e.getStatus() : 1);
        doc.setCreateTime(e.getCreateTime());
        doc.setUpdateTime(e.getUpdateTime());
        doc.setQualityScore(calculateQualityScore(doc).doubleValue());
        return doc;
    }

    private List<String> parseTags(String tags) {
        if (!StringUtils.hasText(tags)) return Collections.emptyList();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String stripHtml(String html) {
        if (!StringUtils.hasText(html)) return html;
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private BigDecimal calculateQualityScore(KnowledgeDocument d) {
        int likes = d.getLikeCount() != null ? d.getLikeCount() : 0;
        int dislikes = d.getDislikeCount() != null ? d.getDislikeCount() : 0;
        int uses = d.getUseCount() != null ? d.getUseCount() : 0;
        int views = d.getViewCount() != null ? d.getViewCount() : 0;

        double ratingScore = 0.5;
        if (likes + dislikes > 0) {
            ratingScore = (double) likes / (likes + dislikes);
            ratingScore = 0.3 + 0.7 * ratingScore;
        }
        double usageScore = Math.min(1.0, uses / 10.0);
        double contentScore = 0.5;
        if (StringUtils.hasText(d.getSolutionRichText())) {
            contentScore += 0.25;
        }
        if (StringUtils.hasText(d.getTags())) {
            contentScore += 0.15;
        }
        if (StringUtils.hasText(d.getVideoUrl())) {
            contentScore += 0.1;
        }

        double score = ratingScore * 0.4 + usageScore * 0.3 + contentScore * 0.3;
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }
}
