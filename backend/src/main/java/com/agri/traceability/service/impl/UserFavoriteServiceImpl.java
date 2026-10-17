package com.agri.traceability.service.impl;

import com.agri.traceability.entity.UserFavorite;
import com.agri.traceability.mapper.UserFavoriteMapper;
import com.agri.traceability.service.UserFavoriteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements UserFavoriteService {
}
