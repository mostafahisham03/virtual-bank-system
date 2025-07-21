package com.vbank.bff_service.service;

import com.vbank.bff_service.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboard(String userId);
}
