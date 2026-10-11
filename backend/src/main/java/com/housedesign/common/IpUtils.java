package com.housedesign.common;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时取第一个
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
