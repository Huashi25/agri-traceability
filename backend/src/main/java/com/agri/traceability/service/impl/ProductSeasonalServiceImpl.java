package com.agri.traceability.service.impl;

import com.agri.traceability.entity.ProductSeasonal;
import com.agri.traceability.mapper.ProductSeasonalMapper;
import com.agri.traceability.service.ProductSeasonalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductSeasonalServiceImpl extends ServiceImpl<ProductSeasonalMapper, ProductSeasonal> implements ProductSeasonalService {
}
