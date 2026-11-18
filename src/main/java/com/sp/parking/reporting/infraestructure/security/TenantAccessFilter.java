package com.sp.parking.reporting.infraestructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Toda la API exige una sesion valida y que el usuario pertenezca al parqueadero de X-Tenant-Id.
 * Deja el usuario y su rol como atributos de la peticion.
 */
@Component
@RequiredArgsConstructor
public class TenantAccessFilter extends OncePerRequestFilter {
  public static final String TENANT_HEADER = "X-Tenant-Id";
  public static final String USER_ATTRIBUTE = "auth.userUuid";
  public static final String ROLE_ATTRIBUTE = "auth.role";
  public static final String USER_NAME_ATTRIBUTE = "auth.userName";

  private final SessionClient sessionClient;
  private final ObjectMapper objectMapper;

  /** Authorization de la peticion en curso, para reenviarlo a otros servicios. */
  public static String currentAuthorization() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
      return attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
    return null;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/") || HttpMethod.OPTIONS.matches(request.getMethod());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
    throws ServletException, IOException {
    try {
      UUID tenantId = tenantId(request);
      SessionClient.Session session = sessionClient.validate(request.getHeader(HttpHeaders.AUTHORIZATION));
      String role = session.roleIn(tenantId)
        .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "No tienes acceso a este parqueadero"));
      request.setAttribute(USER_ATTRIBUTE, session.user().uuid());
      request.setAttribute(ROLE_ATTRIBUTE, role);
      request.setAttribute(USER_NAME_ATTRIBUTE, session.user().fullName());
    } catch (ResponseStatusException ex) {
      reject(response, ex);
      return;
    }
    chain.doFilter(request, response);
  }

  private UUID tenantId(HttpServletRequest request) {
    String value = request.getHeader(TENANT_HEADER);
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(BAD_REQUEST, "Falta el encabezado " + TENANT_HEADER);
    }
    try {
      return UUID.fromString(value.trim());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(BAD_REQUEST, "El encabezado " + TENANT_HEADER + " no es válido");
    }
  }

  private void reject(HttpServletResponse response, ResponseStatusException ex) throws IOException {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), Map.of(
      "timestamp", Instant.now().toString(),
      "status", status.value(),
      "error", status.getReasonPhrase(),
      "message", ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()
    ));
  }
}
