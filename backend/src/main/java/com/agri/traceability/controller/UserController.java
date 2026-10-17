package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.SysUser;
import com.agri.traceability.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        if (userService.register(user)) {
            return Result.success();
        }
        return Result.error("用户名已存在");
    }

    @ApiOperation("获取用户列表（管理员）")
    @GetMapping("/list")
    @RequireRole({"ADMIN"})
    public Result list() {
        return Result.success(userService.list());
    }

    @ApiOperation("根据ID获取用户信息")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @ApiOperation("新增/更新用户（管理员）")
    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody SysUser user) {
        userService.saveOrUpdate(user);
        return Result.success();
    }

    @ApiOperation("更新用户信息")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        userService.updateById(user);
        return Result.success();
    }

    @ApiOperation("删除用户（管理员）")
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
