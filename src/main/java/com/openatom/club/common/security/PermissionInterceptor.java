package com.openatom.club.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 权限拦截器（兼容层）：如果 JWT 拦截器已经设置了 ActorHolder，则跳过。
 * 仅在无 JWT token 时，回退到读取 X-Actor-* 请求头（保留向下兼容）。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private String decode(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 若 JWT 拦截器已设置 ActorHolder，则不再覆盖
        ActorContext existing = ActorHolder.get();
        if (existing != null && existing.getUserId() != null) {
            return true;
        }

        // 回退：从 X-Actor-* 请求头读取（开发调试用）
        String name = decode(request.getHeader("X-Actor-Name"));
        String department = decode(request.getHeader("X-Actor-Department"));
        String position = decode(request.getHeader("X-Actor-Position"));

        ActorContext actor = new ActorContext(
                StringUtils.hasText(name) ? name : "匿名",
                StringUtils.hasText(department) ? department : null,
                StringUtils.hasText(position) ? position : "社员"
        );
        ActorHolder.set(actor);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        ActorHolder.clear();
    }
}

