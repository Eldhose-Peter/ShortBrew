package com.epproject.ShortBrew.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String name();
    int limit();
    int windowSeconds() default 60;
    RateLimitType type() default RateLimitType.IP;
}
