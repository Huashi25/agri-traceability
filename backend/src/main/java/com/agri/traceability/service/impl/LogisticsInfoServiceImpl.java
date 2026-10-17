package com.agri.traceability.service.impl;

import com.agri.traceability.entity.LogisticsInfo;
import com.agri.traceability.mapper.LogisticsInfoMapper;
import com.agri.traceability.service.LogisticsInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class LogisticsInfoServiceImpl extends ServiceImpl<LogisticsInfoMapper, LogisticsInfo> implements LogisticsInfoService {
}
