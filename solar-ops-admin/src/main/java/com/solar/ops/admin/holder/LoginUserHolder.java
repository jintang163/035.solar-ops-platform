package com.solar.ops.admin.holder;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
@Component
@RequestScope
public class LoginUserHolder {

    private Long userId;

    private String username;

    private String role;

    public void clear() {
        this.userId = null;
        this.username = null;
        this.role = null;
    }
}
