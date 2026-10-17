package com.agri.traceability.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 封装常用缓存操作，支持缓存穿透、缓存雪崩防护
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("缓存写入: key={}", key);
        } catch (Exception e) {
            log.error("缓存写入失败: key={}", key, e);
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("缓存写入(TTL={}{}): key={}", timeout, unit, key);
        } catch (Exception e) {
            log.error("缓存写入失败: key={}", key, e);
        }
    }

    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("缓存命中: key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("缓存读取失败: key={}", key, e);
            return null;
        }
    }

    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("缓存删除: key={}, result={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("缓存删除失败: key={}", key, e);
            return false;
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("缓存检查失败: key={}", key, e);
            return false;
        }
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            log.error("设置过期失败: key={}", key, e);
            return false;
        }
    }
}
