package com.agri.traceability.service;

import com.agri.traceability.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    Map<String, Object> login(String username, String password);
    boolean register(SysUser user);
}
