package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.entity.QualityStandard;
import com.agri.traceability.entity.TraceProcessNode;
import com.agri.traceability.mapper.QualityStandardMapper;
import com.agri.traceability.mapper.TraceProcessNodeMapper;
import com.agri.traceability.service.CacheService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质量标准控制器
 * 数据来源从硬编码迁移到数据库表，并使用Redis缓存提升性能
 */
@Api(tags = "质量标准", description = "农产品质量标准与溯源流程")
@RestController
@RequestMapping("/api/quality")
public class QualityStandardsController {

    private static final Logger log = LoggerFactory.getLogger(QualityStandardsController.class);

    @Autowired
    private QualityStandardMapper qualityStandardMapper;
    @Autowired
    private TraceProcessNodeMapper traceProcessNodeMapper;
    @Autowired
    private CacheService cacheService;

    @ApiOperation("查询质量标准列表")
    @GetMapping("/standards")
    @SuppressWarnings("unchecked")
    public Result<List<QualityStandard>> standards() {
        String cacheKey = "quality:standards";
        List<QualityStandard> list = (List<QualityStandard>) cacheService.get(cacheKey);
        if (list == null) {
            list = qualityStandardMapper.selectList(
                    new QueryWrapper<QualityStandard>().orderByAsc("sort_num"));
            cacheService.set(cacheKey, list, 30, java.util.concurrent.TimeUnit.MINUTES);
            log.info("质量标准列表从数据库加载并缓存, size={}", list.size());
        }
        return Result.success(list);
    }

    @ApiOperation("查询溯源流程节点")
    @GetMapping("/trace-process")
    @SuppressWarnings("unchecked")
    public Result<List<TraceProcessNode>> traceProcess() {
        String cacheKey = "quality:trace-process";
        List<TraceProcessNode> list = (List<TraceProcessNode>) cacheService.get(cacheKey);
        if (list == null) {
            list = traceProcessNodeMapper.selectList(
                    new QueryWrapper<TraceProcessNode>().orderByAsc("sort_num"));
            cacheService.set(cacheKey, list, 30, java.util.concurrent.TimeUnit.MINUTES);
            log.info("溯源流程节点从数据库加载并缓存, size={}", list.size());
        }
        return Result.success(list);
    }
}
