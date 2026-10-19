package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.common.UserContext;
import com.agri.traceability.entity.SysUser;
import com.agri.traceability.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Api(tags = "用户管理", description = "用户登录、注册、信息管理")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SysUserService userService;

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser user) {
        Map<String, Object> result = userService.login(user.getUsername(), user.getPassword());
        if (result != null) {
            return Result.success(result);
        }
        return Result.error("用户名或密码错误");
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        try {
            if (userService.register(user)) {
                return Result.success();
            }
            return Result.error("用户名已存在");
        } catch (IllegalArgumentException e) {
            // 密码强度等参数校验失败，把具体原因返回给前端
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("获取用户列表（管理员）")
    @GetMapping("/list")
    @RequireRole({"ADMIN"})
    public Result list() {
        // 密码属于敏感字段，列表接口统一脱敏，不返回任何形式的密码
        List<SysUser> users = userService.list();
        if (users != null) {
            users.forEach(u -> u.setPassword(null));
        }
        return Result.success(users);
    }

    @ApiOperation("根据ID获取用户信息")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id, HttpServletRequest request) {
        // 信息泄露防护：手机号、邮箱属于敏感信息，管理员可查任何人，普通用户只能查自己
        if (!UserContext.isAdmin(request)) {
            Long currentUserId = UserContext.currentUserId(request);
            if (currentUserId == null || !currentUserId.equals(id)) {
                return Result.error("无权查看其他用户的信息");
            }
        }
        SysUser user = userService.getById(id);
        if (user != null) {
            // 密码一律不返回
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @ApiOperation("新增/更新用户（管理员）")
    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody SysUser user) {
        try {
            userService.saveOrUpdate(user);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("更新用户信息")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user, HttpServletRequest request) {
        // 越权防护：管理员可改任何人；普通用户只能改自己，且不允许改角色与状态（防止自我提权）
        if (!UserContext.isAdmin(request)) {
            Long currentUserId = UserContext.currentUserId(request);
            if (currentUserId == null || user.getId() == null
                    || !currentUserId.equals(user.getId())) {
                return Result.error("无权修改其他用户的信息");
            }
            // 置空后 MyBatis-Plus 不会更新这两个字段
            user.setRole(null);
            user.setStatus(null);
        }
        try {
            // 密码为空串表示不修改密码，由 Service 层处理
            userService.updateById(user);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @ApiOperation("删除用户（管理员）")
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

}
