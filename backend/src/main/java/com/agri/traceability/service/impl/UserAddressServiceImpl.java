package com.agri.traceability.service.impl;

import com.agri.traceability.entity.UserAddress;
import com.agri.traceability.service.UserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.agri.traceability.mapper.UserAddressMapper;
import org.springframework.stereotype.Service;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
}
