package com.agri.traceability.service;

import com.agri.traceability.common.TraceQueryVO;
import com.agri.traceability.entity.TraceBatch;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TraceBatchService extends IService<TraceBatch> {
    TraceQueryVO queryByTraceCode(String traceCode);
    String generateBatchNo();
    String generateTraceCode(Long productId);
}
