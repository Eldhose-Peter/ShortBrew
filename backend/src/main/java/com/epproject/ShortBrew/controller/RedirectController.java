package com.epproject.ShortBrew.controller;

import com.epproject.ShortBrew.security.RateLimit;
import com.epproject.ShortBrew.security.RateLimitType;
import com.epproject.ShortBrew.service.RedirectionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final RedirectionService redirectionService;

    public RedirectController(RedirectionService redirectionService) {
        this.redirectionService = redirectionService;
    }

    @GetMapping("/{code}")
    @RateLimit(name = "redirect", limit = 30, type = RateLimitType.IP)
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        String targetUrl = redirectionService.resolveRedirect(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, targetUrl)
                .build();
    }
}
