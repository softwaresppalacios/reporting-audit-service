package com.sp.parking.reporting.infraestructure.client;

import com.sp.parking.reporting.infraestructure.security.TenantAccessFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentCashClient {
  private final RestClient restClient;

  public PaymentCashClient(@Value("${clients.payment-cash-service.url}") String baseUrl) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
  }

  public CashSummary current(UUID tenantId) {
    return restClient.get()
      .uri("/cash-sessions/current")
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
}

