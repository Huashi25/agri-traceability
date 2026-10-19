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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 用户服务实现：登录、注册，以及用户信息的保存与更新。
 *
 * 安全约定（2026-10-19 加固）：
 *   1. 所有写入数据库的密码必须是 BCrypt 密文，绝不允许明文落库；
 *   2. 密码长度统一限制为 6~20 位，校验收在本服务内，任何入口都绕不过；
 *   3. updateById 时密码为 null 或空串表示"不修改密码"，不会把密码清空；
 *   4. 已加密的密文（$2a$ 开头）不会被二次加密。
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /** 密码最小长度 */
    private static final int PASSWORD_MIN_LEN = 6;
    /** 密码最大长度 */
    private static final int PASSWORD_MAX_LEN = 20;
    /** BCrypt 密文前缀，用于判断密码是否已加密 */
    private static final String BCRYPT_PREFIX = "$2a$";

    /**
     * 允许"自助注册"的角色白名单。
     *
     * 安全背景（2026-10-19 加固）：注册接口是公开的（不走 JWT），
     * 曾经直接把请求体里的 role 写进数据库 —— 任何人 POST role=ADMIN 就能拿到管理员账号。
     * 因此这里必须白名单限定：ADMIN / MERCHANT 只能由管理员在后台创建，不允许自助注册。
     */
    private static final Set<String> SELF_REGISTER_ROLES = new HashSet<>(Arrays.asList("CONSUMER", "FARMER"));

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
        // 注册入口必须先过密码强度校验，长度不合规直接抛错（由 Controller 转成提示）
        validatePassword(user.getPassword());

        // 角色白名单：不传默认普通客户；传了就必须在允许范围内，防止伪造 ADMIN/MERCHANT
        String role = user.getRole();
        if (role == null || role.trim().isEmpty()) {
            user.setRole("CONSUMER");
        } else {
            String normalized = role.trim().toUpperCase();
            if (!SELF_REGISTER_ROLES.contains(normalized)) {
                log.warn("注册被拒绝: 非法角色 username={}, role={}", user.getUsername(), role);
                throw new IllegalArgumentException("注册身份不合法");
            }
            user.setRole(normalized);
        }

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", user.getUsername());
        if (count(wrapper) > 0) {
            log.warn("注册失败: 用户名已存在, username={}", user.getUsername());
            return false;
        }
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

    /**
     * 重写保存/更新：管理员新增或修改用户时统一做密码校验与加密。
     * 新增场景密码必填；已有密文不会被二次加密。
     */
    @Override
    public boolean saveOrUpdate(SysUser user) {
        String pwd = user.getPassword();
        if (pwd == null || pwd.isEmpty()) {
            // 新增用户必须设置密码；此处不区分新增/修改，交由调用方保证修改时置空
            throw new IllegalArgumentException("密码不能为空");
        }
        if (!isEncoded(pwd)) {
            validatePassword(pwd);
            user.setPassword(passwordEncoder.encode(pwd));
        }
        return super.saveOrUpdate(user);
    }

    /**
     * 重写按 ID 更新：
     *   - 密码为 null 或空串 → 视为"不修改密码"，置为 null（MyBatis-Plus 默认不更新 null 字段）；
     *   - 密码为明文 → 校验长度后加密；
     *   - 密码已是 BCrypt 密文 → 原样保留，避免 PasswordMigrationRunner 等场景被二次加密。
     */
    @Override
    public boolean updateById(SysUser user) {
        String pwd = user.getPassword();
        if (pwd != null) {
            if (pwd.isEmpty()) {
                user.setPassword(null);
            } else if (!isEncoded(pwd)) {
                validatePassword(pwd);
                user.setPassword(passwordEncoder.encode(pwd));
            }
        }
        return super.updateById(user);
    }

    /**
     * 校验密码强度：非空 + 长度 6~20 位。
     * @param password 明文密码
     * @throws IllegalArgumentException 不合规时抛出，消息可直接返回给前端
     */
    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        int len = password.length();
        if (len < PASSWORD_MIN_LEN || len > PASSWORD_MAX_LEN) {
            log.warn("密码长度不合规: length={}", len);
            throw new IllegalArgumentException(
                    "密码长度必须为 " + PASSWORD_MIN_LEN + "~" + PASSWORD_MAX_LEN + " 位");
        }
    }

    /**
     * 判断密码是否已经是 BCrypt 密文。
     */
    private boolean isEncoded(String password) {
        return password != null && password.startsWith(BCRYPT_PREFIX);
    }

    /**
     * 查询登录账号绑定的农户ID。
     *
     * 只 select farmer_id 一列，避免把密码哈希等无关字段读出来。
     */
    @Override
    public Long getFarmerId(Long userId) {
        if (userId == null) {
            return null;
        }
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.select("farmer_id").eq("id", userId);
        SysUser user = getOne(wrapper);
        return user == null ? null : user.getFarmerId();
    }
}
