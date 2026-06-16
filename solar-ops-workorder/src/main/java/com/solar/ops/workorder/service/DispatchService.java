package com.solar.ops.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solar.ops.workorder.entity.DispatchInverter;
import com.solar.ops.workorder.entity.DispatchStation;
import com.solar.ops.workorder.entity.DispatchUser;
import com.solar.ops.workorder.entity.OperatorLocation;
import com.solar.ops.workorder.entity.WorkOrder;
import com.solar.ops.workorder.mapper.DispatchInverterMapper;
import com.solar.ops.workorder.mapper.DispatchSkillTagMapper;
import com.solar.ops.workorder.mapper.DispatchStationMapper;
import com.solar.ops.workorder.mapper.DispatchUserMapper;
import com.solar.ops.workorder.mapper.OperatorLocationMapper;
import com.solar.ops.workorder.mapper.WorkOrderMapper;
import com.solar.ops.workorder.vo.OperatorRecommendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final OperatorLocationMapper operatorLocationMapper;
    private final DispatchUserMapper dispatchUserMapper;
    private final WorkOrderMapper workOrderMapper;
    private final DispatchStationMapper dispatchStationMapper;
    private final DispatchInverterMapper dispatchInverterMapper;
    private final DispatchSkillTagMapper dispatchSkillTagMapper;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int MAX_RECOMMEND_COUNT = 10;
    private static final double DEFAULT_SEARCH_RADIUS_KM = 50.0;
    private static final double AVG_SPEED_KMH = 30.0;
    private static final long LOCATION_EXPIRE_MINUTES = 30;

    public List<OperatorRecommendVO> recommendByOrder(Long orderId, Long stationId, BigDecimal stationLng,
                                                     BigDecimal stationLat, String requiredSkill, Integer faultLevel) {
        BigDecimal lng = stationLng;
        BigDecimal lat = stationLat;

        if ((lng == null || lat == null) && orderId != null) {
            WorkOrder order = workOrderMapper.selectById(orderId);
            if (order != null) {
                BigDecimal[] coords = resolveLocation(order);
                if (coords != null && coords[0] != null && coords[1] != null) {
                    lng = coords[0];
                    lat = coords[1];
                }
                if (stationId == null) {
                    stationId = order.getStationId();
                }
                if (!StringUtils.hasText(requiredSkill)) {
                    requiredSkill = order.getFaultName();
                }
                if (faultLevel == null) {
                    faultLevel = order.getFaultLevel();
                }
            }
        }

        if (lng == null || lat == null) {
            log.warn("无法获取有效经纬度, orderId: {}, stationId: {}", orderId, stationId);
            return Collections.emptyList();
        }

        return recommendOperators(stationId, lng, lat, requiredSkill, faultLevel);
    }

    public BigDecimal[] resolveLocation(WorkOrder order) {
        if (order == null) {
            return null;
        }

        if (order.getInverterId() != null) {
            DispatchInverter inverter = dispatchInverterMapper.selectById(order.getInverterId());
            if (inverter != null && inverter.getLongitude() != null && inverter.getLatitude() != null) {
                return new BigDecimal[]{inverter.getLongitude(), inverter.getLatitude()};
            }
        }

        if (order.getStationId() != null) {
            DispatchStation station = dispatchStationMapper.selectById(order.getStationId());
            if (station != null && station.getLongitude() != null && station.getLatitude() != null) {
                return new BigDecimal[]{station.getLongitude(), station.getLatitude()};
            }
        }

        return null;
    }

    public List<OperatorRecommendVO> recommendOperators(Long stationId, BigDecimal stationLng, BigDecimal stationLat,
                                                        String requiredSkill, Integer faultLevel) {
        if (stationLng == null || stationLat == null) {
            return Collections.emptyList();
        }

        List<OperatorLocation> locations = operatorLocationMapper.selectWithinRadius(
                stationLng, stationLat, DEFAULT_SEARCH_RADIUS_KM);

        if (CollectionUtils.isEmpty(locations)) {
            log.warn("搜索半径内无运维人员位置, stationId: {}, radius: {}km", stationId, DEFAULT_SEARCH_RADIUS_KM);
            return Collections.emptyList();
        }

        Set<Long> userIds = locations.stream()
                .map(OperatorLocation::getUserId)
                .collect(Collectors.toSet());

        List<DispatchUser> users = dispatchUserMapper.selectBatchIds(userIds);
        Map<Long, DispatchUser> userMap = users.stream()
                .collect(Collectors.toMap(DispatchUser::getId, u -> u));

        Map<Long, Integer> taskCountMap = getActiveTaskCountMap(userIds);

        Map<Long, OperatorLocation> locationMap = locations.stream()
                .collect(Collectors.toMap(OperatorLocation::getUserId, loc -> loc,
                        (loc1, loc2) -> loc1.getCreateTime().isAfter(loc2.getCreateTime()) ? loc1 : loc2));

        List<OperatorRecommendVO> recommendList = new ArrayList<>();

        for (Map.Entry<Long, OperatorLocation> entry : locationMap.entrySet()) {
            Long userId = entry.getKey();
            OperatorLocation location = entry.getValue();
            DispatchUser user = userMap.get(userId);

            if (user == null || user.getStatus() == null || user.getStatus() == 0) {
                continue;
            }

            OperatorRecommendVO vo = new OperatorRecommendVO();
            vo.setUserId(userId);
            vo.setUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            vo.setPhone(user.getPhone());
            vo.setLongitude(location.getLongitude());
            vo.setLatitude(location.getLatitude());
            vo.setLastReportTime(location.getCreateTime() != null ? location.getCreateTime().toString() : "");

            double distance = calculateDistance(stationLat.doubleValue(), stationLng.doubleValue(),
                    location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
            vo.setDistanceKm(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));

            List<String> skillTags = getUserSkillTags(userId);
            vo.setSkillTags(skillTags);

            int skillScore = calculateSkillScore(skillTags, requiredSkill);
            vo.setSkillMatchScore(skillScore);

            int activeCount = taskCountMap.getOrDefault(userId, 0);
            vo.setActiveTaskCount(activeCount);
            int workloadScore = calculateWorkloadScore(activeCount, faultLevel);
            vo.setWorkloadScore(workloadScore);

            int distanceScore = calculateDistanceScore(distance);

            int totalScore = calculateTotalScore(skillScore, workloadScore, distanceScore, faultLevel);
            vo.setTotalScore(totalScore);

            vo.setRecommendLevel(getRecommendLevel(totalScore));

            int etaMinutes = (int) Math.ceil((distance / AVG_SPEED_KMH) * 60);
            vo.setEtaMinutes(etaMinutes);

            recommendList.add(vo);
        }

        recommendList.sort((a, b) -> b.getTotalScore() - a.getTotalScore());

        return recommendList.stream()
                .limit(MAX_RECOMMEND_COUNT)
                .collect(Collectors.toList());
    }

    private List<String> getUserSkillTags(Long userId) {
        List<String> tags = new ArrayList<>();

        try {
            List<String> skillTags = dispatchSkillTagMapper.selectTagNamesByUserId(userId);
            if (skillTags != null && !skillTags.isEmpty()) {
                tags.addAll(skillTags);
            }
        } catch (Exception e) {
            log.debug("从技能标签表获取失败，尝试从role字段解析", e);
        }

        if (tags.isEmpty()) {
            try {
                DispatchUser user = dispatchUserMapper.selectById(userId);
                if (user != null && StringUtils.hasText(user.getRole())) {
                    String role = user.getRole();
                    String[] parts = role.split(",|，|;|；");
                    for (String part : parts) {
                        String tag = part.trim();
                        if (StringUtils.hasText(tag)) {
                            tags.add(tag);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("从role字段解析技能标签异常", e);
            }
        }

        if (tags.isEmpty()) {
            tags.add("常规运维");
        }
        return tags;
    }

    private Map<Long, Integer> getActiveTaskCountMap(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WorkOrder::getHandlerId, userIds);
        wrapper.in(WorkOrder::getStatus, 1, 2);

        List<WorkOrder> activeOrders = workOrderMapper.selectList(wrapper);

        Map<Long, Integer> countMap = new HashMap<>();
        for (WorkOrder order : activeOrders) {
            if (order.getHandlerId() != null) {
                countMap.merge(order.getHandlerId(), 1, Integer::sum);
            }
        }
        return countMap;
    }

    private int calculateSkillScore(List<String> skillTags, String requiredSkill) {
        if (!StringUtils.hasText(requiredSkill)) {
            return 80;
        }
        if (CollectionUtils.isEmpty(skillTags)) {
            return 30;
        }

        String requiredLower = requiredSkill.toLowerCase();

        for (String skill : skillTags) {
            if (skill.toLowerCase().contains(requiredLower) || requiredLower.contains(skill.toLowerCase())) {
                return 100;
            }
        }

        if (skillTags.contains("常规运维") || skillTags.contains("运维")) {
            return 60;
        }

        return 30;
    }

    private int calculateWorkloadScore(int activeTaskCount, Integer faultLevel) {
        int baseScore = 100;

        int penalty = activeTaskCount * 15;

        if (faultLevel != null && faultLevel >= 3) {
            penalty = (int) (penalty * 1.2);
        }

        return Math.max(0, baseScore - penalty);
    }

    private int calculateDistanceScore(double distanceKm) {
        if (distanceKm <= 2) {
            return 100;
        }
        if (distanceKm <= 5) {
            return 90;
        }
        if (distanceKm <= 10) {
            return 75;
        }
        if (distanceKm <= 20) {
            return 55;
        }
        if (distanceKm <= 30) {
            return 35;
        }
        if (distanceKm <= 50) {
            return 20;
        }
        return 10;
    }

    private int calculateTotalScore(int skillScore, int workloadScore, int distanceScore, Integer faultLevel) {
        double distanceWeight = 0.35;
        double skillWeight = 0.35;
        double workloadWeight = 0.30;

        if (faultLevel != null && faultLevel >= 3) {
            distanceWeight = 0.45;
            skillWeight = 0.35;
            workloadWeight = 0.20;
        }
        if (faultLevel != null && faultLevel >= 4) {
            distanceWeight = 0.50;
            skillWeight = 0.30;
            workloadWeight = 0.20;
        }

        double total = skillScore * skillWeight
                + workloadScore * workloadWeight
                + distanceScore * distanceWeight;

        return (int) Math.round(total);
    }

    private String getRecommendLevel(int totalScore) {
        if (totalScore >= 85) {
            return "强烈推荐";
        }
        if (totalScore >= 70) {
            return "推荐";
        }
        if (totalScore >= 55) {
            return "一般";
        }
        if (totalScore >= 40) {
            return "可考虑";
        }
        return "不推荐";
    }

    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public boolean reportLocation(Long userId, String userName, BigDecimal longitude, BigDecimal latitude,
                                   BigDecimal accuracy, BigDecimal speed, BigDecimal heading, String locationType) {
        try {
            OperatorLocation location = new OperatorLocation();
            location.setUserId(userId);
            location.setUserName(userName);
            location.setLongitude(longitude);
            location.setLatitude(latitude);
            location.setAccuracy(accuracy);
            location.setSpeed(speed);
            location.setHeading(heading);
            location.setLocationType(locationType);
            location.setReportTime(System.currentTimeMillis());

            operatorLocationMapper.insert(location);
            return true;
        } catch (Exception e) {
            log.error("位置上报失败, userId: {}", userId, e);
            return false;
        }
    }

    public OperatorLocation getLatestLocation(Long userId) {
        return operatorLocationMapper.getLatestByUserId(userId);
    }

    public List<OperatorLocation> getAllLatestLocations(Long stationId) {
        List<OperatorLocation> allLocations = operatorLocationMapper.selectList(
                new LambdaQueryWrapper<OperatorLocation>()
                        .eq(OperatorLocation::getDeleted, 0)
                        .orderByDesc(OperatorLocation::getCreateTime)
        );

        Map<Long, OperatorLocation> latestMap = new HashMap<>();
        for (OperatorLocation loc : allLocations) {
            if (!latestMap.containsKey(loc.getUserId())) {
                latestMap.put(loc.getUserId(), loc);
            }
        }

        return new ArrayList<>(latestMap.values());
    }
}
