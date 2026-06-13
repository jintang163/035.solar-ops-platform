package com.solar.ops.admin.interceptor;

import com.solar.ops.admin.holder.LoginUserHolder;
import com.solar.ops.admin.util.JwtTokenUtil;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private LoginUserHolder loginUserHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(header);
        if (authHeader == null || !authHeader.startsWith(tokenPrefix)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(tokenPrefix.length());
        if (!jwtTokenUtil.validateToken(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String role = jwtTokenUtil.getRoleFromToken(token);

        loginUserHolder.setUserId(userId);
        loginUserHolder.setUsername(username);
        loginUserHolder.setRole(role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        loginUserHolder.clear();
    }
}
