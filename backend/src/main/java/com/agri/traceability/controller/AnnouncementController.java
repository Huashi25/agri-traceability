package com.agri.traceability.controller;

import com.agri.traceability.common.RequireRole;
import com.agri.traceability.common.Result;
import com.agri.traceability.entity.Announcement;
import com.agri.traceability.service.AnnouncementService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {
    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/list")
    public Result list() {
        QueryWrapper<Announcement> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort");
        return Result.success(announcementService.list(wrapper));
    }

    @PostMapping
    @RequireRole({"ADMIN"})
    public Result<Void> save(@RequestBody Announcement announcement) {
        announcementService.saveOrUpdate(announcement);
        return Result.success();
    }

    @PutMapping
    @RequireRole({"ADMIN"})
    public Result<Void> update(@RequestBody Announcement announcement) {
        announcementService.updateById(announcement);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN"})
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.removeById(id);
        return Result.success();
    }
}
