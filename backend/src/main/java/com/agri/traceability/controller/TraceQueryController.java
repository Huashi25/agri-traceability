package com.agri.traceability.controller;

import com.agri.traceability.common.Result;
import com.agri.traceability.common.TraceQueryVO;
import com.agri.traceability.service.TraceBatchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 溯源查询控制器（公开接口，无需登录）
 */
@Api(tags = "溯源查询", description = "通过溯源码查询农产品全链路信息")
@RestController
@RequestMapping("/api/trace/query")
public class TraceQueryController {

    @Autowired
    private TraceBatchService traceBatchService;

    @ApiOperation("根据溯源码查询溯源信息")
    @GetMapping("/{traceCode}")
    public Result<TraceQueryVO> queryByTraceCode(@PathVariable String traceCode) {
        TraceQueryVO vo = traceBatchService.queryByTraceCode(traceCode);
        if (vo != null) {
            return Result.success(vo);
        }
        return Result.error("溯源码不存在");
    }
}
