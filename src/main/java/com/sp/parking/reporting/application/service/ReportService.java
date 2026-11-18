package com.sp.parking.reporting.application.service;

import com.sp.parking.reporting.infraestructure.client.ParkingOperationClient;
import com.sp.parking.reporting.infraestructure.client.ParkingOperationClient.TicketRow;
import com.sp.parking.reporting.infraestructure.client.PaymentCashClient;
import com.sp.parking.reporting.infraestructure.client.PaymentCashClient.PaymentRow;
import com.sp.parking.reporting.web.dto.ReportDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** Reportes consolidados: consume los APIs de operacion y caja, no sus tablas. */
@Service
@RequiredArgsConstructor
public class ReportService {
  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  private static final int MAX_DETAIL = 1000;
  private static final List<String> TYPES = List.of("CAR", "MOTORCYCLE", "HEAVY", "BICYCLE");

  private final ParkingOperationClient operationClient;
  private final PaymentCashClient paymentClient;

  public ReportResponse report(UUID tenantId, LocalDate from, LocalDate to, String vehicleType) {
    if (to.isBefore(from)) throw new ResponseStatusException(BAD_REQUEST, "La fecha inicio debe ser anterior a la fecha fin");
    if (from.plusDays(366).isBefore(to)) throw new ResponseStatusException(BAD_REQUEST, "El rango máximo es de un año");
    String type = vehicleType == null || vehicleType.isBlank() ? null : vehicleType.trim().toUpperCase(Locale.ROOT);

    List<TicketRow> tickets = operationClient.history(tenantId, from, to).stream()
      .filter(ticket -> type == null || type.equals(ticket.vehicleType()))
      .toList();
    List<PaymentRow> payments = paymentClient.payments(tenantId, from, to).stream()
      .filter(payment -> type == null || type.equals(payment.vehicleType()))
      .toList();

    List<PaymentRow> parking = payments.stream().filter(payment -> "PARKING".equals(payment.concept())).toList();
    List<PaymentRow> monthly = payments.stream().filter(payment -> "MONTHLY".equals(payment.concept())).toList();
    BigDecimal parkingRevenue = sum(parking);
    BigDecimal monthlyRevenue = sum(monthly);
    BigDecimal cash = sum(payments.stream().filter(payment -> "CASH".equals(payment.method())).toList());
    BigDecimal online = sum(payments.stream().filter(payment -> !"CASH".equals(payment.method())).toList());

    List<TicketRow> closed = tickets.stream().filter(ticket -> ticket.checkedOutAt() != null).toList();
    long averageStay = closed.isEmpty() ? 0 : Math.round(closed.stream()
      .mapToLong(ticket -> Duration.between(ticket.checkedInAt(), ticket.checkedOutAt()).toMinutes())
      .average().orElse(0));

    Totals totals = new Totals(
      parkingRevenue.add(monthlyRevenue),
      parkingRevenue,
      monthlyRevenue,
      cash,
      online,
      tickets.size(),
      parking.size(),
      tickets.stream().filter(TicketRow::monthlySubscription).count(),
      monthly.size(),
      tickets.stream().filter(ticket -> ticket.checkedOutAt() == null).count(),
      parking.isEmpty() ? BigDecimal.ZERO : parkingRevenue.divide(BigDecimal.valueOf(parking.size()), 0, RoundingMode.HALF_UP),
      averageStay,
      ChronoUnit.DAYS.between(from, to) + 1
    );

    List<PaymentDetail> detail = payments.stream()
      .sorted(Comparator.comparing(PaymentRow::paidAt).reversed())
      .limit(MAX_DETAIL)
      .map(payment -> new PaymentDetail(
        payment.paidAt(), payment.concept(), payment.plate(), payment.vehicleType(), payment.checkedInAt(),
        payment.checkedOutAt(), payment.method(), payment.amount(), payment.registeredByName(), payment.description()))
      .toList();

    return new ReportResponse(
      from, to, type, totals,
      daily(from, to, tickets, payments),
      byType(tickets, payments),
      byGroup(payments, payment -> "CASH".equals(payment.method()) ? "CASH" : "ONLINE").entrySet().stream()
        .map(entry -> new MethodRow(entry.getKey(), entry.getValue().size(), sum(entry.getValue()))).toList(),
      byGroup(payments, payment -> payment.registeredByName() == null ? "Sin registrar" : payment.registeredByName()).entrySet().stream()
        .map(entry -> new CashierRow(entry.getKey(), entry.getValue().size(), sum(entry.getValue())))
        .sorted(Comparator.comparing(CashierRow::amount).reversed()).toList(),
      detail,
      payments.size() > MAX_DETAIL
    );
  }

  private List<DayRow> daily(LocalDate from, LocalDate to, List<TicketRow> tickets, List<PaymentRow> payments) {
    Map<LocalDate, List<PaymentRow>> paymentsByDay = payments.stream().collect(Collectors.groupingBy(payment -> day(payment.paidAt())));
    Map<LocalDate, Long> entriesByDay = tickets.stream().collect(Collectors.groupingBy(ticket -> day(ticket.checkedInAt()), Collectors.counting()));
    List<DayRow> rows = new ArrayList<>();
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      List<PaymentRow> dayPayments = paymentsByDay.getOrDefault(date, List.of());
      BigDecimal parking = sum(dayPayments.stream().filter(payment -> "PARKING".equals(payment.concept())).toList());
      BigDecimal monthly = sum(dayPayments.stream().filter(payment -> "MONTHLY".equals(payment.concept())).toList());
      rows.add(new DayRow(date, parking.add(monthly), parking, monthly, entriesByDay.getOrDefault(date, 0L)));
    }
    return rows;
  }

  private List<TypeRow> byType(List<TicketRow> tickets, List<PaymentRow> payments) {
    return TYPES.stream()
      .map(type -> new TypeRow(
        type,
        tickets.stream().filter(ticket -> type.equals(ticket.vehicleType())).count(),
        sum(payments.stream().filter(payment -> type.equals(payment.vehicleType())).toList())))
      .filter(row -> row.entries() > 0 || row.revenue().signum() > 0)
      .toList();
  }

  private <K> Map<K, List<PaymentRow>> byGroup(List<PaymentRow> payments, Function<PaymentRow, K> key) {
    return payments.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
  }

  private BigDecimal sum(List<PaymentRow> payments) {
    return payments.stream().map(PaymentRow::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private LocalDate day(java.time.Instant instant) {
    return instant.atZone(ZONE).toLocalDate();
  }
}
