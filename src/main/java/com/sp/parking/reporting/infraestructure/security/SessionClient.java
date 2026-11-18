package com.sp.parking.reporting.infraestructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Valida el token de sesion contra tenant-management-service (/auth/me).
 * Las respuestas validas se guardan unos segundos para no consultar en cada peticion.
 */
@Component
public class SessionClient {
  private static final int MAX_ENTRIES = 1000;

  private final RestClient restClient;
  private final Duration cacheTtl;
  private final Map<String, CachedSession> cache = new ConcurrentHashMap<>();

  public SessionClient(
    RestClient.Builder restClientBuilder,
    @Value("${services.tenant-management.base-url}") String tenantBaseUrl,
    @Value("${auth.cache-seconds:30}") long cacheSeconds
  ) {
    this.restClient = restClientBuilder.baseUrl(tenantBaseUrl).build();
    this.cacheTtl = Duration.ofSeconds(cacheSeconds);
  }

  public Session validate(String authorization) {
    if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      throw new ResponseStatusException(UNAUTHORIZED, "Inicia sesión para continuar");
    }
    Instant now = Instant.now();
    CachedSession cached = cache.get(authorization);
    if (cached != null && cached.expiresAt().isAfter(now)) return cached.session();

    Session session = fetch(authorization);
    if (cache.size() >= MAX_ENTRIES) cache.values().removeIf(item -> !item.expiresAt().isAfter(now));
    cache.put(authorization, new CachedSession(session, now.plus(cacheTtl)));
    return session;
  }

  private Session fetch(String authorization) {
    Session session;
    try {
      session = restClient.get()
        .uri("/auth/me")
        .header(HttpHeaders.AUTHORIZATION, authorization)
        .retrieve()
        .body(Session.class);
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode().value() == FORBIDDEN.value()) {
        throw new ResponseStatusException(FORBIDDEN, "Tu cuenta está desactivada");
      }
      throw new ResponseStatusException(UNAUTHORIZED, "Tu sesión expiró, vuelve a iniciar sesión");
    } catch (RestClientException ex) {
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No se pudo validar la sesión, intenta de nuevo");
    }
    if (session == null || session.user() == null) {
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No se pudo validar la sesión, intenta de nuevo");
    }
    return session;
  }

  public record Session(User user, List<TenantAccess> tenants) {
    public Optional<String> roleIn(UUID tenantId) {
      if (tenants == null) return Optional.empty();
      return tenants.stream().filter(tenant -> tenantId.equals(tenant.uuid())).map(TenantAccess::role).findFirst();
    }
  }
  public record User(UUID uuid, String fullName, String email) {}
  public record TenantAccess(UUID uuid, String role) {}
  private record CachedSession(Session session, Instant expiresAt) {}
}
