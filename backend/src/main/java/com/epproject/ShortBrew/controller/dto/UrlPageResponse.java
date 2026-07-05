package com.epproject.ShortBrew.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UrlPageResponse(
    List<UrlResponse> items,
    long total,
    int page,
    @JsonProperty("page_size") int pageSize,
    @JsonProperty("total_pages") int totalPages
) {}
