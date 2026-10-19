package com.agri.traceability.common;

import javax.servlet.http.HttpServletRequest;

/**
 * 当前登录用户上下文工具类。
 *
 * 数据来源：{@link JwtInterceptor} 解析 JWT 后写入的 request attribute
 * （userId / username / role），业务代码统一通过本类读取，避免各处重复实现。
 *
 * 注意（踩过的坑）：JWT 里的 userId 是 JSON 数字，JJWT 反序列化后
 * 可能是 Integer 也可能是 Long，直接强转 Long 会抛 ClassCastException，
 * 因此这里统一按 Number 处理。
 */
public final class UserContext {

    /** 管理员角色标识 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 商家角色标识 */
    public static final String ROLE_MERCHANT = "MERCHANT";
    /** 农户角色标识 */
    public static final String ROLE_FARMER = "FARMER";

    private UserContext() {
        // 工具类不允许实例化
    }

    /**
     * 取当前登录用户 ID。
     * @return 用户 ID；未登录或类型异常时返回 null
     */
    public static Long currentUserId(HttpServletRequest request) {
        Object value = request.getAttribute("userId");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 取当前登录用户角色。
     * @return 角色字符串；未登录时返回 null
     */
    public static String currentRole(HttpServletRequest request) {
        Object value = request.getAttribute("role");
        return value == null ? null : value.toString();
    }

    /**
     * 当前用户是否为管理员。
     */
    public static boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(currentRole(request));
    }

    /**
     * 当前用户是否为管理员或商家（发货类操作的放行范围）。
     */
    public static boolean isAdminOrMerchant(HttpServletRequest request) {
        String role = currentRole(request);
        return ROLE_ADMIN.equals(role) || ROLE_MERCHANT.equals(role);
    }

    /**
     * 当前用户是否为农户。
     */
    public static boolean isFarmer(HttpServletRequest request) {
        return ROLE_FARMER.equals(currentRole(request));
    }

    /**
     * 判断某条数据是否属于当前登录用户（用于防越权）。
     * @param ownerId 数据所属用户 ID
     * @return 属于返回 true；未登录或 ownerId 为空返回 false
     */
    public static boolean isOwner(HttpServletRequest request, Long ownerId) {
        Long current = currentUserId(request);
        return current != null && current.equals(ownerId);
    }
}
