package com.epproject.ShortBrew.controller.dto;

public record LoginRequest(
    String email,
    String password
) {}
