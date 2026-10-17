package com.agri.traceability.service;

import com.agri.traceability.common.Result;
import com.agri.traceability.entity.SysUser;
import com.agri.traceability.mapper.SysUserMapper;
import com.agri.traceability.service.impl.SysUserServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 测试SysUserServiceImpl的登录、注册等核心业务方法
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户服务单元测试")
class SysUserServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private com.agri.traceability.common.JwtUtil jwtUtil;

    private SysUserServiceImpl sysUserService;

    private BCryptPasswordEncoder passwordEncoder;
    private SysUser testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        sysUserService = new SysUserServiceImpl();
        // 手动注入baseMapper（ServiceImpl基类的protected字段）
        ReflectionTestUtils.setField(sysUserService, "baseMapper", sysUserMapper);
        // 注入jwtUtil
        ReflectionTestUtils.setField(sysUserService, "jwtUtil", jwtUtil);

        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRealName("测试用户");
        testUser.setRole("CONSUMER");
        testUser.setStatus(1);
        testUser.setPhone("13800000001");
    }

    @Test
    @DisplayName("登录成功 - 用户名密码正确")
    void testLogin_Success() {
        // given
        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(1L, "testuser", "CONSUMER")).thenReturn("test-token-123");

        // when
        Map<String, Object> result = sysUserService.login("testuser", "password123");

        // then
        assertNotNull(result);
        assertEquals("test-token-123", result.get("token"));
        SysUser returnedUser = (SysUser) result.get("user");
        assertNotNull(returnedUser);
        assertEquals("testuser", returnedUser.getUsername());
        assertNull(returnedUser.getPassword(), "返回用户信息时密码应为null");
        verify(sysUserMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(jwtUtil, times(1)).generateToken(1L, "testuser", "CONSUMER");
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void testLogin_UserNotFound() {
        // given
        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // when
        Map<String, Object> result = sysUserService.login("nonexistent", "password123");

        // then
        assertNull(result);
        verify(sysUserMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_WrongPassword() {
        // given
        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(testUser);

        // when
        Map<String, Object> result = sysUserService.login("testuser", "wrongpassword");

        // then
        assertNull(result);
        verify(sysUserMapper, times(1)).selectOne(any(QueryWrapper.class));
        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 用户已禁用")
    void testLogin_UserDisabled() {
        // given
        testUser.setStatus(0);
        when(sysUserMapper.selectOne(any(QueryWrapper.class))).thenReturn(testUser);

        // when
        Map<String, Object> result = sysUserService.login("testuser", "password123");

        // then
        assertNull(result);
        verify(jwtUtil, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("注册成功 - 新用户")
    void testRegister_Success() {
        // given
        SysUser newUser = new SysUser();
        newUser.setUsername("newuser");
        newUser.setPassword("newpass123");
        newUser.setRealName("新用户");
        newUser.setRole("CONSUMER");

        when(sysUserMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        // when
        boolean result = sysUserService.register(newUser);

        // then
        assertTrue(result);
        verify(sysUserMapper, times(1)).insert(any(SysUser.class));
        // 验证密码已加密
        assertNotEquals("newpass123", newUser.getPassword());
        assertTrue(passwordEncoder.matches("newpass123", newUser.getPassword()));
        assertEquals(1, newUser.getStatus());
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void testRegister_UsernameExists() {
        // given
        SysUser existingUser = new SysUser();
        existingUser.setUsername("existing");
        existingUser.setPassword("pass123");

        when(sysUserMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // when
        boolean result = sysUserService.register(existingUser);

        // then
        assertFalse(result);
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("注册 - 角色为空时默认CONSUMER")
    void testRegister_DefaultRole() {
        // given
        SysUser newUser = new SysUser();
        newUser.setUsername("newuser2");
        newUser.setPassword("pass123");
        newUser.setRole(null);

        when(sysUserMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        // when
        boolean result = sysUserService.register(newUser);

        // then
        assertTrue(result);
        assertEquals("CONSUMER", newUser.getRole());
    }

    @Test
    @DisplayName("注册 - 支持FARMER角色")
    void testRegister_FarmerRole() {
        // given
        SysUser farmer = new SysUser();
        farmer.setUsername("farmer01");
        farmer.setPassword("pass123");
        farmer.setRole("FARMER");
        farmer.setFarmerId(1L);

        when(sysUserMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        // when
        boolean result = sysUserService.register(farmer);

        // then
        assertTrue(result);
        assertEquals("FARMER", farmer.getRole());
        assertEquals(1L, farmer.getFarmerId());
    }
}
