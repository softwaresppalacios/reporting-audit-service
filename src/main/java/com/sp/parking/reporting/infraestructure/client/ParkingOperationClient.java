package com.sp.parking.reporting.infraestructure.client;

import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

  /** Tickets con entrada en el rango (dias de Colombia). */
  public List<TicketRow> history(UUID tenantId, LocalDate from, LocalDate to) {
    return ServiceCalls.call("El servicio de operación", () -> restClient.get()
      .uri(uri -> uri.path("/tickets/history").queryParam("from", from).queryParam("to", to).build())
      .headers(headers -> ServiceCalls.userHeaders(headers, tenantId))
      .retrieve()
      .onStatus(HttpStatusCode::isError, ServiceCalls.propagate("El servicio de operación"))
      .body(new ParameterizedTypeReference<List<TicketRow>>() {}));
  }

  public record TicketRow(
    UUID uuid,
    String plate,
    String vehicleType,
    boolean monthlySubscription,
    Instant checkedInAt,
    Instant checkedOutAt,
    String status,
    BigDecimal amountDue,
    String paymentMethod
  ) {}

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
