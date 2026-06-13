package com.solar.ops.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.solar.ops.admin.dto.LoginDTO;
import com.solar.ops.admin.entity.SysUser;
import com.solar.ops.admin.mapper.SysUserMapper;
import com.solar.ops.admin.util.JwtTokenUtil;
import com.solar.ops.admin.vo.LoginVO;
import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.page.PageQuery;
import com.solar.ops.common.page.PageResult;
import com.solar.ops.common.result.ResultCode;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Service
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername()));

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(tokenPrefix + " " + token);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setRole(user.getRole());
        return loginVO;
    }

    public PageResult<SysUser> page(PageQuery pageQuery, String keyword) {
        Page<SysUser> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        page(page, wrapper);

        page.getRecords().forEach(user -> user.setPassword(null));

        return PageResult.build(page.getTotal(), page.getRecords(), pageQuery.getPageNum(), pageQuery.getPageSize());
    }

    public void addUser(SysUser user) {
        SysUser existUser = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        user.setPassword(BCrypt.hashpw(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
    }

    public void updateUser(SysUser user) {
        if (user.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        SysUser existUser = getById(user.getId());
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(existUser.getUsername())) {
            SysUser sameNameUser = getOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, user.getUsername()));
            if (sameNameUser != null) {
                throw new BusinessException("用户名已存在");
            }
        }

        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }

        updateById(user);
    }

    public void deleteUser(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        removeById(id);
    }

    public SysUser getUserById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        SysUser user = getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
