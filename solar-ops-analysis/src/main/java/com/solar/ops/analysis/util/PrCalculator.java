package com.solar.ops.analysis.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PrCalculator {

    private static final int SCALE = 4;

    public static BigDecimal calculatePr(BigDecimal actualEnergy, BigDecimal installedCapacity, BigDecimal peakSunHours) {
        if (installedCapacity == null || installedCapacity.compareTo(BigDecimal.ZERO) <= 0
                || peakSunHours == null || peakSunHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (actualEnergy == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal theoreticalEnergy = installedCapacity.multiply(peakSunHours);
        return actualEnergy.divide(theoreticalEnergy, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateSystemEfficiency(BigDecimal actualEnergy, BigDecimal installedCapacity, BigDecimal irradiance) {
        if (installedCapacity == null || installedCapacity.compareTo(BigDecimal.ZERO) <= 0
                || irradiance == null || irradiance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (actualEnergy == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal theoreticalEnergy = installedCapacity.multiply(irradiance).divide(new BigDecimal("1000"), SCALE, RoundingMode.HALF_UP);
        return actualEnergy.divide(theoreticalEnergy, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateEquivalentHours(BigDecimal totalEnergy, BigDecimal installedCapacity) {
        if (installedCapacity == null || installedCapacity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (totalEnergy == null) {
            return BigDecimal.ZERO;
        }
        return totalEnergy.divide(installedCapacity, SCALE, RoundingMode.HALF_UP);
    }

    public static int getHealthLevel(BigDecimal prValue) {
        if (prValue == null) {
            return 3;
        }
        if (prValue.compareTo(new BigDecimal("0.85")) >= 0) {
            return 1;
        } else if (prValue.compareTo(new BigDecimal("0.75")) >= 0) {
            return 2;
        } else {
            return 3;
        }
    }

    public static String getHealthLevelDesc(int healthLevel) {
        switch (healthLevel) {
            case 1:
                return "优秀";
            case 2:
                return "良好";
            case 3:
                return "差";
            default:
                return "未知";
        }
    }

    public static String getHealthColor(int healthLevel) {
        switch (healthLevel) {
            case 1:
                return "green";
            case 2:
                return "yellow";
            case 3:
                return "red";
            default:
                return "gray";
        }
    }

    public static BigDecimal calculateEfficiencyScore(BigDecimal prValue, Integer faultCount) {
        if (prValue == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal prScore = prValue.multiply(new BigDecimal("100"));
        BigDecimal faultPenalty = BigDecimal.ZERO;
        if (faultCount != null && faultCount > 0) {
            faultPenalty = new BigDecimal(faultCount).multiply(new BigDecimal("5"));
        }
        BigDecimal score = prScore.subtract(faultPenalty);
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100");
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }
}
