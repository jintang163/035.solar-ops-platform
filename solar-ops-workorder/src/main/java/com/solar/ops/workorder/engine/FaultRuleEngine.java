package com.solar.ops.workorder.engine;

import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.workorder.entity.FaultLibrary;
import com.solar.ops.workorder.enums.FaultLevelEnum;
import com.solar.ops.workorder.service.FaultLibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaultRuleEngine {

    private final FaultLibraryService faultLibraryService;

    public FaultMatchResult match(String faultCode) {
        if (faultCode == null || faultCode.trim().isEmpty()) {
            throw new BusinessException("故障码不能为空");
        }

        FaultLibrary faultLibrary = faultLibraryService.getByFaultCode(faultCode);
        if (faultLibrary == null) {
            log.warn("未匹配到故障码: {}", faultCode);
            return buildDefaultResult(faultCode);
        }

        FaultMatchResult result = new FaultMatchResult();
        result.setFaultCode(faultLibrary.getFaultCode());
        result.setFaultName(faultLibrary.getFaultName());
        result.setFaultLevel(faultLibrary.getFaultLevel());
        result.setFaultDesc(faultLibrary.getFaultDesc());
        result.setSolution(faultLibrary.getSolution());
        result.setMatched(true);
        result.setExpectHours(calculateExpectHours(faultLibrary.getFaultLevel()));

        return result;
    }

    public FaultMatchResult matchByEfficiency(Double efficiency, Double threshold) {
        if (efficiency == null || threshold == null) {
            return null;
        }

        if (efficiency >= threshold) {
            return null;
        }

        double ratio = efficiency / threshold;
        FaultLevelEnum level;
        String faultCode;
        String faultName;
        String solution;

        if (ratio < 0.3) {
            level = FaultLevelEnum.CRITICAL;
            faultCode = "EFF_CRITICAL";
            faultName = "效率严重偏低";
            solution = "立即停机检查，全面排查逆变器、组件、汇流箱等设备";
        } else if (ratio < 0.5) {
            level = FaultLevelEnum.HIGH;
            faultCode = "EFF_HIGH";
            faultName = "效率大幅偏低";
            solution = "24小时内安排现场检查，重点排查组件遮挡和逆变器故障";
        } else if (ratio < 0.8) {
            level = FaultLevelEnum.MEDIUM;
            faultCode = "EFF_MEDIUM";
            faultName = "效率中度偏低";
            solution = "48小时内安排检查，排查组件清洁度和接线问题";
        } else {
            level = FaultLevelEnum.LOW;
            faultCode = "EFF_LOW";
            faultName = "效率轻度偏低";
            solution = "持续观察，定期巡检时重点关注";
        }

        FaultMatchResult result = new FaultMatchResult();
        result.setFaultCode(faultCode);
        result.setFaultName(faultName);
        result.setFaultLevel(level.getCode());
        result.setFaultDesc("电站效率低于阈值，当前效率: " + efficiency + "%, 阈值: " + threshold + "%");
        result.setSolution(solution);
        result.setMatched(true);
        result.setExpectHours(calculateExpectHours(level.getCode()));

        return result;
    }

    private int calculateExpectHours(Integer faultLevel) {
        if (faultLevel == null) {
            return 72;
        }
        FaultLevelEnum level = FaultLevelEnum.getByCode(faultLevel);
        if (level == null) {
            return 72;
        }
        switch (level) {
            case CRITICAL:
                return 2;
            case HIGH:
                return 24;
            case MEDIUM:
                return 48;
            case LOW:
            default:
                return 72;
        }
    }

    private FaultMatchResult buildDefaultResult(String faultCode) {
        FaultMatchResult result = new FaultMatchResult();
        result.setFaultCode(faultCode);
        result.setFaultName("未知故障");
        result.setFaultLevel(FaultLevelEnum.LOW.getCode());
        result.setFaultDesc("未匹配到故障库记录，请人工确认");
        result.setSolution("请联系技术支持人员处理");
        result.setMatched(false);
        result.setExpectHours(72);
        return result;
    }

    public static class FaultMatchResult {
        private String faultCode;
        private String faultName;
        private Integer faultLevel;
        private String faultDesc;
        private String solution;
        private boolean matched;
        private Integer expectHours;

        public String getFaultCode() {
            return faultCode;
        }

        public void setFaultCode(String faultCode) {
            this.faultCode = faultCode;
        }

        public String getFaultName() {
            return faultName;
        }

        public void setFaultName(String faultName) {
            this.faultName = faultName;
        }

        public Integer getFaultLevel() {
            return faultLevel;
        }

        public void setFaultLevel(Integer faultLevel) {
            this.faultLevel = faultLevel;
        }

        public String getFaultDesc() {
            return faultDesc;
        }

        public void setFaultDesc(String faultDesc) {
            this.faultDesc = faultDesc;
        }

        public String getSolution() {
            return solution;
        }

        public void setSolution(String solution) {
            this.solution = solution;
        }

        public boolean isMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }

        public Integer getExpectHours() {
            return expectHours;
        }

        public void setExpectHours(Integer expectHours) {
            this.expectHours = expectHours;
        }
    }
}
