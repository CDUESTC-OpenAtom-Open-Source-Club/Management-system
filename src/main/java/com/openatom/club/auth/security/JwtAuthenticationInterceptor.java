package com.openatom.club.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 认证拦截器：
 * - 从 Authorization: Bearer <token> 中解析 userId
 * - 查询 users 表 + 绑定的 member，构造 ActorContext
 * - 白名单接口（登录、Swagger）不拦截
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final MemberRepository memberRepository;
    private final CohortRepository cohortRepository;
    private final ObjectMapper objectMapper;

    /** 不需要认证的路径前缀 */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/swagger-ui",
            "/v3/api-docs",
            "/h2-console",
            "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // 公开路径放行
        for (String pub : PUBLIC_PATHS) {
            if (path.startsWith(pub)) {
                return true;
            }
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        Long userId;
        try {
            userId = jwtTokenProvider.getUserId(token);
        } catch (Exception e) {
            writeUnauthorized(response, "Token 解析失败");
            return false;
        }

        UserAccount user = userAccountRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            writeUnauthorized(response, "账号不存在或已被禁用");
            return false;
        }

        // 从绑定 member 构造 ActorContext（每次请求实时读取，权限随 member 实时变化）
        String name = user.getUsername();
        String department = null;
        String position = "社员";
        Long cohortId = null;
        Integer cohortYear = null;

        if (user.getMemberId() != null) {
            Member member = memberRepository.findByIdAndDeletedAtIsNull(user.getMemberId()).orElse(null);
            if (member != null) {
                name = member.getName();
                department = member.getDepartment();
                position = member.getPosition();
                cohortId = member.getCohortId();
                if (cohortId != null) {
                    cohortYear = cohortRepository.findByIdAndDeletedAtIsNull(cohortId)
                            .map(Cohort::getYear).orElse(null);
                }
            }
        }

        ActorContext actor = new ActorContext(name, department, position);
        // 把 userId 也存到 ActorContext，供日志等使用
        actor.setUserId(userId);
        actor.setUsername(user.getUsername());
        actor.setMemberId(user.getMemberId());
        actor.setCohortId(cohortId);
        actor.setCohortYear(cohortYear);
        ActorHolder.set(actor);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        ActorHolder.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.error(401, message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
