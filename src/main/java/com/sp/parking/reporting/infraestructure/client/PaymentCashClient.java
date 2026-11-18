package com.sp.parking.reporting.infraestructure.client;

import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import org.springframework.core.ParameterizedTypeReference;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentCashClient {
  private final RestClient restClient;

  public PaymentCashClient(@Value("${clients.payment-cash-service.url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public CashSummary current(UUID tenantId) {
    return restClient.get()
      .uri("/payments/today")
      .header("X-Tenant-Id", tenantId.toString())
      .headers(headers -> {
        String authorization = TenantAccessFilter.currentAuthorization();
        if (authorization != null) headers.set(HttpHeaders.AUTHORIZATION, authorization);
      })
      .retrieve()
      .onStatus(HttpStatusCode::isError, (request, response) -> {})
      .body(CashSummary.class);
  }

  public record CashSummary(BigDecimal todayRevenue) {}

  /** Cobros pagados en el rango (dias de Colombia). */
  public List<PaymentRow> payments(UUID tenantId, LocalDate from, LocalDate to) {
    return ServiceCalls.call("El servicio de caja", () -> restClient.get()
      .uri(uri -> uri.path("/payments").queryParam("from", from).queryParam("to", to).build())
      .headers(headers -> ServiceCalls.userHeaders(headers, tenantId))
      .retrieve()
      .onStatus(HttpStatusCode::isError, ServiceCalls.propagate("El servicio de caja"))
      .body(new ParameterizedTypeReference<List<PaymentRow>>() {}));
  }

  /** concept: PARKING o MONTHLY; method: CASH u ONLINE. */
  public record PaymentRow(
    UUID uuid,
    String concept,
    UUID referenceUuid,
    BigDecimal amount,
    String method,
    String onlineReference,
    String plate,
    String vehicleType,
    Instant checkedInAt,
    Instant checkedOutAt,
    String description,
    String registeredByName,
    Instant paidAt
  ) {}
}

