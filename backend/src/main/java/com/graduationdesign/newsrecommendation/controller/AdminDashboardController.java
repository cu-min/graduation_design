package com.graduationdesign.newsrecommendation.controller;

import com.graduationdesign.newsrecommendation.common.Result;
import com.graduationdesign.newsrecommendation.service.DashboardService;
import com.graduationdesign.newsrecommendation.vo.AdminDashboardSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryVO> summary() {
        return Result.success(dashboardService.getAdminDashboardSummary());
    }
}
