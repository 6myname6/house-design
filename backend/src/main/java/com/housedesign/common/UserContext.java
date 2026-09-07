package com.housedesign.common;

/**
 * 当前登录用户上下文。
 * 拦截器解析 JWT 后把 userId 存进来，业务代码随时取用，无需层层传参。
 *
 * 注意：必须在请求结束时调用 clear()（JwtInterceptor.afterCompletion 已做）。
 * Tomcat 线程池会复用线程，不清理会导致上一个用户的身份"串"到下一个请求。
 */
public class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /** 获取当前登录用户 ID；未经过拦截器的场景（如定时任务）返回 null */
    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
