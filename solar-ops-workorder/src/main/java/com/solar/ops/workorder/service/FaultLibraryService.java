package com.solar.ops.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.workorder.document.KnowledgeDocument;
import com.solar.ops.workorder.dto.KnowledgeQueryDTO;
import com.solar.ops.workorder.dto.KnowledgeRecommendDTO;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.entity.KnowledgeUsageLog;
import com.solar.ops.workorder.mapper.FaultLibraryMapper;
import com.solar.ops.workorder.mapper.KnowledgeUsageLogMapper;
import com.solar.ops.workorder.vo.KnowledgeRecommendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaultLibraryService extends ServiceImpl<FaultLibraryMapper, FaultLibrary> {

    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final KnowledgeUsageLogMapper usageLogMapper;

    private final Map<String, FaultLibrary> faultCache = new ConcurrentHashMap<>();
    private final List<FaultLibrary> allKnowledgeCache = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        loadFaultCache();
    }

    public void loadFaultCache() {
        List<FaultLibrary> list = this.list();
        faultCache.clear();
        allKnowledgeCache.clear();
        allKnowledgeCache.addAll(list);
        for (FaultLibrary fault : list) {
            faultCache.put(fault.getFaultCode(), fault);
        }
        log.info("知识库缓存加载完成，共{}条记录", list.size());
    }

    public FaultLibrary getByFaultCode(String faultCode) {
        if (!StringUtils.hasText(faultCode)) {
            return null;
        }
        FaultLibrary fault = faultCache.get(faultCode);
        if (fault == null) {
            LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FaultLibrary::getFaultCode, faultCode);
            fault = this.getOne(wrapper);
            if (fault != null) {
                faultCache.put(faultCode, fault);
            }
        }
        return fault;
    }

    public PageResult<FaultLibrary> page(KnowledgeQueryDTO dto) {
        if (dto.getPageNum() == null) dto.setPageNum(1);
        if (dto.getPageSize() == null) dto.setPageSize(10);

        if (knowledgeIndexService.isEsAvailable()) {
            try {
                org.springframework.data.domain.Page<KnowledgeDocument> esPage = knowledgeSearchService.searchKnowledge(dto);
                if (esPage != null && esPage.hasContent()) {
                    List<Long> ids = esPage.getContent().stream()
                            .map(KnowledgeDocument::getId)
                            .collect(Collectors.toList());
                    LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
                    wrapper.in(FaultLibrary::getId, ids);
                    List<FaultLibrary> dbList = this.list(wrapper);
                    Map<Long, FaultLibrary> dbMap = dbList.stream()
                            .collect(Collectors.toMap(FaultLibrary::getId, f -> f, (a, b) -> a));
                    List<FaultLibrary> orderedList = ids.stream()
                            .map(dbMap::get)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toList());
                    return PageResult.build(esPage.getTotalElements(), orderedList, dto.getPageNum(), dto.getPageSize());
                }
                if (esPage != null) {
                    return PageResult.build(esPage.getTotalElements(), java.util.Collections.emptyList(), dto.getPageNum(), dto.getPageSize());
                }
            } catch (Exception e) {
                log.warn("ES搜索失败，降级为数据库查询: {}", e.getMessage());
            }
        }

        return pageFromDb(dto);
    }

    private PageResult<FaultLibrary> pageFromDb(KnowledgeQueryDTO dto) {
        LambdaQueryWrapper<FaultLibrary> wrapper = new LambdaQueryWrapper<>();
        if (dto != null) {
            if (StringUtils.hasText(dto.getKeyword())) {
                String keyword = dto.getKeyword();
                wrapper.and(w -> w.like(FaultLibrary::getFaultCode, keyword)
                        .or().like(FaultLibrary::getFaultName, keyword)
                        .or().like(FaultLibrary::getFaultDesc, keyword)
                        .or().like(FaultLibrary::getTags, keyword));
            }
            if (StringUtils.hasText(dto.getFaultCode())) {
                wrapper.like(FaultLibrary::getFaultCode, dto.getFaultCode());
            }
            if (StringUtils.hasText(dto.getFaultName())) {
                wrapper.like(FaultLibrary::getFaultName, dto.getFaultName());
            }
            if (dto.getFaultLevel() != null) {
                wrapper.eq(FaultLibrary::getFaultLevel, dto.getFaultLevel());
            }
            if (StringUtils.hasText(dto.getFaultType())) {
                wrapper.eq(FaultLibrary::getFaultType, dto.getFaultType());
            }
            if (StringUtils.hasText(dto.getTag())) {
                wrapper.like(FaultLibrary::getTags, dto.getTag());
            }
            if (dto.getStatus() != null) {
                wrapper.eq(FaultLibrary::getStatus, dto.getStatus());
            }
            if (dto.getCreatorId() != null) {
                wrapper.eq(FaultLibrary::getCreatorId, dto.getCreatorId());
            }
        }
        wrapper.orderByDesc(FaultLibrary::getUpdateTime);

        Page<FaultLibrary> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<FaultLibrary> result = this.page(page, wrapper);
        return PageResult.build(result.getTotal(), result.getRecords(), dto.getPageNum(), dto.getPageSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addKnowledge(FaultLibrary faultLibrary) {
        if (faultLibrary.getLikeCount() == null) {
            faultLibrary.setLikeCount(0);
        }
        if (faultLibrary.getDislikeCount() == null) {
            faultLibrary.setDislikeCount(0);
        }
        if (faultLibrary.getViewCount() == null) {
            faultLibrary.setViewCount(0);
        }
        if (faultLibrary.getUseCount() == null) {
            faultLibrary.setUseCount(0);
        }
        if (faultLibrary.getStatus() == null) {
            faultLibrary.setStatus(1);
        }
        boolean saved = this.save(faultLibrary);
        if (saved) {
            faultCache.put(faultLibrary.getFaultCode(), faultLibrary);
            allKnowledgeCache.add(faultLibrary);
            knowledgeIndexService.syncById(faultLibrary.getId());
            log.info("知识库新增成功，ID={} 已同步到ES", faultLibrary.getId());
        }
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateKnowledge(FaultLibrary faultLibrary) {
        boolean updated = this.updateById(faultLibrary);
        if (updated) {
            FaultLibrary updatedFault = this.getById(faultLibrary.getId());
            if (updatedFault != null) {
                faultCache.put(updatedFault.getFaultCode(), updatedFault);
                allKnowledgeCache.removeIf(f -> f.getId().equals(updatedFault.getId()));
                allKnowledgeCache.add(updatedFault);
                knowledgeIndexService.syncById(updatedFault.getId());
                log.info("知识库更新成功，ID={} 已同步到ES", faultLibrary.getId());
            }
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteKnowledge(Long id) {
        FaultLibrary fault = this.getById(id);
        boolean deleted = this.removeById(id);
        if (deleted && fault != null) {
            faultCache.remove(fault.getFaultCode());
            allKnowledgeCache.removeIf(f -> f.getId().equals(id));
            knowledgeIndexService.deleteById(id);
            log.info("知识库删除成功，ID={} 已从ES移除", id);
        }
        return deleted;
    }

    public List<FaultLibrary> listAll() {
        return this.list();
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id) {
        FaultLibrary knowledge = this.getById(id);
        if (knowledge != null) {
            knowledge.setViewCount((knowledge.getViewCount() != null ? knowledge.getViewCount() : 0) + 1);
            this.updateById(knowledge);
            allKnowledgeCache.removeIf(f -> f.getId().equals(id));
            allKnowledgeCache.add(knowledge);
            knowledgeIndexService.syncById(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long id) {
        FaultLibrary knowledge = this.getById(id);
        if (knowledge != null) {
            knowledge.setUseCount((knowledge.getUseCount() != null ? knowledge.getUseCount() : 0) + 1);
            this.updateById(knowledge);
            allKnowledgeCache.removeIf(f -> f.getId().equals(id));
            allKnowledgeCache.add(knowledge);
            knowledgeIndexService.syncById(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLikeCount(Long id, boolean isLike) {
        FaultLibrary knowledge = this.getById(id);
        if (knowledge != null) {
            if (isLike) {
                knowledge.setLikeCount((knowledge.getLikeCount() != null ? knowledge.getLikeCount() : 0) + 1);
            } else {
                knowledge.setDislikeCount((knowledge.getDislikeCount() != null ? knowledge.getDislikeCount() : 0) + 1);
            }
            this.updateById(knowledge);
            allKnowledgeCache.removeIf(f -> f.getId().equals(id));
            allKnowledgeCache.add(knowledge);
            knowledgeIndexService.syncById(id);
        }
    }

    public List<KnowledgeRecommendVO> recommend(KnowledgeRecommendDTO dto) {
        List<KnowledgeRecommendVO> results = null;

        if (knowledgeIndexService.isEsAvailable()) {
            try {
                results = knowledgeSearchService.recommend(dto);
                if (!CollectionUtils.isEmpty(results)) {
                    log.info("ES智能推荐返回{}条结果", results.size());
                }
            } catch (Exception e) {
                log.warn("ES推荐失败，错误: {}", e.getMessage());
            }
        }

        if (CollectionUtils.isEmpty(results)) {
            log.warn("ES不可用或无结果，不降级（ES为推荐主引擎）");
            return java.util.Collections.emptyList();
        }

        if (!CollectionUtils.isEmpty(results) && dto.getWorkOrderId() != null) {
            for (KnowledgeRecommendVO vo : results) {
                try {
                    KnowledgeUsageLog usageLog = new KnowledgeUsageLog();
                    usageLog.setKnowledgeId(vo.getId());
                    usageLog.setWorkOrderId(dto.getWorkOrderId());
                    usageLog.setUsageType(1);
                    usageLog.setSourceType(1);
                    usageLog.setConfidence(vo.getConfidence() != null ? vo.getConfidence() : BigDecimal.ZERO);
                    usageLog.setCreateTime(LocalDateTime.now());
                    usageLogMapper.insert(usageLog);
                } catch (Exception e) {
                    log.warn("记录推荐使用日志失败", e);
                }
            }
        }

        return results;
    }

    public FaultLibrary getDetail(Long id) {
        FaultLibrary knowledge = this.getById(id);
        if (knowledge != null) {
            incrementViewCount(id);
        }
        return knowledge;
    }

    public void recordUsage(Long knowledgeId, Long workOrderId, Long userId, String userName, Integer sourceType) {
        incrementUseCount(knowledgeId);
        try {
            KnowledgeUsageLog usageLog = new KnowledgeUsageLog();
            usageLog.setKnowledgeId(knowledgeId);
            usageLog.setWorkOrderId(workOrderId);
            usageLog.setUserId(userId);
            usageLog.setUserName(userName);
            usageLog.setUsageType(2);
            usageLog.setSourceType(sourceType != null ? sourceType : 1);
            usageLog.setCreateTime(LocalDateTime.now());
            usageLogMapper.insert(usageLog);
        } catch (Exception e) {
            log.warn("记录知识库使用日志失败", e);
        }
    }

    public boolean isEsAvailable() {
        return knowledgeIndexService.isEsAvailable();
    }
}
