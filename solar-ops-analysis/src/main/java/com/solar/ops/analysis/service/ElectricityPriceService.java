package com.solar.ops.analysis.service;

import com.solar.ops.analysis.dto.InvestmentInfoDTO;
import com.solar.ops.analysis.dto.PriceSchemeCompareDTO;
import com.solar.ops.analysis.dto.PriceSchemeQueryDTO;
import com.solar.ops.analysis.entity.ElectricityPriceScheme;
import com.solar.ops.analysis.vo.PriceSchemeCompareVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ElectricityPriceService {

    List<ElectricityPriceScheme> list(PriceSchemeQueryDTO query);

    ElectricityPriceScheme getById(Long id);

    boolean save(ElectricityPriceScheme scheme);

    boolean update(ElectricityPriceScheme scheme);

    boolean remove(Long id);

    ElectricityPriceScheme getDefaultScheme(Long stationId);

    BigDecimal calculateEffectivePrice(Long schemeId, LocalDate date);

    List<PriceSchemeCompareVO> compareSchemes(PriceSchemeCompareDTO dto, InvestmentInfoDTO investment);
}
