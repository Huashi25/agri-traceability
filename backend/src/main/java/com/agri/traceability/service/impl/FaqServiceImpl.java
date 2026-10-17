package com.agri.traceability.service.impl;

import com.agri.traceability.entity.Faq;
import com.agri.traceability.mapper.FaqMapper;
import com.agri.traceability.service.FaqService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {
}
