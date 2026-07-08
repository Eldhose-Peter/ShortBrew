package com.epproject.ShortBrew.security;

import com.epproject.ShortBrew.exception.RateLimitException;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit != null) {
            String identifier;
            if (rateLimit.type() == RateLimitType.USER) {
                User user = (User) request.getAttribute("currentUser");
                if (user == null) {
                    identifier = getClientIp(request);
                } else {
                    identifier = user.id().toString();
                }
            } else {
                identifier = getClientIp(request);
            }

            String key = "rate_limit:" + rateLimit.name() + ":" + rateLimit.type().name().toLowerCase() + ":" + identifier;
            long windowMs = rateLimit.windowSeconds() * 1000L;

            if (!rateLimiterService.isAllowed(key, rateLimit.limit(), windowMs)) {
                throw new RateLimitException("Too many requests. Limit: " + rateLimit.limit() + " requests per " + rateLimit.windowSeconds() + " seconds.");
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
