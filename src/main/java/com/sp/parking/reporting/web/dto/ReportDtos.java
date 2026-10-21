package com.sp.parking.reporting.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
  private ReportDtos() {}

  /**
   * Reporte de un rango de fechas. Ingresos salen de los cobros de caja (parqueo y mensualidades);
   * entradas y estadias, de los tickets registrados en el rango.
   */
  public record ReportResponse(
    LocalDate from,
    LocalDate to,
    String vehicleType,
    Totals totals,
    List<DayRow> daily,
    List<TypeRow> byVehicleType,
    List<MethodRow> byMethod,
    List<CashierRow> byCashier,
    List<PaymentDetail> payments,
    boolean paymentsTruncated
  ) {}

  /** paidExits: salidas con cobro; monthlyExits: salidas de vehiculos con mensualidad (sin cobro). */
  public record Totals(
    BigDecimal revenue,
    BigDecimal parkingRevenue,
    BigDecimal monthlyRevenue,
    BigDecimal cashRevenue,
    BigDecimal onlineRevenue,
    long entries,
    long paidExits,
    long monthlyEntries,
    long monthlySales,
    long stillInside,
    BigDecimal averageTicket,
    long averageStayMinutes,
    long days
  ) {}

  public record DayRow(LocalDate date, BigDecimal revenue, BigDecimal parkingRevenue, BigDecimal monthlyRevenue, long entries) {}
  public record TypeRow(String vehicleType, long entries, BigDecimal revenue) {}
  public record MethodRow(String method, long payments, BigDecimal amount) {}
  public record CashierRow(String name, long payments, BigDecimal amount) {}

  public record PaymentDetail(
    Instant paidAt,
    String concept,
    String plate,
    String vehicleType,
    Instant checkedInAt,
    Instant checkedOutAt,
    String method,
    BigDecimal amount,
    String registeredByName,
    String description
  ) {}
}
