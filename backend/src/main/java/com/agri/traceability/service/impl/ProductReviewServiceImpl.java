package com.agri.traceability.service.impl;
import com.agri.traceability.entity.ProductReview;
import com.agri.traceability.mapper.ProductReviewMapper;
import com.agri.traceability.service.ProductReviewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service
public class ProductReviewServiceImpl extends ServiceImpl<ProductReviewMapper, ProductReview> implements ProductReviewService {}
