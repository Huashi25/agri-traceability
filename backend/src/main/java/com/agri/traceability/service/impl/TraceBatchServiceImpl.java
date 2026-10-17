package com.agri.traceability.service.impl;

import com.agri.traceability.common.TraceQueryVO;
import com.agri.traceability.entity.*;
import com.agri.traceability.mapper.*;
import com.agri.traceability.service.CacheService;
import com.agri.traceability.service.TraceBatchService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TraceBatchServiceImpl extends ServiceImpl<TraceBatchMapper, TraceBatch> implements TraceBatchService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OriginInfoMapper originInfoMapper;
    @Autowired
    private QualityInspectionMapper qualityInspectionMapper;
    @Autowired
    private LogisticsInfoMapper logisticsInfoMapper;
    @Autowired
    private CacheService cacheService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String[] TYPE_CODES = {"DLN", "ORG", "RIC", "GRL", "OTH"};
    private static final Random RANDOM = new Random();

    @Override
    public TraceQueryVO queryByTraceCode(String traceCode) {
        String cacheKey = "trace:query:" + traceCode;
        TraceQueryVO cached = (TraceQueryVO) cacheService.get(cacheKey);
        if (cached != null) {
            log.info("溯源查询命中缓存: traceCode={}", traceCode);
            return cached;
        }

        log.debug("溯源查询: traceCode={}", traceCode);
        QueryWrapper<TraceBatch> wrapper = new QueryWrapper<>();
        wrapper.eq("trace_code", traceCode);
        TraceBatch batch = getOne(wrapper);
        if (batch == null) {
            log.warn("溯源查询失败: 溯源码不存在, traceCode={}", traceCode);
            return null;
        }
        TraceQueryVO vo = new TraceQueryVO();
        vo.setBatch(batch);
        vo.setProduct(productMapper.selectById(batch.getProductId()));

        QueryWrapper<OriginInfo> originWrapper = new QueryWrapper<>();
        originWrapper.eq("batch_id", batch.getId());
        vo.setOrigin(originInfoMapper.selectOne(originWrapper));

        QueryWrapper<QualityInspection> inspectionWrapper = new QueryWrapper<>();
        inspectionWrapper.eq("batch_id", batch.getId());
        vo.setInspection(qualityInspectionMapper.selectOne(inspectionWrapper));

        QueryWrapper<LogisticsInfo> logisticsWrapper = new QueryWrapper<>();
        logisticsWrapper.eq("batch_id", batch.getId());
        vo.setLogistics(logisticsInfoMapper.selectOne(logisticsWrapper));

        cacheService.set(cacheKey, vo, 10, TimeUnit.MINUTES);
        log.info("溯源查询成功并缓存: traceCode={}, productId={}", traceCode, batch.getProductId());
        return vo;
    }

    @Override
    public String generateBatchNo() {
        return "BATCH" + System.currentTimeMillis();
    }

    @Override
    public String generateTraceCode(Long productId) {
        // 格式: AGRI + 8位日期 + 3位类型 + 3位流水号 + 3位随机字母
        String datePart = LocalDate.now().format(DATE_FMT);
        String typeCode = TYPE_CODES[4]; // 默认OTH
        if (productId != null) {
            Product product = productMapper.selectById(productId);
            if (product != null && product.getCategoryId() != null) {
                int catIdx = product.getCategoryId().intValue() - 1;
                if (catIdx >= 0 && catIdx < TYPE_CODES.length - 1) {
                    typeCode = TYPE_CODES[catIdx];
                }
            }
        }
        // 3位流水号
        long count = count() + 1;
        String serialPart = String.format("%03d", count % 1000);
        // 3位随机字母
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            randomPart.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return "AGRI" + datePart + typeCode + serialPart + randomPart.toString();
    }
}
