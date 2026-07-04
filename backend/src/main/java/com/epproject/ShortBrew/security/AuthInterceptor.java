package com.epproject.ShortBrew.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.epproject.ShortBrew.exception.UnauthorizedException;
import com.epproject.ShortBrew.model.User;
import com.epproject.ShortBrew.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    public AuthInterceptor(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        boolean requiresAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class) ||
                               handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class) ||
                               hasCurrentUserParam(handlerMethod);

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null) {
            try {
                UUID userId = jwtService.verifyAccessToken(token);
                User user = userService.getUserById(userId);
                request.setAttribute("currentUser", user);
            } catch (JWTVerificationException | UnauthorizedException e) {
                if (requiresAuth) {
                    throw new UnauthorizedException("Invalid or expired token");
                }
            } catch (Exception e) {
                if (requiresAuth) {
                    throw new UnauthorizedException("Authentication failed");
                }
            }
        } else if (requiresAuth) {
            throw new UnauthorizedException("Missing authentication token");
        }

        return true;
    }

    private boolean hasCurrentUserParam(HandlerMethod handlerMethod) {
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            if (parameter.hasParameterAnnotation(CurrentUser.class)) {
                return true;
            }
        }
        return false;
    }
}
