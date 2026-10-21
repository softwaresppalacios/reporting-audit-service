package com.sp.parking.reporting.application.service;

import com.sp.parking.reporting.domain.model.AuditLog;
import com.sp.parking.reporting.domain.repository.AuditLogRepository;
import com.sp.parking.reporting.web.dto.AuditDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AuditService {
  private static final ZoneId ZONE = ZoneId.of("America/Bogota");
  /** Se leen hasta SCAN registros del rango y se devuelven hasta LIMIT despues de filtrar. */
  private static final int SCAN = 5000;
  private static final int LIMIT = 500;

  private final AuditLogRepository repository;

  @Transactional
  public void record(UUID tenantId, UUID actorUuid, String actorName, AuditEventRequest event) {
    repository.save(AuditLog.builder()
      .tenantId(tenantId)
      .actorUuid(actorUuid)
      .actor(actorName)
      .action(event.action().trim().toUpperCase(Locale.ROOT))
      .entityType(trim(event.entityType()))
      .entityRef(trim(event.entityRef()))
      .details(trim(event.details()))
      .source(trim(event.source()))
      .build());
  }

  @Transactional(readOnly = true)
  public AuditPage search(UUID tenantId, LocalDate from, LocalDate to, String action, String actor, String query) {
    if (to.isBefore(from)) throw new ResponseStatusException(BAD_REQUEST, "La fecha inicio debe ser anterior a la fecha fin");
    if (from.plusDays(366).isBefore(to)) throw new ResponseStatusException(BAD_REQUEST, "El rango máximo es de un año");
    List<AuditLog> logs = repository.findByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
      tenantId, from.atStartOfDay(ZONE).toInstant(), to.plusDays(1).atStartOfDay(ZONE).toInstant(), PageRequest.of(0, SCAN));
    String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    String plateText = text.replaceAll("[^a-z0-9]", "");
    List<AuditEntry> matches = logs.stream()
      .filter(log -> action == null || action.isBlank() || log.getAction().equalsIgnoreCase(action))
      .filter(log -> actor == null || actor.isBlank() || actor.equals(log.getActor()))
      .filter(log -> text.isEmpty() || contains(log.getEntityRef(), text, plateText) || contains(log.getDetails(), text, plateText))
      .map(this::toEntry)
      .toList();
    List<String> actors = logs.stream().map(AuditLog::getActor).filter(Objects::nonNull).distinct().sorted().toList();
    return new AuditPage(matches.stream().limit(LIMIT).toList(), matches.size() > LIMIT || logs.size() >= SCAN, actors);
  }

  private boolean contains(String value, String text, String plateText) {
    if (value == null) return false;
    String lower = value.toLowerCase(Locale.ROOT);
    return lower.contains(text) || (!plateText.isEmpty() && lower.replaceAll("[^a-z0-9]", "").contains(plateText));
  }

  private AuditEntry toEntry(AuditLog log) {
    return new AuditEntry(log.getId(), log.getCreatedAt(), log.getActorUuid(), log.getActor(), log.getAction(),
      log.getEntityType(), log.getEntityRef(), log.getDetails(), log.getSource());
  }

  private String trim(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
