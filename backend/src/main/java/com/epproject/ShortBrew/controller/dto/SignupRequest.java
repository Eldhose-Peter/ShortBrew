package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignupRequest(
    String email,
    String password,
    @JsonProperty("full_name") String fullName
) {}
