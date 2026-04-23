package com.sp.parking.reporting.web.controller;

import com.sp.parking.reporting.application.service.DashboardService;
import com.sp.parking.reporting.web.dto.DashboardDtos.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboardService;

  @GetMapping("/current")
  public DashboardResponse current(@RequestHeader("X-Tenant-Id") UUID tenantId) {
    return dashboardService.current(tenantId);
  }
}

