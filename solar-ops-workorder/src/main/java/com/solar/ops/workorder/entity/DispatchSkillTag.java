package com.solar.ops.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.solar.ops.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_skill_tag")
public class DispatchSkillTag extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String tagName;

    private String tagCode;

    private String category;

    private Integer sort;

    private Integer status;
}
