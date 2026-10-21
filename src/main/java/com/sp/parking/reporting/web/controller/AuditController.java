package com.sp.parking.reporting.web.controller;

import com.sp.parking.reporting.application.service.AuditService;
import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import com.sp.parking.reporting.web.dto.AuditDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {
  private final AuditService auditService;

  /** Lo llaman los otros servicios con la sesion del usuario que hizo la accion; el autor sale de esa sesion. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void record(
    @RequestHeader("X-Tenant-Id") UUID tenantId,
    @RequestAttribute(TenantAccessFilter.USER_ATTRIBUTE) UUID userUuid,
    @RequestAttribute(TenantAccessFilter.USER_NAME_ATTRIBUTE) String userName,
    @Valid @RequestBody AuditEventRequest event
  ) {
    auditService.record(tenantId, userUuid, userName, event);
  }

  /** Consulta: solo propietario o administrador. Por defecto los ultimos 7 dias. */
  @GetMapping
  public AuditPage search(
    @RequestHeader("X-Tenant-Id") UUID tenantId,
    @RequestAttribute(name = TenantAccessFilter.ROLE_ATTRIBUTE, required = false) String role,
    @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @RequestParam(value = "action", required = false) String action,
    @RequestParam(value = "actor", required = false) String actor,
    @RequestParam(value = "q", required = false) String query
  ) {
    if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
      throw new ResponseStatusException(FORBIDDEN, "Solo el propietario o un administrador pueden ver la auditoría");
    }
    LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
    return auditService.search(tenantId, from == null ? today.minusDays(6) : from, to == null ? today : to, action, actor, query);
  }
}
