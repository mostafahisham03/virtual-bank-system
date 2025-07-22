package com.vbank.bff_service.service;

import com.vbank.bff_service.dto.DashboardResponse;

import reactor.core.publisher.Mono;

public interface DashboardService {
    Mono<DashboardResponse> getDashboard(String userId);
}
