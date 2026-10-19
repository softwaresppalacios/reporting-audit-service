package com.sp.parking.reporting.infraestructure.client;

import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.UUID;

@Component
public class ParkingOperationClient {
  private final RestClient restClient;

  public ParkingOperationClient(@Value("${clients.parking-operation-service.url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public ParkingSummary current(UUID tenantId) {
    return restClient.get()
      .uri("/parking-summary/current")
      .header("X-Tenant-Id", tenantId.toString())
      .headers(headers -> {
        String authorization = TenantAccessFilter.currentAuthorization();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
      })
      .retrieve()
      .onStatus(HttpStatusCode::isError, (request, response) -> {})
      .body(ParkingSummary.class);
  }

  public record ParkingSummary(
    long activeTickets,
    long occupiedSpaces,
    long availableSpaces,
    long carOccupiedSpaces,
    long carAvailableSpaces,
    long motorcycleOccupiedSpaces,
    long motorcycleAvailableSpaces
  ) {}
}
