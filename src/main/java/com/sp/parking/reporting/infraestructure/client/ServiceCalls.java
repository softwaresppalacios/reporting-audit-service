package com.sp.parking.reporting.infraestructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

/** Llamadas a otros servicios con la sesion del usuario: si fallan, el error llega al frontend con un mensaje claro. */
final class ServiceCalls {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ServiceCalls() {}

  static void userHeaders(HttpHeaders headers, UUID tenantId) {
    headers.set("X-Tenant-Id", tenantId.toString());
    String authorization = TenantAccessFilter.currentAuthorization();
    if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
  }

  /** Convierte la respuesta de error del otro servicio en la misma respuesta para el usuario. */
  static RestClient.ResponseSpec.ErrorHandler propagate(String serviceName) {
    return (request, response) -> {
      String message = readMessage(response.getBody());
      throw new ResponseStatusException(response.getStatusCode(),
        message != null ? message : serviceName + " respondió con un error");
    };
  }

  static <T> T call(String serviceName, Supplier<T> call) {
    try {
      return call.get();
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, serviceName + " no responde; intenta de nuevo");
    }
  }

  private static String readMessage(InputStream body) {
    try {
      JsonNode node = MAPPER.readTree(body);
      return node != null && node.hasNonNull("message") ? node.get("message").asText() : null;
    } catch (IOException ex) {
      return null;
    }
  }
}
