package com.epproject.ShortBrew.security;

import com.epproject.ShortBrew.exception.RateLimitException;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final Environment environment;

    public RateLimitInterceptor(RateLimiterService rateLimiterService, Environment environment) {
        this.rateLimiterService = rateLimiterService;
        this.environment = environment;
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
            String name = rateLimit.name();

            int limit = getIntProperty("rate-limit." + name + ".max",
                        getIntProperty("RATE_LIMIT_" + name.toUpperCase() + "_MAX", rateLimit.limit()));

            int windowSeconds = getIntProperty("rate-limit." + name + ".window-seconds",
                                getIntProperty("RATE_LIMIT_" + name.toUpperCase() + "_WINDOW_SECONDS", rateLimit.windowSeconds()));

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

            String key = "rate_limit:" + name + ":" + rateLimit.type().name().toLowerCase() + ":" + identifier;
            long windowMs = windowSeconds * 1000L;

            if (!rateLimiterService.isAllowed(key, limit, windowMs)) {
                throw new RateLimitException("Too many requests. Limit: " + limit + " requests per " + windowSeconds + " seconds.");
            }
        }

        return true;
    }

    private int getIntProperty(String key, int defaultValue) {
        String val = environment.getProperty(key);
        if (val != null && !val.isBlank()) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}

