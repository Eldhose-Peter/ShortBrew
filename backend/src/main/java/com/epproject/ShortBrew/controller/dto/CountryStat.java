package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountryStat(
    @JsonProperty("country_code") String countryCode,
    long count
) {}
