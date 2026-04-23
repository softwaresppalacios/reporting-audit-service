package com.sp.parking.reporting.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "reporting_audit_service")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private UUID tenantId;
  private String actor;
  private String action;
  private String details;
  private Instant createdAt;
}
