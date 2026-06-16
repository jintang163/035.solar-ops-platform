package com.solar.ops.workorder.service;

import com.solar.ops.workorder.document.KnowledgeDocument;
import com.solar.ops.workorder.dto.KnowledgeQueryDTO;
import com.solar.ops.workorder.dto.KnowledgeRecommendDTO;
import com.solar.ops.workorder.vo.KnowledgeRecommendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSearchService {

    private final KnowledgeIndexService knowledgeIndexService;
    private final ElasticsearchOperations elasticsearchOperations;

    public Page<KnowledgeDocument> searchKnowledge(KnowledgeQueryDTO dto) {
        if (!knowledgeIndexService.isEsAvailable()) {
            log.warn("ES不可用，searchKnowledge降级返回空");
            return new PageImpl<>(Collections.emptyList());
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StringUtils.hasText(dto.getKeyword())) {
            String kw = dto.getKeyword();
            BoolQueryBuilder keywordQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.termQuery("faultCode", kw).boost(10.0f))
                    .should(QueryBuilders.matchQuery("faultName", kw).boost(8.0f).analyzer("ik_smart"))
                    .should(QueryBuilders.matchQuery("tags", kw).boost(5.0f).analyzer("ik_smart"))
                    .should(QueryBuilders.matchQuery("faultDesc", kw).boost(3.0f).analyzer("ik_smart"))
                    .should(QueryBuilders.matchQuery("solution", kw).boost(2.0f).analyzer("ik_smart"))
                    .should(QueryBuilders.matchQuery("solutionRichText", kw).boost(1.5f).analyzer("ik_smart"));
            boolQuery.must(keywordQuery);
        }

        if (StringUtils.hasText(dto.getFaultCode())) {
            boolQuery.must(QueryBuilders.termQuery("faultCode", dto.getFaultCode()));
        }
        if (StringUtils.hasText(dto.getFaultName())) {
            boolQuery.must(QueryBuilders.matchQuery("faultName", dto.getFaultName()).analyzer("ik_smart"));
        }
        if (dto.getFaultLevel() != null) {
            boolQuery.must(QueryBuilders.termQuery("faultLevel", dto.getFaultLevel()));
        }
        if (StringUtils.hasText(dto.getFaultType())) {
            boolQuery.must(QueryBuilders.termQuery("faultType", dto.getFaultType()));
        }
        if (StringUtils.hasText(dto.getTag())) {
            boolQuery.must(QueryBuilders.termQuery("tagList", dto.getTag()));
        }
        if (dto.getStatus() != null) {
            boolQuery.must(QueryBuilders.termQuery("status", dto.getStatus()));
        }
        if (dto.getCreatorId() != null) {
            boolQuery.must(QueryBuilders.termQuery("creatorId", dto.getCreatorId()));
        }

        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("qualityScore")
                                        .factor(2.0f)
                                        .modifier(org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.LN1P)
                                        .weight(1.0f)
                        ),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("useCount")
                                        .factor(0.1f)
                                        .modifier(org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.LOG1P)
                                        .weight(1.0f)
                        ),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("likeCount")
                                        .factor(0.05f)
                                        .modifier(org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.LOG1P)
                                        .weight(1.0f)
                        )
                })
                .scoreMode(org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode.SUM)
                .boostMode(CombineFunction.SUM);

        queryBuilder.withQuery(functionScoreQuery);
        queryBuilder.withSorts(SortBuilders.scoreSort().order(SortOrder.DESC),
                SortBuilders.fieldSort("updateTime").order(SortOrder.DESC));

        Pageable pageable = PageRequest.of(dto.getPageNum() - 1, dto.getPageSize());
        queryBuilder.withPageable(pageable);

        NativeSearchQuery query = queryBuilder.build();
        try {
            SearchHits<KnowledgeDocument> searchHits = elasticsearchOperations.search(query, KnowledgeDocument.class);
            List<KnowledgeDocument> docs = searchHits.stream()
                    .map(hit -> hit.getContent())
                    .collect(Collectors.toList());
            return new PageImpl<>(docs, pageable, searchHits.getTotalHits());
        } catch (Exception e) {
            log.error("ES搜索知识库失败", e);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }

    public List<KnowledgeRecommendVO> recommend(KnowledgeRecommendDTO dto) {
        if (!knowledgeIndexService.isEsAvailable()) {
            log.warn("ES不可用，recommend降级返回空");
            return Collections.emptyList();
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        BoolQueryBuilder shouldQuery = QueryBuilders.boolQuery();

        if (StringUtils.hasText(dto.getFaultCode())) {
            String fc = dto.getFaultCode();
            shouldQuery.should(QueryBuilders.termQuery("faultCode", fc).boost(15.0f));
            shouldQuery.should(QueryBuilders.wildcardQuery("faultCode", "*" + fc + "*").boost(5.0f));
        }

        if (StringUtils.hasText(dto.getFaultName())) {
            shouldQuery.should(QueryBuilders.matchQuery("faultName", dto.getFaultName())
                    .analyzer("ik_smart").boost(10.0f));
        }

        if (StringUtils.hasText(dto.getDescription())) {
            BoolQueryBuilder descQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("faultDesc", dto.getDescription())
                            .analyzer("ik_smart").boost(6.0f))
                    .should(QueryBuilders.matchQuery("tags", dto.getDescription())
                            .analyzer("ik_smart").boost(4.0f))
                    .should(QueryBuilders.matchQuery("solution", dto.getDescription())
                            .analyzer("ik_smart").boost(3.0f))
                    .should(QueryBuilders.multiMatchQuery(dto.getDescription(),
                                    "faultName", "faultDesc", "tags", "solution", "solutionRichText")
                            .analyzer("ik_smart")
                            .type(org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.MOST_FIELDS)
                            .boost(2.0f));
            shouldQuery.should(descQuery);
        }

        if (dto.getFaultLevel() != null) {
            int targetLevel = dto.getFaultLevel();
            for (int l = 1; l <= 4; l++) {
                int diff = Math.abs(l - targetLevel);
                float boost = diff == 0 ? 3.0f : (diff == 1 ? 1.5f : (diff == 2 ? 0.5f : 0.1f));
                shouldQuery.should(QueryBuilders.termQuery("faultLevel", l).boost(boost));
            }
        }

        if (CollectionUtils.isEmpty(shouldQuery.shouldClauses())) {
            return Collections.emptyList();
        }

        boolQuery.must(shouldQuery);
        boolQuery.filter(QueryBuilders.termQuery("status", 1));

        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("qualityScore")
                                        .factor(3.0f)
                                        .modifier(org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.LN1P)
                        ),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("useCount")
                                        .factor(0.2f)
                                        .modifier(org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.LOG1P)
                        ),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("likeCount")
                                        .factor(0.1f)
                                        .modifier(org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier.LOG1P)
                        )
                })
                .scoreMode(org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode.SUM)
                .boostMode(CombineFunction.SUM);

        int topN = dto.getTopN() != null ? dto.getTopN() : 5;
        queryBuilder.withQuery(functionScoreQuery)
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, topN));

        NativeSearchQuery query = queryBuilder.build();

        try {
            SearchHits<KnowledgeDocument> searchHits = elasticsearchOperations.search(query, KnowledgeDocument.class);

            if (searchHits.getTotalHits() == 0) {
                return Collections.emptyList();
            }

            float maxScore = searchHits.getMaxScore() != null ? searchHits.getMaxScore() : 1.0f;
            if (maxScore <= 0) maxScore = 1.0f;

            float minConfidence = dto.getMinConfidence() != null ? dto.getMinConfidence().floatValue() : 0.2f;
            final float finalMaxScore = maxScore;

            List<KnowledgeRecommendVO> results = new ArrayList<>();
            searchHits.forEach(hit -> {
                float normalizedScore = hit.getScore() / finalMaxScore;
                if (normalizedScore >= minConfidence) {
                    results.add(convertToRecommendVO(hit.getContent(), normalizedScore, hit.getScore()));
                }
            });

            return results;
        } catch (Exception e) {
            log.error("ES推荐相似案例失败", e);
            return Collections.emptyList();
        }
    }

    private KnowledgeRecommendVO convertToRecommendVO(KnowledgeDocument doc, double normalizedScore, float rawScore) {
        KnowledgeRecommendVO vo = new KnowledgeRecommendVO();
        vo.setId(doc.getId());
        vo.setFaultCode(doc.getFaultCode());
        vo.setFaultName(doc.getFaultName());
        vo.setFaultLevel(doc.getFaultLevel());
        vo.setFaultType(doc.getFaultType());
        vo.setFaultDesc(doc.getFaultDesc());
        vo.setSolution(truncate(doc.getSolution(), 200));
        vo.setSolutionRichText(doc.getSolutionRichText());
        vo.setVideoUrl(doc.getVideoUrl());
        vo.setTags(doc.getTags());
        vo.setConfidence(BigDecimal.valueOf(normalizedScore).setScale(4, RoundingMode.HALF_UP));
        vo.setLikeCount(doc.getLikeCount());
        vo.setDislikeCount(doc.getDislikeCount());
        vo.setUseCount(doc.getUseCount());
        vo.setCreatorName(doc.getCreatorName());
        vo.setUpdateTime(doc.getUpdateTime());

        if (normalizedScore >= 0.8) {
            vo.setConfidenceLevel("high");
        } else if (normalizedScore >= 0.5) {
            vo.setConfidenceLevel("medium");
        } else {
            vo.setConfidenceLevel("low");
        }

        vo.setMatchReason(buildMatchReason(doc, normalizedScore, rawScore));
        return vo;
    }

    private String buildMatchReason(KnowledgeDocument doc, double normScore, float rawScore) {
        List<String> reasons = new ArrayList<>();
        if (normScore >= 0.8) {
            reasons.add("高度匹配");
        } else if (normScore >= 0.5) {
            reasons.add("相关度较高");
        } else {
            reasons.add("智能匹配");
        }
        int likes = doc.getLikeCount() != null ? doc.getLikeCount() : 0;
        if (likes >= 20) {
            reasons.add(likes + "人点赞推荐");
        } else if (likes >= 5) {
            reasons.add(likes + "人认可");
        }
        int uses = doc.getUseCount() != null ? doc.getUseCount() : 0;
        if (uses >= 20) {
            reasons.add("已解决" + uses + "次同类问题");
        } else if (uses >= 3) {
            reasons.add("已被使用" + uses + "次");
        }
        return String.join("，", reasons);
    }

    private String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) return text;
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
