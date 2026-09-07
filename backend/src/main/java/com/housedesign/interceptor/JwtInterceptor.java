package com.housedesign.interceptor;

import com.housedesign.common.UserContext;
import com.housedesign.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器：校验请求头中的 token，通过后把 userId 放入 UserContext。
 * 放行路径在 WebConfig 中配置（登录/注册不需要 token）。
 */
@RequiredArgsConstructor
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 跨域预检请求不携带自定义头，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, "未登录");
        }

        String token = auth.substring(7); // 去掉 "Bearer " 前缀
        if (!jwtUtil.isValid(token)) {
            return reject(response, "登录已过期，请重新登录");
        }

        // 身份放入上下文，后续 Service 通过 UserContext.getUserId() 取用
        UserContext.setUserId(jwtUtil.getUserId(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        // 线程池复用线程，必须清理，防止用户身份串到下一个请求
        UserContext.clear();
    }

    /** 返回 401 和统一格式的响应体 */
    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
