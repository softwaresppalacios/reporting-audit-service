package com.sp.parking.reporting.domain.repository;

import com.sp.parking.reporting.domain.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
  List<AuditLog> findByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
    UUID tenantId, Instant from, Instant to, Pageable pageable);
}
