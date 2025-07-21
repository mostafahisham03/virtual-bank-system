package com.vbank.bff_service.controller;

import com.vbank.bff_service.dto.DashboardResponse;
import com.vbank.bff_service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bff/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/{userId}")
    public DashboardResponse getDashboard(@PathVariable String userId) {
        return dashboardService.getDashboard(userId);
    }
}
