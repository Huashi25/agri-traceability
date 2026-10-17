package com.agri.traceability.service.impl;

import com.agri.traceability.common.JwtUtil;
import com.agri.traceability.entity.SysUser;
import com.agri.traceability.mapper.SysUserMapper;
import com.agri.traceability.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Map<String, Object> login(String username, String password) {
        log.info("用户登录: username={}", username);
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        SysUser user = getOne(wrapper);
        if (user == null) {
            log.warn("登录失败: 用户不存在, username={}", username);
            return null;
        }
        if (user.getStatus() != 1) {
            log.warn("登录失败: 用户已禁用, username={}", username);
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败: 密码错误, username={}", username);
            return null;
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        user.setPassword(null);
        result.put("user", user);
        log.info("登录成功: username={}, role={}", username, user.getRole());
        return result;
    }

    @Override
    public boolean register(SysUser user) {
        log.info("用户注册: username={}, role={}", user.getUsername(), user.getRole());
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        if (count(wrapper) > 0) {
            log.warn("注册失败: 用户名已存在, username={}", user.getUsername());
            return false;
        }
        if (user.getRole() == null) user.setRole("CONSUMER");
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        boolean success = save(user);
        if (success) {
            log.info("注册成功: userId={}, username={}", user.getId(), user.getUsername());
        } else {
            log.error("注册失败: username={}", user.getUsername());
        }
        return success;
    }
}
