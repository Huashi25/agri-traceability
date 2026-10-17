package com.agri.traceability.service.impl;

import com.agri.traceability.entity.ProductCategory;
import com.agri.traceability.mapper.ProductCategoryMapper;
import com.agri.traceability.service.ProductCategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
}
