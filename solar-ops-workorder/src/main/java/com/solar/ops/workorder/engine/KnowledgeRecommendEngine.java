package com.solar.ops.workorder.engine;

import com.solar.ops.workorder.dto.KnowledgeRecommendDTO;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.vo.KnowledgeRecommendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KnowledgeRecommendEngine {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "和", "是", "就", "都", "而", "及", "与", "或", "一个", "没有", "我们", "你们", "他们",
            "这个", "那个", "这些", "那些", "进行", "通过", "使用", "检查", "是否", "可以", "需要", "正常",
            "情况", "问题", "方法", "步骤", "如果", "然后", "以及", "可能", "应该", "因为", "所以", "但是"
    ));

    public List<KnowledgeRecommendVO> recommend(List<FaultLibrary> allKnowledge, KnowledgeRecommendDTO query) {
        if (CollectionUtils.isEmpty(allKnowledge) || query == null) {
            return Collections.emptyList();
        }

        String queryText = buildQueryText(query);
        if (!StringUtils.hasText(queryText)) {
            if (StringUtils.hasText(query.getFaultCode())) {
                return recommendByFaultCode(allKnowledge, query);
            }
            return Collections.emptyList();
        }

        List<String> queryTerms = tokenize(queryText);
        if (CollectionUtils.isEmpty(queryTerms)) {
            if (StringUtils.hasText(query.getFaultCode())) {
                return recommendByFaultCode(allKnowledge, query);
            }
            return Collections.emptyList();
        }

        Map<String, Double> idfMap = calculateIDF(allKnowledge, queryTerms);
        Map<String, Double> queryTfIdf = calculateTFIDF(queryTerms, idfMap);

        List<KnowledgeRecommendVO> results = new ArrayList<>();
        for (FaultLibrary knowledge : allKnowledge) {
            if (knowledge.getStatus() != null && knowledge.getStatus() != 1) {
                continue;
            }

            String docText = buildDocumentText(knowledge);
            List<String> docTerms = tokenize(docText);

            double codeMatchScore = calculateCodeMatchScore(knowledge.getFaultCode(), query.getFaultCode());
            double levelMatchScore = calculateLevelMatchScore(knowledge.getFaultLevel(), query.getFaultLevel());
            double tfidfSimilarity = 0.0;

            if (!CollectionUtils.isEmpty(docTerms)) {
                Map<String, Double> docTfIdf = calculateTFIDF(docTerms, idfMap);
                tfidfSimilarity = cosineSimilarity(queryTfIdf, docTfIdf);
            }

            double qualityScore = calculateQualityScore(knowledge);
            double finalScore = codeMatchScore * 0.35 + levelMatchScore * 0.10
                    + tfidfSimilarity * 0.40 + qualityScore * 0.15;

            if (finalScore >= (query.getMinConfidence() != null ? query.getMinConfidence() : 0.3)) {
                KnowledgeRecommendVO vo = convertToVO(knowledge, finalScore, codeMatchScore, tfidfSimilarity);
                results.add(vo);
            }
        }

        results.sort((a, b) -> b.getConfidence().compareTo(a.getConfidence()));

        int topN = query.getTopN() != null ? query.getTopN() : 5;
        return results.stream().limit(topN).collect(Collectors.toList());
    }

    private List<KnowledgeRecommendVO> recommendByFaultCode(List<FaultLibrary> allKnowledge, KnowledgeRecommendDTO query) {
        List<KnowledgeRecommendVO> results = new ArrayList<>();
        String targetCode = query.getFaultCode();

        for (FaultLibrary knowledge : allKnowledge) {
            if (knowledge.getStatus() != null && knowledge.getStatus() != 1) {
                continue;
            }

            double score = 0.0;
            if (targetCode != null && knowledge.getFaultCode() != null) {
                if (knowledge.getFaultCode().equalsIgnoreCase(targetCode)) {
                    score = 0.95;
                } else if (knowledge.getFaultCode().contains(targetCode) || targetCode.contains(knowledge.getFaultCode())) {
                    score = 0.75;
                }
            }

            if (score > 0) {
                score = score * 0.85 + calculateQualityScore(knowledge) * 0.15;
                if (score >= (query.getMinConfidence() != null ? query.getMinConfidence() : 0.3)) {
                    KnowledgeRecommendVO vo = convertToVO(knowledge, score, score, 0);
                    results.add(vo);
                }
            }
        }

        results.sort((a, b) -> b.getConfidence().compareTo(a.getConfidence()));
        int topN = query.getTopN() != null ? query.getTopN() : 5;
        return results.stream().limit(topN).collect(Collectors.toList());
    }

    private String buildQueryText(KnowledgeRecommendDTO query) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(query.getFaultName())) {
            sb.append(query.getFaultName()).append(" ");
        }
        if (StringUtils.hasText(query.getDescription())) {
            sb.append(query.getDescription()).append(" ");
        }
        if (StringUtils.hasText(query.getFaultCode())) {
            sb.append(query.getFaultCode()).append(" ");
        }
        return sb.toString().trim();
    }

    private String buildDocumentText(FaultLibrary doc) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(doc.getFaultName())) {
            sb.append(doc.getFaultName()).append(" ");
        }
        if (StringUtils.hasText(doc.getFaultCode())) {
            sb.append(doc.getFaultCode()).append(" ");
        }
        if (StringUtils.hasText(doc.getFaultDesc())) {
            sb.append(doc.getFaultDesc()).append(" ");
        }
        if (StringUtils.hasText(doc.getFaultType())) {
            sb.append(doc.getFaultType()).append(" ");
        }
        if (StringUtils.hasText(doc.getTags())) {
            sb.append(doc.getTags().replace(",", " ")).append(" ");
        }
        if (StringUtils.hasText(doc.getSolution())) {
            sb.append(doc.getSolution()).append(" ");
        }
        return sb.toString().trim();
    }

    public List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        text = text.toLowerCase();

        StringBuilder currentToken = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                currentToken.append(c);
            } else {
                if (currentToken.length() > 0) {
                    addToken(tokens, currentToken.toString());
                    currentToken = new StringBuilder();
                }
            }
        }
        if (currentToken.length() > 0) {
            addToken(tokens, currentToken.toString());
        }

        for (int i = 0; i < text.length() - 1; i++) {
            char c1 = text.charAt(i);
            char c2 = text.charAt(i + 1);
            if (isChinese(c1) && isChinese(c2)) {
                String bigram = String.valueOf(c1) + c2;
                if (!STOP_WORDS.contains(bigram)) {
                    tokens.add(bigram);
                }
            }
        }

        return tokens;
    }

    private void addToken(List<String> tokens, String token) {
        if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
            tokens.add(token);
        }
    }

    private boolean isChinese(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
    }

    private Map<String, Double> calculateIDF(List<FaultLibrary> documents, Collection<String> terms) {
        Map<String, Double> idfMap = new HashMap<>();
        int docCount = documents.size();

        for (String term : terms) {
            int containCount = 0;
            for (FaultLibrary doc : documents) {
                String docText = buildDocumentText(doc).toLowerCase();
                if (docText.contains(term.toLowerCase())) {
                    containCount++;
                }
            }
            double idf = Math.log((double) (docCount + 1) / (containCount + 1)) + 1.0;
            idfMap.put(term, idf);
        }

        return idfMap;
    }

    private Map<String, Double> calculateTFIDF(List<String> tokens, Map<String, Double> idfMap) {
        Map<String, Integer> tfMap = new HashMap<>();
        for (String token : tokens) {
            tfMap.merge(token, 1, Integer::sum);
        }

        int totalTokens = tokens.size();
        Map<String, Double> tfidfMap = new HashMap<>();

        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String term = entry.getKey();
            double tf = (double) entry.getValue() / totalTokens;
            double idf = idfMap.getOrDefault(term, 1.0);
            tfidfMap.put(term, tf * idf);
        }

        return tfidfMap;
    }

    private double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vec1.entrySet()) {
            String term = entry.getKey();
            if (vec2.containsKey(term)) {
                dotProduct += entry.getValue() * vec2.get(term);
            }
        }

        double norm1 = 0.0;
        for (double v : vec1.values()) {
            norm1 += v * v;
        }
        norm1 = Math.sqrt(norm1);

        double norm2 = 0.0;
        for (double v : vec2.values()) {
            norm2 += v * v;
        }
        norm2 = Math.sqrt(norm2);

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    private double calculateCodeMatchScore(String code1, String code2) {
        if (!StringUtils.hasText(code1) || !StringUtils.hasText(code2)) {
            return 0.0;
        }
        if (code1.equalsIgnoreCase(code2)) {
            return 1.0;
        }
        if (code1.contains(code2) || code2.contains(code1)) {
            return 0.6;
        }
        String[] parts1 = code1.split("_");
        String[] parts2 = code2.split("_");
        int matchCount = 0;
        for (String p1 : parts1) {
            for (String p2 : parts2) {
                if (p1.equalsIgnoreCase(p2)) {
                    matchCount++;
                }
            }
        }
        if (matchCount > 0) {
            return (double) matchCount / Math.max(parts1.length, parts2.length) * 0.4;
        }
        return 0.0;
    }

    private double calculateLevelMatchScore(Integer level1, Integer level2) {
        if (level1 == null || level2 == null) {
            return 0.3;
        }
        if (level1.equals(level2)) {
            return 1.0;
        }
        int diff = Math.abs(level1 - level2);
        switch (diff) {
            case 1:
                return 0.7;
            case 2:
                return 0.4;
            default:
                return 0.1;
        }
    }

    private double calculateQualityScore(FaultLibrary knowledge) {
        int likes = knowledge.getLikeCount() != null ? knowledge.getLikeCount() : 0;
        int dislikes = knowledge.getDislikeCount() != null ? knowledge.getDislikeCount() : 0;
        int uses = knowledge.getUseCount() != null ? knowledge.getUseCount() : 0;
        int views = knowledge.getViewCount() != null ? knowledge.getViewCount() : 0;

        double ratingScore = 0.5;
        if (likes + dislikes > 0) {
            ratingScore = (double) likes / (likes + dislikes);
            ratingScore = 0.3 + 0.7 * ratingScore;
        }

        double usageScore = Math.min(1.0, uses / 10.0);
        double contentScore = 0.5;
        if (StringUtils.hasText(knowledge.getSolutionRichText())) {
            contentScore += 0.25;
        }
        if (StringUtils.hasText(knowledge.getTags())) {
            contentScore += 0.15;
        }
        if (StringUtils.hasText(knowledge.getVideoUrl()) || StringUtils.hasText(knowledge.getAttachments())) {
            contentScore += 0.1;
        }

        return ratingScore * 0.4 + usageScore * 0.3 + contentScore * 0.3;
    }

    private KnowledgeRecommendVO convertToVO(FaultLibrary knowledge, double score,
                                             double codeScore, double textScore) {
        KnowledgeRecommendVO vo = new KnowledgeRecommendVO();
        vo.setId(knowledge.getId());
        vo.setFaultCode(knowledge.getFaultCode());
        vo.setFaultName(knowledge.getFaultName());
        vo.setFaultLevel(knowledge.getFaultLevel());
        vo.setFaultType(knowledge.getFaultType());
        vo.setFaultDesc(knowledge.getFaultDesc());
        vo.setSolution(truncateText(knowledge.getSolution(), 200));
        vo.setSolutionRichText(knowledge.getSolutionRichText());
        vo.setVideoUrl(knowledge.getVideoUrl());
        vo.setTags(knowledge.getTags());
        vo.setConfidence(BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP));
        vo.setLikeCount(knowledge.getLikeCount());
        vo.setDislikeCount(knowledge.getDislikeCount());
        vo.setUseCount(knowledge.getUseCount());
        vo.setCreatorName(knowledge.getCreatorName());
        vo.setUpdateTime(knowledge.getUpdateTime());

        if (score >= 0.8) {
            vo.setConfidenceLevel("high");
        } else if (score >= 0.5) {
            vo.setConfidenceLevel("medium");
        } else {
            vo.setConfidenceLevel("low");
        }

        vo.setMatchReason(buildMatchReason(codeScore, textScore, knowledge));
        return vo;
    }

    private String buildMatchReason(double codeScore, double textScore, FaultLibrary knowledge) {
        List<String> reasons = new ArrayList<>();
        if (codeScore >= 0.9) {
            reasons.add("故障码完全匹配");
        } else if (codeScore >= 0.5) {
            reasons.add("故障码部分匹配");
        }
        if (textScore >= 0.6) {
            reasons.add("故障描述高度相关");
        } else if (textScore >= 0.3) {
            reasons.add("故障描述相关");
        }
        int likes = knowledge.getLikeCount() != null ? knowledge.getLikeCount() : 0;
        if (likes >= 10) {
            reasons.add(likes + "人点赞推荐");
        }
        int uses = knowledge.getUseCount() != null ? knowledge.getUseCount() : 0;
        if (uses >= 5) {
            reasons.add("已被使用" + uses + "次");
        }
        if (reasons.isEmpty()) {
            reasons.add("系统智能匹配");
        }
        return String.join("，", reasons);
    }

    private String truncateText(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
