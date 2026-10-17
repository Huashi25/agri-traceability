package com.agri.traceability.common;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseToken(token);
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("username", claims.get("username"));
                request.setAttribute("role", claims.get("role"));
                return true;
            }
        }

        if ("GET".equalsIgnoreCase(request.getMethod()) && isPublicGetPath(request.getRequestURI())) {
            return true;
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }

    private boolean isPublicGetPath(String path) {
        if (path.contains("/admin/")) return false;
        if (path.startsWith("/api/user/login") || path.startsWith("/api/user/register")) return true;
        if (path.matches("/api/product/list.*") || path.matches("/api/product/\\d+")) return true;
        if (path.matches("/api/category/list.*") || path.matches("/api/category/\\d+")) return true;
        if (path.startsWith("/api/trace/query/") || path.equals("/api/trace/batch/list")) return true;
        if (path.startsWith("/api/announcement/list")) return true;
        if (path.startsWith("/api/farmer/list")) return true;
        if (path.startsWith("/api/review/list")) return true;
        if (path.startsWith("/api/seasonal/")) return true;
        if (path.startsWith("/api/knowledge/")) return true;
        if (path.startsWith("/api/recipe/")) return true;
        if (path.startsWith("/api/faq/")) return true;
        if (path.startsWith("/api/origin/")) return true;
        if (path.startsWith("/api/stats/")) return true;
        if (path.startsWith("/api/quality/")) return true;
        return false;
    }
}
