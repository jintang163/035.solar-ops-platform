package com.solar.ops.admin.holder;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

@Data
@Component
@RequestScope
public class LoginUserHolder {

    private Long userId;

    private String username;

    private String role;

    private Integer isAdmin;

    private Long orgId;

    private Integer dataScope;

    private List<Long> stationIds;

    private Long currentStationId;

    public void clear() {
        this.userId = null;
        this.username = null;
        this.role = null;
        this.isAdmin = null;
        this.orgId = null;
        this.dataScope = null;
        this.stationIds = null;
        this.currentStationId = null;
    }
}
