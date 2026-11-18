package com.sp.parking.reporting.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditDtos {
  private AuditDtos() {}

  /** Evento que reporta un servicio despues de completar una accion del usuario. */
  public record AuditEventRequest(
    @NotBlank @Size(max = 40) String action,
    @Size(max = 40) String entityType,
    @Size(max = 120) String entityRef,
    @Size(max = 1000) String details,
    @Size(max = 60) String source
  ) {}

  public record AuditEntry(
    Long id,
    Instant createdAt,
    UUID actorUuid,
    String actor,
    String action,
    String entityType,
    String entityRef,
    String details,
    String source
  ) {}

  /** truncated: hay mas registros que el limite; conviene acotar el rango o los filtros. */
  public record AuditPage(List<AuditEntry> entries, boolean truncated, List<String> actors) {}
}
