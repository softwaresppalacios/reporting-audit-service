package com.sp.parking.reporting.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria: quien hizo que y cuando. El autor sale de la sesion validada del usuario,
 * no de lo que diga el servicio que reporta el evento.
 */
@Entity
@Table(schema = "reporting_audit_service", indexes = {
  @Index(name = "idx_audit_tenant_created", columnList = "tenantId, createdAt")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private UUID tenantId;
  private UUID actorUuid;
  private String actor;
  @Column(nullable = false, length = 40)
  private String action;
  /** Ej. TICKET, MONTHLY, CASH_SESSION, TARIFF, BRANCH, MEMBER. */
  @Column(length = 40)
  private String entityType;
  /** Referencia legible: placa, codigo de ticket, tipo de vehiculo, correo... */
  @Column(length = 120)
  private String entityRef;
  @Column(length = 1000)
  private String details;
  /** Servicio que reporto el evento. */
  @Column(length = 60)
  private String source;
  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
  }
}
