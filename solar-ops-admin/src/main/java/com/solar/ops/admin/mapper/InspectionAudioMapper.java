package com.solar.ops.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solar.ops.admin.entity.InspectionAudio;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InspectionAudioMapper extends BaseMapper<InspectionAudio> {

    List<InspectionAudio> selectByResultId(@Param("resultId") Long resultId);

    List<InspectionAudio> selectByResultItemId(@Param("resultItemId") Long resultItemId);

    int batchInsert(@Param("list") List<InspectionAudio> list);
}
