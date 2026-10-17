package com.agri.traceability.service.impl;

import com.agri.traceability.entity.KnowledgeArticle;
import com.agri.traceability.mapper.KnowledgeArticleMapper;
import com.agri.traceability.service.KnowledgeArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeArticleServiceImpl extends ServiceImpl<KnowledgeArticleMapper, KnowledgeArticle> implements KnowledgeArticleService {
}
