package com.agri.traceability.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }
            if (requireRole != null) {
                String userRole = (String) request.getAttribute("role");
                if (userRole == null || !Arrays.asList(requireRole.value()).contains(userRole)) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 403);
                    result.put("message", "无权限访问");
                    result.put("data", null);
                    response.getWriter().write(objectMapper.writeValueAsString(result));
                    return false;
                }
            }
        }
        return true;
    }
}
