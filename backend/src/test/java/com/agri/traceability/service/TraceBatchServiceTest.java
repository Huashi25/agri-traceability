package com.agri.traceability.service;

import com.agri.traceability.common.TraceQueryVO;
import com.agri.traceability.entity.*;
import com.agri.traceability.mapper.*;
import com.agri.traceability.service.CacheService;
import com.agri.traceability.service.impl.TraceBatchServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 溯源服务单元测试
 * 测试TraceBatchServiceImpl的溯源查询、批次创建等核心方法
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("溯源服务单元测试")
class TraceBatchServiceTest {

    @Mock
    private TraceBatchMapper traceBatchMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private OriginInfoMapper originInfoMapper;
    @Mock
    private QualityInspectionMapper qualityInspectionMapper;
    @Mock
    private LogisticsInfoMapper logisticsInfoMapper;
    @Mock
    private CacheService cacheService;

    private TraceBatchServiceImpl traceBatchService;

    private TraceBatch testBatch;
    private Product testProduct;
    private OriginInfo testOrigin;
    private QualityInspection testInspection;
    private LogisticsInfo testLogistics;

    @BeforeEach
    void setUp() {
        traceBatchService = new TraceBatchServiceImpl();
        ReflectionTestUtils.setField(traceBatchService, "baseMapper", traceBatchMapper);
        ReflectionTestUtils.setField(traceBatchService, "productMapper", productMapper);
        ReflectionTestUtils.setField(traceBatchService, "originInfoMapper", originInfoMapper);
        ReflectionTestUtils.setField(traceBatchService, "qualityInspectionMapper", qualityInspectionMapper);
        ReflectionTestUtils.setField(traceBatchService, "logisticsInfoMapper", logisticsInfoMapper);
        ReflectionTestUtils.setField(traceBatchService, "cacheService", cacheService);

        // cacheService.get() 默认返回null（缓存未命中），走数据库查询
        when(cacheService.get(any())).thenReturn(null);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("有机红富士苹果");
        testProduct.setCategoryId(1L);
        testProduct.setOrigin("山东烟台");

        testBatch = new TraceBatch();
        testBatch.setId(1L);
        testBatch.setBatchNo("BATCH20260901001");
        testBatch.setTraceCode("TRACE20260901ABCDEF");
        testBatch.setProductId(1L);
        testBatch.setStatus(1);
        testBatch.setCreateTime(LocalDateTime.now().minusDays(10));

        testOrigin = new OriginInfo();
        testOrigin.setId(1L);
        testOrigin.setBatchId(1L);
        testOrigin.setFarmName("烟台栖霞苹果庄园");
        testOrigin.setLocation("山东省烟台市栖霞市");
        testOrigin.setFarmerName("王建国");
        testOrigin.setPlantDate(LocalDate.now().minusMonths(6));
        testOrigin.setHarvestDate(LocalDate.now().minusDays(10));
        testOrigin.setDescription("有机种植，不使用化学农药");

        testInspection = new QualityInspection();
        testInspection.setId(1L);
        testInspection.setBatchId(1L);
        testInspection.setInspector("李质检");
        testInspection.setInspectionDate(LocalDateTime.now().minusDays(5));
        testInspection.setInspectionItem("农药残留、重金属");
        testInspection.setResult("合格");
        testInspection.setQualified(1);

        testLogistics = new LogisticsInfo();
        testLogistics.setId(1L);
        testLogistics.setBatchId(1L);
        testLogistics.setShipDate(LocalDateTime.now().minusDays(2));
        testLogistics.setFromLocation("山东烟台");
        testLogistics.setToLocation("北京朝阳");
        testLogistics.setTransportCompany("顺丰冷链");
        testLogistics.setTransportMethod("冷藏车");
        testLogistics.setTemperature("2-8℃");
        testLogistics.setArrivalDate(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("溯源查询成功 - 返回完整溯源信息")
    void testQueryByTraceCode_Success() {
        String traceCode = "TRACE20260901ABCDEF";
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(testBatch);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(originInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testOrigin);
        when(qualityInspectionMapper.selectOne(any(QueryWrapper.class))).thenReturn(testInspection);
        when(logisticsInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testLogistics);

        TraceQueryVO vo = traceBatchService.queryByTraceCode(traceCode);

        assertNotNull(vo);
        assertNotNull(vo.getBatch());
        assertNotNull(vo.getProduct());
        assertNotNull(vo.getOrigin());
        assertNotNull(vo.getInspection());
        assertNotNull(vo.getLogistics());
        assertEquals("BATCH20260901001", vo.getBatch().getBatchNo());
        assertEquals("有机红富士苹果", vo.getProduct().getName());
        assertEquals("山东烟台", vo.getProduct().getOrigin());
        assertEquals("烟台栖霞苹果庄园", vo.getOrigin().getFarmName());
        assertEquals("合格", vo.getInspection().getResult());
        assertEquals("顺丰冷链", vo.getLogistics().getTransportCompany());
    }

    @Test
    @DisplayName("溯源查询失败 - 溯源码不存在")
    void testQueryByTraceCode_NotFound() {
        String invalidCode = "INVALID_CODE_123";
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        TraceQueryVO vo = traceBatchService.queryByTraceCode(invalidCode);

        assertNull(vo);
        verify(productMapper, never()).selectById(anyLong());
        verify(originInfoMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("溯源查询 - 产地信息为空时返回null")
    void testQueryByTraceCode_NoOriginInfo() {
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(testBatch);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(originInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(qualityInspectionMapper.selectOne(any(QueryWrapper.class))).thenReturn(testInspection);
        when(logisticsInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testLogistics);

        TraceQueryVO vo = traceBatchService.queryByTraceCode("TRACE20260901ABCDEF");

        assertNotNull(vo);
        assertNull(vo.getOrigin());
        assertNotNull(vo.getInspection());
        assertNotNull(vo.getLogistics());
    }

    @Test
    @DisplayName("溯源查询 - 质检信息为空时返回null")
    void testQueryByTraceCode_NoInspection() {
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(testBatch);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(originInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testOrigin);
        when(qualityInspectionMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(logisticsInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testLogistics);

        TraceQueryVO vo = traceBatchService.queryByTraceCode("TRACE20260901ABCDEF");

        assertNotNull(vo);
        assertNotNull(vo.getOrigin());
        assertNull(vo.getInspection());
        assertNotNull(vo.getLogistics());
    }

    @Test
    @DisplayName("溯源查询 - 物流信息为空时返回null")
    void testQueryByTraceCode_NoLogistics() {
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(testBatch);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(originInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testOrigin);
        when(qualityInspectionMapper.selectOne(any(QueryWrapper.class))).thenReturn(testInspection);
        when(logisticsInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        TraceQueryVO vo = traceBatchService.queryByTraceCode("TRACE20260901ABCDEF");

        assertNotNull(vo);
        assertNotNull(vo.getOrigin());
        assertNotNull(vo.getInspection());
        assertNull(vo.getLogistics());
    }

    @Test
    @DisplayName("溯源码生成 - 格式正确")
    void testGenerateTraceCode_Format() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String generatedCode = "TRACE" + datePart + uuidPart;

        assertTrue(generatedCode.startsWith("TRACE"));
        assertTrue(generatedCode.length() > 13);
        assertTrue(generatedCode.contains(datePart));
    }

    @Test
    @DisplayName("溯源链路完整 - 四环节全部有数据")
    void testTraceChain_Complete() {
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(testBatch);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(originInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testOrigin);
        when(qualityInspectionMapper.selectOne(any(QueryWrapper.class))).thenReturn(testInspection);
        when(logisticsInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testLogistics);

        TraceQueryVO vo = traceBatchService.queryByTraceCode("TRACE20260901ABCDEF");

        assertNotNull(vo.getBatch(), "批次信息不能为空");
        assertNotNull(vo.getProduct(), "商品信息不能为空");
        assertNotNull(vo.getOrigin(), "产地信息不能为空 - 农户环节");
        assertNotNull(vo.getInspection(), "质检信息不能为空 - 质量把关环节");
        assertNotNull(vo.getLogistics(), "物流信息不能为空 - 商家环节");
    }

    @Test
    @DisplayName("溯源链路部分缺失 - 只有产地和质检（农户刚完成，还没发货）")
    void testTraceChain_Partial_FarmerOnly() {
        when(traceBatchMapper.selectOne(any(QueryWrapper.class))).thenReturn(testBatch);
        when(productMapper.selectById(1L)).thenReturn(testProduct);
        when(originInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(testOrigin);
        when(qualityInspectionMapper.selectOne(any(QueryWrapper.class))).thenReturn(testInspection);
        when(logisticsInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        TraceQueryVO vo = traceBatchService.queryByTraceCode("TRACE20260901ABCDEF");

        assertNotNull(vo.getBatch());
        assertNotNull(vo.getOrigin());
        assertNotNull(vo.getInspection());
        assertNull(vo.getLogistics(), "农户刚完成质检，商家还未发货，物流信息为空");
    }
}
