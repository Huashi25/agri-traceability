package com.agri.traceability.service.impl;
import com.agri.traceability.entity.Farmer;
import com.agri.traceability.mapper.FarmerMapper;
import com.agri.traceability.service.FarmerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service
public class FarmerServiceImpl extends ServiceImpl<FarmerMapper, Farmer> implements FarmerService {}
