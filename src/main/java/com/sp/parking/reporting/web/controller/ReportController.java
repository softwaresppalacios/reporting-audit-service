package com.sp.parking.reporting.web.controller;

import com.sp.parking.reporting.application.service.ReportService;
import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import com.sp.parking.reporting.web.dto.ReportDtos.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
  private final ReportService reportService;

  /** Reporte por rango de fechas (por defecto el mes en curso): solo propietario o administrador. */
  @GetMapping
  public ReportResponse report(
    @RequestHeader("X-Tenant-Id") UUID tenantId,
    @RequestAttribute(name = TenantAccessFilter.ROLE_ATTRIBUTE, required = false) String role,
    @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @RequestParam(value = "vehicleType", required = false) String vehicleType
  ) {
    if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
      throw new ResponseStatusException(FORBIDDEN, "Solo el propietario o un administrador pueden ver los reportes");
    }
    LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
    return reportService.report(tenantId, from == null ? today.withDayOfMonth(1) : from, to == null ? today : to, vehicleType);
  }
}
