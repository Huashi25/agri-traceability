package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Notification;
import com.agri.traceability.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    public Result list(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        List<Notification> list = notificationService.list(wrapper);
        return Result.success(list);
    }

    @GetMapping("/unread-count")
    public Result unreadCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_read", 0);
        long count = notificationService.count(wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.success(data);
    }

    @PutMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        Notification n = new Notification();
        n.setId(id);
        n.setIsRead(1);
        n.setUpdateTime(LocalDateTime.now());
        notificationService.updateById(n);
        return Result.success();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId).eq("is_read", 0).set("is_read", 1).set("update_time", LocalDateTime.now());
        notificationService.update(wrapper);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return Result.error(401, "未登录");
        notificationService.removeById(id);
        return Result.success();
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> create(@RequestBody Notification notification) {
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        notificationService.save(notification);
        return Result.success();
    }

    private Long getUserId(HttpServletRequest request) {
        Object id = request.getAttribute("userId");
        if (id == null) return null;
        if (id instanceof Number) return ((Number) id).longValue();
        return Long.parseLong(id.toString());
    }
}
