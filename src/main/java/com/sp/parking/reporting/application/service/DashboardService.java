package com.sp.parking.reporting.application.service;

import com.sp.parking.reporting.infraestructure.client.*;
import com.sp.parking.reporting.web.dto.DashboardDtos.DashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {
  private final ParkingOperationClient parkingOperationClient;
  private final PaymentCashClient paymentCashClient;

  public DashboardResponse current(UUID tenantId) {
    ParkingOperationClient.ParkingSummary parking = parkingOperationClient.current(tenantId);
    PaymentCashClient.CashSummary cash = paymentCashClient.current(tenantId);
    return new DashboardResponse(
      parking == null ? 0 : parking.activeTickets(),
      parking == null ? 0 : parking.occupiedSpaces(),
      parking == null ? 0 : parking.availableSpaces(),
      cash == null || cash.todayRevenue() == null ? BigDecimal.ZERO : cash.todayRevenue()
    );
  }
}
