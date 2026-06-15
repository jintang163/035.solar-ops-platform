package com.solar.ops.analysis.util;

import com.solar.ops.analysis.entity.EfficiencyStatistics;
import com.solar.ops.analysis.enums.DustLevelEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class DustAccumulationCalculator {

    private static final BigDecimal SCALE_HUNDRED = new BigDecimal("100");

    private DustAccumulationCalculator() {
    }

    public static BigDecimal calculateRatio(BigDecimal actualEnergy, BigDecimal theoreticalEnergy) {
        if (actualEnergy == null || theoreticalEnergy == null
                || theoreticalEnergy.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return actualEnergy.divide(theoreticalEnergy, 6, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateAttenuationRate(BigDecimal referenceRatio, BigDecimal detectRatio) {
        if (referenceRatio == null || detectRatio == null
                || referenceRatio.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal attenuation = referenceRatio.subtract(detectRatio)
                .divide(referenceRatio, 6, RoundingMode.HALF_UP);
        return attenuation.max(BigDecimal.ZERO);
    }

    public static BigDecimal calculateAttenuationRatePercentage(BigDecimal attenuationRate) {
        if (attenuationRate == null) {
            return BigDecimal.ZERO;
        }
        return attenuationRate.multiply(SCALE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    public static DustLevelEnum determineDustLevel(BigDecimal attenuationRate) {
        return DustLevelEnum.getByAttenuationRate(attenuationRate);
    }

    public static BigDecimal calculateEstimatedLoss(BigDecimal actualEnergy, BigDecimal attenuationRate) {
        if (actualEnergy == null || attenuationRate == null
                || attenuationRate.compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal denominator = BigDecimal.ONE.subtract(attenuationRate);
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal theoretical = actualEnergy.divide(denominator, 4, RoundingMode.HALF_UP);
        return theoretical.subtract(actualEnergy).setScale(2, RoundingMode.HALF_UP);
    }

    public static int calculateContinuousDeclineDays(List<EfficiencyStatistics> statisticsList, LocalDate detectDate) {
        if (statisticsList == null || statisticsList.isEmpty()) {
            return 0;
        }

        statisticsList.sort(Comparator.comparing(EfficiencyStatistics::getStatisticsDate).reversed());

        int continuousDays = 0;
        LocalDate currentDate = detectDate;
        BigDecimal previousRatio = null;

        for (EfficiencyStatistics stat : statisticsList) {
            if (!stat.getStatisticsDate().isBefore(currentDate.minusDays(14))
                    && stat.getStatisticsDate().isBefore(currentDate.plusDays(1))) {

                if (stat.getPrValue() == null) {
                    continue;
                }

                if (previousRatio == null) {
                    previousRatio = stat.getPrValue();
                    currentDate = stat.getStatisticsDate();
                    continue;
                }

                if (stat.getPrValue().compareTo(previousRatio) <= 0) {
                    continuousDays++;
                    previousRatio = stat.getPrValue();
                    currentDate = stat.getStatisticsDate();
                } else {
                    break;
                }
            }
        }

        return continuousDays;
    }

    public static BigDecimal calculateAverageEnergy(List<EfficiencyStatistics> statisticsList,
                                                     LocalDate startDate, LocalDate endDate) {
        if (statisticsList == null || statisticsList.isEmpty()
                || startDate == null || endDate == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (EfficiencyStatistics stat : statisticsList) {
            if ((stat.getStatisticsDate().isEqual(startDate) || stat.getStatisticsDate().isAfter(startDate))
                    && (stat.getStatisticsDate().isEqual(endDate) || stat.getStatisticsDate().isBefore(endDate))
                    && stat.getTotalEnergy() != null) {
                total = total.add(stat.getTotalEnergy());
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateImprovementRate(BigDecimal beforeEnergy, BigDecimal afterEnergy) {
        if (beforeEnergy == null || afterEnergy == null
                || beforeEnergy.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return afterEnergy.subtract(beforeEnergy)
                .divide(beforeEnergy, 6, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
    }
}
