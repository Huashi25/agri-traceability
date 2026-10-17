package com.agri.traceability.service.impl;

import com.agri.traceability.entity.RecipeProduct;
import com.agri.traceability.mapper.RecipeProductMapper;
import com.agri.traceability.service.RecipeProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RecipeProductServiceImpl extends ServiceImpl<RecipeProductMapper, RecipeProduct> implements RecipeProductService {
}
