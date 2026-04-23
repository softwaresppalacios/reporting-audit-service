package com.sp.parking.reporting.web.dto;

import java.math.BigDecimal;

public final class DashboardDtos {
  private DashboardDtos() {}
  public record DashboardResponse(long activeTickets, long occupiedSpaces, long availableSpaces, BigDecimal todayRevenue) {}
}

