package com.agri.traceability.service.impl;

import com.agri.traceability.entity.Notification;
import com.agri.traceability.mapper.NotificationMapper;
import com.agri.traceability.service.NotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {}
