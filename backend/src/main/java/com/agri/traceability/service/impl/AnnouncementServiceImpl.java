package com.agri.traceability.service.impl;
import com.agri.traceability.entity.Announcement;
import com.agri.traceability.mapper.AnnouncementMapper;
import com.agri.traceability.service.AnnouncementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {}
