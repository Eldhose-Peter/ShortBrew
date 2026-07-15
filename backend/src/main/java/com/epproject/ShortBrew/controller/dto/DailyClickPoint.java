package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record DailyClickPoint(
    @JsonProperty("stat_date") LocalDate statDate,
    @JsonProperty("click_count") long clickCount
) {}
